const assert = require('node:assert/strict');
const {test} = require('node:test');
const {prepareTriage, applyVerdict} = require('./opened-issue-validator.cjs');

function report(version = '252.54.0') {
  return {
    number: 123,
    title: '[Automated Report] Specific CSV exception',
    state: 'open',
    body: `Message\n---\nSpecific CSV exception\n\nStacktrace\n---\n at CsvTable.paint\n\nPlugin\n---\n`
      + `PluginClassLoader(plugin=PluginMainDescriptor(name=CSV Editor, id=net.seesharpsoft.intellij.plugins.csv, version=${version}, isBundled=false), `
      + 'parents=PluginMainDescriptor(name=IDEA CORE, id=com.intellij, version=262.123.456, isBundled=true))\n\nIDE\n---\nIntelliJ IDEA'
  };
}

function client(issue = report()) {
  const comments = [];
  const updates = [];
  const events = [];
  const messages = [];
  const pages = [];
  const candidateRequests = [];
  const candidateChanges = new Map();
  const current = {...issue};
  const release = {tag_name: '252.54.0'};
  const issues = {
    get: async ({issue_number}) => {
      if (issue_number === issue.number) return {data: current};
      const candidate = pages.flat().find(value => value.number === issue_number);
      assert.ok(candidate, `Unknown issue ${issue_number}`);
      return {data: {...candidate, ...candidateChanges.get(issue_number)}};
    },
    listEvents: async () => ({data: events}),
    listComments: async () => ({data: comments}),
    createComment: async comment => comments.push({...comment, user: {type: 'Bot'}}),
    update: async update => {
      updates.push(update);
      current.state = update.state;
    },
    listForRepo: async args => {
      candidateRequests.push(args);
      return {data: pages[args.page - 1] ?? []};
    }
  };
  const paginate = async (method, args) => (await method(args)).data;
  paginate.iterator = async function* (method, args) {
    for (let page = 1; ; page++) {
      const response = await method({...args, page});
      yield response;
      if (response.data.length < args.per_page) return;
    }
  };
  return {
    github: {
      rest: {issues, repos: {getLatestRelease: async () => ({data: release})}},
      paginate
    },
    context: {
      eventName: 'issues',
      payload: {action: 'opened', issue},
      repo: {owner: 'owner', repo: 'repo'},
      serverUrl: 'https://github.com'
    },
    core: {info: message => messages.push(message)},
    current, comments, updates, events, messages, release, pages, candidateRequests, candidateChanges
  };
}

test('outdated reports are closed with update instructions before any duplicate detection', async () => {
  const api = client(report('252.53.0'));
  api.github.rest.issues.listForRepo = async () => assert.fail('Outdated reports must not reach duplicate detection');
  assert.equal(await prepareTriage(api), null);
  assert.match(api.comments[0].body, /outdated CSV Editor plugin version/);
  assert.match(api.comments[0].body, /252\.54\.0.*releases\/tag\/252\.54\.0/);
  assert.match(api.comments[0].body, /Please update CSV Editor/);
  assert.deepEqual(api.updates, [
    {owner: 'owner', repo: 'repo', issue_number: 123, state: 'closed', state_reason: 'completed'}
  ]);
});

test('a report without the exact latest-release descriptor is closed before duplicate preparation', async () => {
  const api = client(report('252.54.1'));
  api.github.rest.issues.listForRepo = async () => assert.fail('A nonmatching descriptor must never reach duplicate preparation');
  assert.equal(await prepareTriage(api), null);
  assert.equal(api.updates.length, 1);
  assert.match(api.comments[0].body, /outdated CSV Editor plugin version/);
  assert.match(api.comments[0].body, /Please update CSV Editor/);
});

function duplicateClient() {
  const api = client();
  api.pages.push([{...report(), number: 122}, {...report(), number: 121}]);
  return api;
}

function verdict(overrides = {}) {
  return JSON.stringify({
    duplicate: true, confidence: 'high', issue_number: 122,
    reasoning: 'Both reports contain the same exception and relevant CSV stack frames.',
    ...overrides
  });
}

