const assert = require('node:assert/strict');
const {test} = require('node:test');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const {prepareTriage, canTriage, applyVerdict} = require('./ai-changelog-validator.cjs');

const changelog = `# Changelog
## Unreleased
### Fixed
- Not published yet
## 252.55.0 - Nov 04, 2026
### Fixed
- A version not published on GitHub yet
## 252.54.0 - Nov 03, 2026
### Added
- A feature is not a bug fix
### Fixed
- Fixed the specific CSV exception
  with a continuation.
## 252.53.0 - Oct 30, 2026
### Fixed
- Fixed another CSV exception
## 4.6.0 - Oct 18, 2026
### Fixed
- A fix already installed by the reporter
`;

function report(version = '4.6.0') {
  return {
    number: 123,
    title: '[Automated Report] Specific CSV exception',
    state: 'open',
    body: `Message\n---\nDetails\n\nStacktrace\n---\nSpecific CSV exception\n at CsvTable.paint\n\nPlugin\n---\n`
      + `PluginClassLoader(plugin=PluginMainDescriptor(name=CSV Editor, id=net.seesharpsoft.intellij.plugins.csv, version=${version}, isBundled=false), `
      + 'parents=PluginMainDescriptor(name=IDEA CORE, id=com.intellij, version=262.123.456, isBundled=true))\n\nIDE\n---\nIntelliJ IDEA'
  };
}

function client(issue = report()) {
  const comments = [];
  const updates = [];
  const events = [];
  const messages = [];
  const current = {...issue};
  const issues = {
    get: async () => ({data: current}),
    listEvents: async () => ({data: events}),
    listComments: async () => ({data: comments}),
    createComment: async comment => comments.push({...comment, user: {type: 'Bot'}}),
    update: async update => updates.push(update)
  };
  return {
    github: {
      rest: {issues},
      paginate: async (method, args) => (await method(args)).data
    },
    context: {
      eventName: 'issues',
      payload: {action: 'opened', issue},
      repo: {owner: 'owner', repo: 'repo'},
      serverUrl: 'https://github.com',
      sha: 'trusted-commit'
    },
    core: {info: message => messages.push(message)},
    current, comments, updates, events, messages
  };
}

function verdict(overrides = {}) {
  return JSON.stringify({
    resolved: true, confidence: 'high', fix_id: 0,
    reasoning: 'The exception and CSV stack frame match the fix.',
    ...overrides
  });
}

test('prepares only newer published Fixed entries, newest first, preserving continuations', () => {
  const state = prepareTriage(report(), changelog, 'v252.54.0');
  assert.deepEqual(state.fixes.map(fix => [fix.version, fix.text]), [
    ['252.54.0', 'Fixed the specific CSV exception with a continuation.'],
    ['252.53.0', 'Fixed another CSV exception']
  ]);
  assert.equal(changelog.split('\n')[state.fixes[0].line - 1], '- Fixed the specific CSV exception');
  const prompt = JSON.parse(state.prompt);
  assert.equal(prompt.reported_plugin_version, '4.6.0');
  assert.match(prompt.report, /CsvTable.paint/);
  assert.doesNotMatch(prompt.report, /PluginClassLoader|IDEA CORE/);
});

test('keeps reports open when the version is missing, ambiguous, or not a stable version', () => {
  for (const body of [
    'No plugin metadata',
    report('4.6.0-SNAPSHOT').body,
    report().body.replace('parents=', 'id=net.seesharpsoft.intellij.plugins.csv, version=4.5.0, parents=')
  ]) {
    assert.match(prepareTriage({...report(), body}, changelog, '252.54.0').skipReason, /version/);
  }
});

test('uses the CSV plugin version rather than an IDE or dependency version, including CRLF reports', () => {
  const issue = report();
  issue.body = issue.body.replace(/\n/g, '\r\n');
  assert.equal(prepareTriage(issue, changelog, '252.54.0').version, '4.6.0');
});

test('keeps reports from the fixed release or newer open', () => {
  for (const version of ['252.54.0', '252.54.1', '262.1.0']) {
    assert.match(prepareTriage(report(version), changelog, '252.54.0').skipReason, /No published/);
  }
  assert.throws(() => prepareTriage(report(), changelog, 'invalid-release'), /Unsupported latest release/);
});

test('bounds report and changelog context without cutting a fix entry', () => {
  const issue = report();
  issue.body = issue.body.replace('Details', 'x'.repeat(10000));
  const largeChangelog = `## 252.54.0 - Nov 03, 2026\n### Fixed\n${'- Fixed a CSV exception\n'.repeat(1000)}`;
  const state = prepareTriage(issue, largeChangelog, '252.54.0');
  const prompt = JSON.parse(state.prompt);
  assert.equal(prompt.report.length, 6000);
  assert.equal(prompt.report_truncated, true);
  assert.ok(state.fixes.length > 0 && state.fixes.length < 1000);
  assert.ok(JSON.stringify(state.fixes).length <= 10000);
  assert.ok(state.fixes.every(fix => fix.text === 'Fixed a CSV exception'));
});

