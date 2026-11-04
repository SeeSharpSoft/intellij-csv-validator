const MARKER = '<!-- ai-changelog-validator -->';
const PLUGIN_ID = 'net.seesharpsoft.intellij.plugins.csv';

function compareVersions(left, right) {
  const a = left.split('.').map(Number);
  const b = right.split('.').map(Number);
  return a[0] - b[0] || a[1] - b[1] || a[2] - b[2];
}

function reportedVersion(body) {
  const pluginSection = body.replace(/\r\n/g, '\n').match(/^Plugin\n---\n([\s\S]*?)(?:\n\nIDE\n---\n|$)/m);
  if (!pluginSection) return null;
  const versions = [...pluginSection[1].matchAll(/\bid=([^,\s]+),\s*version=(\d+\.\d+\.\d+)(?=,|\))/g)]
    .filter(match => match[1] === PLUGIN_ID)
    .map(match => match[2]);
  return versions.length === 1 ? versions[0] : null;
}

function publishedFixes(changelog, currentVersion, latestVersion) {
  const fixes = [];
  let version = null;
  let fixedSection = false;
  for (const [index, line] of changelog.split(/\r?\n/).entries()) {
    if (line.startsWith('## ')) {
      version = line.match(/^## (\d+\.\d+\.\d+) - /)?.[1] ?? null;
      fixedSection = false;
    } else if (line.startsWith('### ')) {
      fixedSection = line.trim() === '### Fixed';
    } else if (version && fixedSection && line.startsWith('- ')
        && compareVersions(currentVersion, version) < 0
        && compareVersions(version, latestVersion) <= 0) {
      fixes.push({id: fixes.length, version, text: line.slice(2), line: index + 1});
    } else if (/^\s+\S/.test(line) && fixes.length && fixedSection && version === fixes.at(-1).version) {
      fixes.at(-1).text += ` ${line.trim()}`;
    }
  }

  const bounded = [];
  let length = 2;
  for (const fix of fixes) {
    const additionalLength = JSON.stringify(fix).length + (bounded.length ? 1 : 0);
    if (length + additionalLength > 10000) break;
    bounded.push(fix);
    length += additionalLength;
  }
  return bounded;
}

function prepareTriage(issue, changelog, latestTag) {
  const version = reportedVersion(issue.body ?? '');
  if (!version) return {skipReason: 'No unambiguous stable CSV plugin version in the report; leaving it open.'};
  const latestVersion = latestTag.replace(/^v/, '');
  if (!/^\d+\.\d+\.\d+$/.test(latestVersion)) {
    throw new Error(`Unsupported latest release tag: ${latestTag}`);
  }
  const fixes = publishedFixes(changelog, version, latestVersion);
  if (!fixes.length) return {skipReason: 'No published changelog fixes newer than the reported version; leaving it open.'};

  const report = issue.body.split(/^Plugin\r?\n---\r?\n/m)[0].trim();
  return {
    version,
    fixes,
    prompt: JSON.stringify({
      title: issue.title,
      reported_plugin_version: version,
      report: report.slice(0, 6000),
      report_truncated: report.length > 6000,
      published_fixes: fixes.map(({id, version, text}) => ({id, version, text}))
    })
  };
}

async function canTriage({github, context, core}) {
  const original = context.payload.issue;
  if (context.eventName !== 'issues' || context.payload.action !== 'opened'
      || !original.title.startsWith('[Automated Report]')) {
    core.info('Only newly opened [Automated Report] issues are eligible.');
    return false;
  }
  const target = {...context.repo, issue_number: original.number};
  const {data: current} = await github.rest.issues.get(target);
  if (current.state !== 'open' || current.title !== original.title || current.body !== original.body) {
    core.info('The issue was closed or edited after opening; leaving it unchanged.');
    return false;
  }
  const events = await github.paginate(github.rest.issues.listEvents, {...target, per_page: 100});
  if (events.some(event => event.event === 'reopened')) {
    core.info('The issue has been reopened; leaving it unchanged.');
    return false;
  }
  return true;
}

function parseVerdict(response, fixes) {
  const verdict = JSON.parse(response);
  if (!verdict || typeof verdict.resolved !== 'boolean'
      || !['high', 'low'].includes(verdict.confidence)
      || typeof verdict.reasoning !== 'string' || !verdict.reasoning.trim() || verdict.reasoning.length > 1500
      || (verdict.resolved
        ? !Number.isInteger(verdict.fix_id) || !fixes.some(fix => fix.id === verdict.fix_id)
        : verdict.fix_id !== null)) {
    throw new Error('Invalid AI triage verdict; refusing to modify the issue.');
  }
  return verdict;
}

function plainMarkdown(text) {
  return text.replace(/\s+/g, ' ').replace(/[\\`*_{}[\]()<>!|]/g, '\\$&').replace(/@/g, '&#64;');
}

async function applyVerdict(client, state, response) {
  const {github, context, core} = client;
  if (!await canTriage(client)) return;
  const verdict = parseVerdict(response, state.fixes);
  if (!verdict.resolved || verdict.confidence !== 'high') {
    core.info('No high-confidence published fix matched; leaving the issue open.');
    return;
  }
  const fix = state.fixes.find(fix => fix.id === verdict.fix_id);
  if (state.version !== reportedVersion(context.payload.issue.body ?? '')
      || compareVersions(state.version, fix.version) >= 0) {
    throw new Error('The reported version is not older than the matched fix; refusing to close the issue.');
  }

  const target = {...context.repo, issue_number: context.payload.issue.number};
  const comments = await github.paginate(github.rest.issues.listComments, {...target, per_page: 100});
  if (!comments.some(comment => comment.user?.type === 'Bot' && comment.body?.startsWith(MARKER))) {
    const url = `${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/blob/${context.sha}/CHANGELOG.md#L${fix.line}`;
    await github.rest.issues.createComment({
      ...target,
      body: [
        MARKER,
        '**AI Triage Assistant:** This automated report appears to match an already published fix.',
        `**Reported plugin version:** ${state.version}`,
        `**Fixed in:** ${fix.version} ([changelog](${url}))`,
        `> ${plainMarkdown(fix.text)}`,
        `**AI assessment:** ${plainMarkdown(verdict.reasoning)}`,
        'Closing as already fixed. Please update CSV Editor through Settings/Preferences > Plugins.',
        'If this still occurs on the latest compatible version, please reopen this issue with your plugin version and reproduction steps.'
      ].join('\n\n')
    });
  }
  if (await canTriage(client)) {
    await github.rest.issues.update({...target, state: 'closed', state_reason: 'completed'});
  }
}

module.exports = {prepareTriage, canTriage, applyVerdict};