test('a high-confidence duplicate is closed with a reference to the matching open issue', async () => {
  const api = duplicateClient();
  await applyVerdict(api, await prepareTriage(api), verdict());
  assert.equal(api.comments.length, 1);
  assert.match(api.comments[0].body, /Duplicate of #122/);
  assert.match(api.comments[0].body, /same exception and relevant CSV stack frames/);
  assert.deepEqual(api.updates, [
    {owner: 'owner', repo: 'repo', issue_number: 123, state: 'closed', state_reason: 'not_planned'}
  ]);
});

test('current reports are compared with the five newest open issues, excluding pull requests and themselves', async () => {
  const api = client();
  api.pages.push([
    api.current,
    ...Array.from({length: 8}, (_, index) => ({...report(), number: 200 - index, pull_request: {}})),
    {...report(), number: 122}
  ], Array.from({length: 10}, (_, index) => ({...report(), number: 121 - index})));
  const state = await prepareTriage(api);
  assert.deepEqual(state.candidates.map(issue => issue.number), [122, 121, 120, 119, 118]);
  const prompt = JSON.parse(state.prompt);
  assert.equal(prompt.report.number, 123);
  assert.deepEqual(prompt.open_issues.map(issue => issue.number), [122, 121, 120, 119, 118]);
  assert.ok(api.candidateRequests.every(request => request.state === 'open'
    && request.sort === 'created' && request.direction === 'desc'));
  assert.deepEqual(api.comments, []);
  assert.deepEqual(api.updates, []);
});

test('manual reports and events other than opened never access GitHub', async () => {
  for (const change of [
    {title: 'Manual bug report'}, {title: '[automated report] Wrong case'},
    {action: 'reopened'}, {action: 'edited'}, {eventName: 'issue_comment'}
  ]) {
    const api = client();
    if (change.title) api.context.payload.issue.title = change.title;
    if (change.action) api.context.payload.action = change.action;
    if (change.eventName) api.context.eventName = change.eventName;
    api.github.rest.issues.get = async () => assert.fail('Ineligible events must not access GitHub');
    assert.equal(await prepareTriage(api), null);
    assert.deepEqual(api.updates, []);
  }
});

test('nonmatching historical versions are closed with links to v-prefixed release tags', async () => {
  for (const version of ['252.9.0', '252.53.10', '4.6.0']) {
    const api = client(report(version));
    api.release.tag_name = 'v252.54.0';
    await prepareTriage(api);
    assert.equal(api.updates.length, 1);
    assert.match(api.comments[0].body, /releases\/tag\/v252\.54\.0/);
    assert.deepEqual(api.candidateRequests, []);
  }
});

test('the exact latest-release descriptor is accepted anywhere in the body, including repeated metadata', async () => {
  const api = duplicateClient();
  const metadata = report().body.split('Plugin\n---\n')[1];
  api.context.payload.issue.body = api.current.body = `${metadata}\r\n${metadata}`;
  assert.ok(await prepareTriage(api));
  assert.deepEqual(api.comments, []);
  assert.deepEqual(api.updates, []);
});

test('the exact plugin ID and latest-version fragment permits duplicate detection regardless of surrounding text', async () => {
  for (const body of [
    report().body,
    report('252.53.0').body + report().body,
    'id=net.seesharpsoft.intellij.plugins.csv, version=252.54.0,',
    report().body.replace('name=CSV Editor,', 'name=Another Editor,')
  ]) {
    const api = duplicateClient();
    api.context.payload.issue.body = api.current.body = body;
    api.release.tag_name = 'v252.54.0';
    assert.ok(await prepareTriage(api));
    assert.deepEqual(api.comments, []);
    assert.deepEqual(api.updates, []);
  }
});

test('missing or nonmatching latest-release descriptors are closed before duplicate detection', async () => {
  for (const body of [
    null,
    'No plugin metadata',
    report('252.53.0-SNAPSHOT').body,
    report('262.1.0').body,
    report('252.54.00').body,
    report('252.53.0').body + report('252.52.0').body,
    report().body.replace('id=net.seesharpsoft.intellij.plugins.csv,', 'id=another.plugin,'),
    report().body.replace('id=net.seesharpsoft.intellij.plugins.csv,', 'id=net.seesharpsoft.intellij.plugins.csv.extra,'),
    report().body.replace('id=net.seesharpsoft.intellij.plugins.csv, version=', 'id=net.seesharpsoft.intellij.plugins.csv, name=CSV Editor, version='),
    report().body.replace('version=252.54.0,', 'version=252.54.0)')
  ]) {
    const api = duplicateClient();
    api.context.payload.issue.body = api.current.body = body;
    api.github.rest.issues.listForRepo = async () => assert.fail('A nonmatching descriptor must never reach duplicate detection');
    assert.equal(await prepareTriage(api), null);
    assert.equal(api.comments.length, 1);
    assert.match(api.comments[0].body, /outdated CSV Editor plugin version/);
    assert.match(api.comments[0].body, /Please update CSV Editor/);
    assert.equal(api.updates.length, 1);
  }
});

test('unsupported release tags fail explicitly without modifying the report', async () => {
  const api = client(report('252.53.0'));
  api.release.tag_name = 'unexpected-tag';
  await assert.rejects(prepareTriage(api), /Unsupported latest release tag/);
  assert.deepEqual(api.comments, []);
  assert.deepEqual(api.updates, []);
});

test('no other open issues skips inference without modifying the report', async () => {
  const api = client();
  api.pages.push([api.current, {...report(), number: 122, pull_request: {}}]);
  assert.equal(await prepareTriage(api), null);
  assert.ok(api.messages.some(message => message.includes('No other open issues')));
  assert.deepEqual(api.comments, []);
  assert.deepEqual(api.updates, []);
});

test('long report bodies are bounded and explicitly marked as truncated in the model prompt', async () => {
  const api = duplicateClient();
  api.context.payload.issue.body = api.current.body = 'New failure\n'.repeat(1000) + report().body;
  api.pages[0][0].body = 'Existing failure\n'.repeat(1000);
  const state = await prepareTriage(api);
  const prompt = JSON.parse(state.prompt);
  assert.equal(prompt.report.body.length, 6000);
  assert.equal(prompt.report.body_truncated, true);
  assert.equal(prompt.open_issues[0].body.length, 6000);
  assert.equal(prompt.open_issues[0].body_truncated, true);
  assert.equal(prompt.open_issues[1].body_truncated, false);
  assert.equal(state.candidates[0].body, api.pages[0][0].body);
});

test('negative and low-confidence duplicate verdicts leave the report open', async () => {
  for (const response of [
    verdict({duplicate: false, confidence: 'low', issue_number: null}),
    verdict({confidence: 'low'})
  ]) {
    const api = duplicateClient();
    await applyVerdict(api, await prepareTriage(api), response);
    assert.deepEqual(api.comments, []);
    assert.deepEqual(api.updates, []);
  }
});

test('malformed, incomplete, and hallucinated model verdicts never modify an issue', async () => {
  for (const response of [
    'DUPLICATE: true', 'null', '```json\n{}\n```', '{}',
    verdict({duplicate: 'true'}), verdict({confidence: 'medium'}),
    verdict({issue_number: 999}), verdict({issue_number: 123}), verdict({issue_number: '122'}),
    verdict({reasoning: ''}), verdict({reasoning: 'x'.repeat(1501)}),
    verdict({duplicate: false, issue_number: 122})
  ]) {
    const api = duplicateClient();
    await assert.rejects(applyVerdict(api, await prepareTriage(api), response));
    assert.deepEqual(api.comments, []);
    assert.deepEqual(api.updates, []);
  }
});

test('a verdict for a different report is rejected', async () => {
  const api = duplicateClient();
  const state = await prepareTriage(api);
  state.issueNumber = 999;
  await assert.rejects(applyVerdict(api, state, verdict()), /different issue/);
  assert.deepEqual(api.comments, []);
  assert.deepEqual(api.updates, []);
});

test('closed, edited, and reopened reports are left unchanged before validation and after inference', async () => {
  for (const change of [{state: 'closed'}, {title: 'Edited title'}, {body: 'Edited body'}, {reopened: true}]) {
    for (const afterInference of [false, true]) {
      const api = duplicateClient();
      const state = afterInference ? await prepareTriage(api) : null;
      if (change.reopened) api.events.push({event: 'reopened'});
      else Object.assign(api.current, change);
      if (afterInference) await applyVerdict(api, state, verdict());
      else assert.equal(await prepareTriage(api), null);
      assert.deepEqual(api.comments, []);
      assert.deepEqual(api.updates, []);
    }
  }
});

test('an edited or closed duplicate candidate is not used to close a report', async () => {
  for (const change of [{state: 'closed'}, {title: 'Different problem'}, {body: 'Edited description'}]) {
    const api = duplicateClient();
    const state = await prepareTriage(api);
    api.candidateChanges.set(122, change);
    await applyVerdict(api, state, verdict());
    assert.deepEqual(api.comments, []);
    assert.deepEqual(api.updates, []);
  }
});

test('reports and duplicate candidates are rechecked after posting the closure comment', async () => {
  for (const change of ['report', 'candidate']) {
    const api = duplicateClient();
    const state = await prepareTriage(api);
    const createComment = api.github.rest.issues.createComment;
    api.github.rest.issues.createComment = async comment => {
      await createComment(comment);
      if (change === 'report') api.events.push({event: 'reopened'});
      else api.candidateChanges.set(122, {state: 'closed'});
    };
    await applyVerdict(api, state, verdict());
    assert.equal(api.comments.length, 1);
    assert.deepEqual(api.updates, []);
  }
});

test('outdated-version closure also respects report changes made during comment posting', async () => {
  const api = client(report('252.53.0'));
  api.github.rest.issues.createComment = async () => { api.current.body = 'New reproduction details'; };
  await prepareTriage(api);
  assert.deepEqual(api.updates, []);
  assert.deepEqual(api.candidateRequests, []);
});

test('retrying a failed closure does not post a second bot comment', async () => {
  const api = duplicateClient();
  const state = await prepareTriage(api);
  const update = api.github.rest.issues.update;
  api.github.rest.issues.update = async () => { throw new Error('Temporary GitHub error'); };
  await assert.rejects(applyVerdict(api, state, verdict()), /Temporary GitHub error/);
  api.github.rest.issues.update = update;
  await applyVerdict(api, state, verdict());
  assert.equal(api.comments.length, 1);
  assert.equal(api.updates.length, 1);
});

test('GitHub API failures are surfaced rather than followed by issue closure', async () => {
  for (const method of ['get', 'listEvents', 'listComments', 'createComment']) {
    const api = duplicateClient();
    const state = await prepareTriage(api);
    api.github.rest.issues[method] = async () => { throw new Error('GitHub unavailable'); };
    await assert.rejects(applyVerdict(api, state, verdict()), /GitHub unavailable/);
    assert.deepEqual(api.updates, []);
  }
  for (const method of ['getLatestRelease', 'listForRepo']) {
    const api = duplicateClient();
    const service = method === 'getLatestRelease' ? api.github.rest.repos : api.github.rest.issues;
    service[method] = async () => { throw new Error('GitHub unavailable'); };
    await assert.rejects(prepareTriage(api), /GitHub unavailable/);
    assert.deepEqual(api.updates, []);
  }
});

test('model reasoning remains data and cannot introduce mentions or Markdown links', async () => {
  const api = duplicateClient();
  const reasoning = '$(touch injected) `code` [link](https://example.com) @someone\nNew line';
  await applyVerdict(api, await prepareTriage(api), verdict({reasoning}));
  assert.match(api.comments[0].body, /&#64;someone/);
  assert.ok(!api.comments[0].body.includes('[link](https://example.com)'));
  assert.ok(!api.comments[0].body.includes('`code`'));
});