test('ordinary issues and all reopened events are ineligible without any GitHub reads', async () => {
  for (const action of ['reopened', 'edited', 'closed']) {
    const api = client();
    api.context.payload.action = action;
    api.github.rest.issues.get = () => assert.fail('Must not read a non-opened issue');
    assert.equal(await canTriage(api), false);
  }
  for (const title of ['A manual bug report', '[automated report] Wrong case']) {
    const api = client({...report(), title});
    api.github.rest.issues.get = () => assert.fail('Must not read a manual report');
    assert.equal(await canTriage(api), false);
  }
});

test('does not triage an opened event after the issue is closed, edited, or reopened', async () => {
  for (const change of [{state: 'closed'}, {title: 'Edited title'}, {body: 'Edited body'}]) {
    const api = client();
    Object.assign(api.current, change);
    assert.equal(await canTriage(api), false);
  }
  const api = client();
  api.events.push({event: 'closed'}, {event: 'reopened'});
  assert.equal(await canTriage(api), false);
});

test('a high-confidence match comments with the exact fix and closes as completed', async () => {
  const api = client();
  const state = prepareTriage(api.context.payload.issue, changelog, '252.54.0');
  await applyVerdict(api, state, verdict());
  assert.equal(api.comments.length, 1);
  assert.match(api.comments[0].body, /Fixed in:\*\* 252\.54\.0/);
  assert.match(api.comments[0].body, /Fixed the specific CSV exception with a continuation\./);
  assert.match(api.comments[0].body, new RegExp(`CHANGELOG.md#L${state.fixes[0].line}`));
  assert.match(api.comments[0].body, /please reopen this issue/);
  assert.deepEqual(api.updates, [
    {owner: 'owner', repo: 'repo', issue_number: 123, state: 'closed', state_reason: 'completed'}
  ]);
});

test('negative or low-confidence verdicts make no issue changes', async () => {
  for (const response of [
    verdict({resolved: false, confidence: 'low', fix_id: null}),
    verdict({confidence: 'low'})
  ]) {
    const api = client();
    await applyVerdict(api, prepareTriage(report(), changelog, '252.54.0'), response);
    assert.deepEqual(api.comments, []);
    assert.deepEqual(api.updates, []);
  }
});

test('malformed, incomplete, and hallucinated verdicts fail without modifying the issue', async () => {
  for (const response of [
    'RESOLVED: true', 'null', '```json\n{}\n```', '{}',
    verdict({resolved: 'true'}), verdict({confidence: 'medium'}),
    verdict({fix_id: 999}), verdict({fix_id: '0'}),
    verdict({reasoning: ''}), verdict({resolved: false, fix_id: 0})
  ]) {
    const api = client();
    await assert.rejects(applyVerdict(api, prepareTriage(report(), changelog, '252.54.0'), response));
    assert.deepEqual(api.comments, []);
    assert.deepEqual(api.updates, []);
  }
});

test('does not close when a report is reopened during inference or comment posting', async () => {
  const api = client();
  api.events.push({event: 'reopened'});
  await applyVerdict(api, prepareTriage(report(), changelog, '252.54.0'), verdict());
  assert.deepEqual(api.comments, []);
  assert.deepEqual(api.updates, []);

  const duringComment = client();
  duringComment.github.rest.issues.createComment = async () => duringComment.events.push({event: 'reopened'});
  await applyVerdict(duringComment, prepareTriage(report(), changelog, '252.54.0'), verdict());
  assert.deepEqual(duringComment.updates, []);
});

test('a rerun does not duplicate its bot comment', async () => {
  const api = client();
  const state = prepareTriage(report(), changelog, '252.54.0');
  await applyVerdict(api, state, verdict());
  await applyVerdict(api, state, verdict());
  assert.equal(api.comments.length, 1);
});

test('GitHub API failures are surfaced and never followed by closing the issue', async () => {
  for (const method of ['get', 'listEvents', 'listComments', 'createComment']) {
    const api = client();
    api.github.rest.issues[method] = async () => { throw new Error('GitHub unavailable'); };
    await assert.rejects(
      applyVerdict(api, prepareTriage(report(), changelog, '252.54.0'), verdict()),
      /GitHub unavailable/
    );
    assert.deepEqual(api.updates, []);
  }
});

test('model text is data, not executable shell, Markdown links, or mentions', async () => {
  const api = client();
  const reasoning = '$(touch injected) `code` [link](https://example.com) @someone\nNew line';
  await applyVerdict(api, prepareTriage(report(), changelog, '252.54.0'), verdict({reasoning}));
  assert.match(api.comments[0].body, /&#64;someone/);
  assert.ok(!api.comments[0].body.includes('[link](https://example.com)'));
  assert.ok(!api.comments[0].body.includes('`code`'));
});

test('the repository changelog and actual report metadata produce published fix candidates', () => {
  const actualChangelog = readFileSync(join(__dirname, '..', '..', 'CHANGELOG.md'), 'utf8');
  const state = prepareTriage(report('4.4.0'), actualChangelog, '252.54.0');
  assert.ok(state.fixes.some(fix => fix.text.includes('#1124')));
  assert.ok(state.fixes.every(fix => fix.version !== '4.4.0'));
});
