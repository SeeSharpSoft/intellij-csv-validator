# Agent guide

## Start here

This is the CSV Editor plugin for IntelliJ Platform IDEs, implemented in Java. CSV, TSV,
and PSV share language support, a text editor, and two table-editor implementations.

Read [README.md](README.md) before changing user-facing behavior: it documents supported
formats, editor features, settings, and limitations. Read [CONTRIBUTING.md](CONTRIBUTING.md)
before preparing a pull request for contribution requirements and plugin-metadata restrictions.

## Code map

The main plugin packages are under `src\main\java\net\seesharpsoft\intellij\plugins`.
The following paths are relative to that directory:

- `csv\Csv.bnf` and `csv\CsvLexer.flex`: Grammar-Kit parser/PSI and JFlex lexer inputs.
  `CsvLexerFactory` selects the generated-lexer adapter or `CsvSharpLexer` for custom
  separators, escape characters, and comment indicators; inspect both paths for lexer changes.
- `csv\CsvHelper.java`, `csv\components`, and `csv\settings`: shared CSV quoting/decoding,
  per-file separator/escape attributes (`CsvFileAttributes`), and editor/code-style settings.
- `csv\psi`: handwritten PSI support and `CsvPsiTreeUpdater`, which batches document/PSI
  changes into IDE write actions and undoable commands.
- `csv\editor\table`: shared PSI-backed model (`CsvTableModelBase`), editor state,
  search/replace, and reordering logic.
- `csv\editor\table\swing`: table UI, Swing model, clipboard, selection, movement, and
  rendering. `csv\editor\table\flatlaf` extends this implementation with FlatLaf components;
  keep behavior consistent across both editors.
- `csv\formatter`, `csv\highlighter`, `csv\inspection`, and `csv\intention`: text-editor
  language features. The sibling `tsv` and `psv` packages provide fixed-separator variants.

Plugin extensions and actions are registered in `src\main\resources\META-INF\plugin.xml`.
UI strings live in `src\main\resources\localization`; update the default and `_zh_CN`
bundles together. Inspection/intention descriptions have their own resource directories.
Tests mirror the source packages under `src\test\java`, with fixtures in `src\test\resources`.

## Build and tests

Use the checked-in Gradle wrapper and a JDK matching the selected IDE: Java 21 through
2026.1, Java 25 starting with 2026.2. `build.gradle.kts` derives the toolchain, source/target
compatibility, compiler release, and generator launchers from the resolved IDE build number.
`PublishStable.yml` stays pinned to 2025.2/Java 21 so compatibility builds using newer
SDKs do not raise the published plugin's Java requirement. `build.gradle.kts` defines the build;
`gradle.properties` defines plugin metadata and the default IntelliJ Platform target.
`IDEA_PLATFORM` and `IDEA_VERSION` environment variables override the target for compatibility
checks; keep APIs compatible with the minimum supported platform. `VERIFY_IDEA_PLATFORM`
and `VERIFY_IDEA_VERSION` optionally select a static-verification target without changing
the build SDK.

Windows/PowerShell commands:

| Command | Purpose |
| --- | --- |
| `.\gradlew.bat test --tests "*CsvTableModelSwingTest"` | Run a focused test class; substitute the relevant class or pattern. |
| `.\gradlew.bat test` | Run the complete test suite. |
| `.\gradlew.bat build` | Build the plugin and run checks, including tests. |
| `.\gradlew.bat runIde` | Launch an IDE sandbox for manual testing. |

On macOS/Linux, use the `gradlew` shell script instead. Linux CI runs Gradle under
`xvfb-run` for UI tests; see `.github\workflows\PullRequest.yml` for compatibility targets.
Gradle downloads the IntelliJ SDK and generation tools and generates parser/lexer sources
before Java compilation. Generated output is under the ignored `src\main\gen` directory:
edit the grammar/lexer inputs or handwritten sources rather than generated files.

## Writing tests

Use the existing IntelliJ fixture tests and resource-based expected results. Most editor
tests extend `CsvBasePlatformTestCase`; table fixtures use `CsvTableEditorSwingTestBase`.
For shared table changes, cover both Swing and FlatLaf (several FlatLaf suites inherit the
Swing tests).

- Prefer real collaborators and existing test fixtures over mocks. Use mocks only when
  exercising the real dependency is impractical.
- Prefer integration tests for functional and behavioral testing. Exercise the actual
  flow through collaborating components rather than relying only on isolated function tests.
- Use focused unit tests mainly to validate input/output contracts and error handling.
- Give tests meaningful names that describe the scenario and expected behavior, and assert
  outcomes that matter to that behavior.
- Prioritize realistic scenarios over achieving 100% coverage through artificial edge cases.
  For example, an enum switch should have a `default` branch that throws an unhandled-case
  exception for future unsupported values; tests need not invent a nonexistent enum value
  just to cover that branch.

For any code change, follow the full-suite and failure-handling requirements in
[Fixing a bug](#fixing-a-bug).

## Fixing a bug

Follow test-driven development (TDD):

1. Write a regression test that reproduces the issue before changing the implementation.
   Run it against the current implementation and confirm it fails because of the reported
   bug, not an unrelated setup or test error.
2. Fix the bug, then rerun the regression test and confirm it passes.
3. After any code change, not only bug fixes, run the complete suite with
   `.\gradlew.bat test` without test filters. The work is complete only when all tests pass.

If other tests fail after an implementation change, leave those tests unchanged initially,
even if the failure reflects an expected behavior change. First identify each failing test
and explain the reason for its failure, stating any uncertainty. Recommend whether the
test should be updated or the fix needs further iteration, then wait for the user's
decision before proceeding.

## Implementation and code style

- Follow clean-code principles, established project conventions, and coding best practices.
  Prefer simple designs with a clear purpose.
- Keep functions short, focused, and human-readable. Split cognitively complex logic into
  smaller functions with descriptive names that explain what they do, so the code reads
  like a book.
- Consider purpose, memory use, performance, and maintainability when implementing.
  This plugin shares IDE resources with many others; minimize unnecessary allocations,
  retained state, and background or UI-thread work.
- Prefer self-explanatory code over comments. When complex logic needs a comment,
  explain why it exists or why a particular approach is necessary, rather than what
  the code does.
- Leave touched code in better shape. Fix suboptimal logic or poor programming practices
  discovered during analysis as small, task-related drive-by improvements, preserving
  intended behavior. Point out each such improvement in the final implementation summary.

## Editing invariants

- Reuse `CsvHelper`, `CsvLexerFactory`, and file-specific attributes for parsing and
  serialization. Preserve custom/multicharacter separators, quoting/escaping, multiline
  values, comment rows, and final-line-break behavior rather than assuming comma-delimited text.
- Keep document content and PSI authoritative. Route edits through the existing updater
  and command infrastructure so text/table views stay synchronized and edits remain undoable.
  Read-only files and files with syntax errors permit table selection/copying, not mutation.
- Follow the existing search/reorder preparation and cancellation paths for expensive
  operations: prepare off the UI thread, validate that the source/settings are still current,
  and apply the document change on the IDE's required write thread.

## Table refresh timing

Swing table refresh timing tests inject `net.seesharpsoft.intellij.util.ManualUpdateScheduler`
from the test sources through the table model factory.
Advance its virtual clock to exercise the production 50-ms delay and cancellation/coalescing logic
without sleeps; the scheduler queues every request, so coalescing remains the model's responsibility.
The default scheduler still refreshes immediately in other unit tests.

## Table painting and read access

Table reads reuse existing IDE read access without entering another read action, since even redundant
read-action entry can be prohibited during painting. Table/header paints without read access are skipped
and coalesced into one application UI callback per table. The callback paints the visible scroll-pane
area immediately while it has read access; queuing `repaint()` instead would lose that access before
rendering. Hidden targets are skipped and disposed editors/projects expire the callback. This requires
no additional CSV-value cache or file-wide preprocessing, but a skipped paint can briefly leave stale/blank pixels.
Other PSI reads still acquire a read lock when necessary.

The inherited Swing/FlatLaf paint regressions remove actual IDE read access and reject explicit
read-action entry. A test-only scroll pane adapts the native display seam for headless execution;
the real table/header renderers still run, and deferred callbacks use the real application UI queue.

The relevant implementation is in `CsvTableModelBase.computeWithReadAccess`, `CsvTable.paint`,
and `CsvTableHeader.paint`; regression coverage is in `CsvTableEditorSwingTest` and its
`CsvTableEditorFlatLafTest` subclass.

## Updating the changelog

Update [CHANGELOG.md](CHANGELOG.md) only when the user asks for a changelog update or
preparation of a new version. Place new entries under `## Unreleased`, choosing the
subsection that fits the task:

| Subsection | Changes |
| --- | --- |
| `### Added` | New features or capabilities. |
| `### Changed` | Changes to existing behavior. |
| `### Removed` | Removed features or capabilities. |
| `### Fixed` | Bug fixes. |

Include the issue number (for example, `#1116`) when an entry is based on a GitHub issue
or ticket.

## Versioning and IDE support

Use the version pattern `VVV.X.x`:

- `VVV` is the earliest officially supported IntelliJ Platform release in JetBrains'
  three-digit build-branch notation: `252` means `2025.2`.
- `X` is the major version. Increment it once for every release that changes behavior,
  introduces incompatible code changes, changes defaults, adds or removes functionality,
  or introduces features or classes. It increases independently of `VVV` and does not
  reset when the minimum supported IDE version changes.
- `x` is the minor bugfix version. Increment it only for releases that restore existing
  intended behavior without adding, removing, or otherwise altering functionality beyond
  fixing bugs. Keep `X` unchanged for these releases; reset `x` to `0` whenever `X` increases.

The minimum supported IDE follows the official IntelliJ release cycle with a one-year
delay. At release preparation, use the latest officially released stable IDE's release
train from the previous year, not an EAP: if the current release is `262` (`2026.2`),
the minimum is `252` (`2025.2`). Advance this baseline as new official releases arrive.

For example, from `252.7.3`, a bugfix-only release becomes `252.7.4`, while a release
containing a new feature becomes `252.8.0`, assuming the IDE support baseline is unchanged.

## Preparing a new version

When asked to prepare a new version:

1. Whenever the user requests a new release, propose a version using
   [Versioning and IDE support](#versioning-and-ide-support). Base the proposal on the last
   released version, all changes since that release, and the current official IDE release.
   Explain the major or bugfix increment and any support-baseline change. If the user
   supplied a version, check it against the policy and resolve any mismatch before applying it.
2. Update `pluginVersion` in `gradle.properties`. The Gradle configuration in
   `build.gradle.kts` validates `VVV.X.x` and derives the minimum build from `VVV`,
   independently of the SDK selected for CI. It writes the version and
   `<idea-version since-build="VVV">` into the packaged `META-INF\plugin.xml`; use this
   mechanism and check the resulting descriptor rather than duplicating metadata in
   `src\main\resources\META-INF\plugin.xml`.
3. Align the default build target in `gradle.properties` and the compatibility targets
   in `.github\workflows` with the support baseline. Cover the earliest supported and
   current stable IDE releases in CI, retaining the existing IDE-product coverage.
   Review explicit `IDEA_VERSION` overrides, including `PullRequest.yml` and `CronEAP.yml`;
   EAP checks supplement, rather than replace, official-support coverage.
4. Move all changelog entries from `## Unreleased`, preserving their subsections, into a
   new section immediately below it. Use `## <version> - <Date>` with the release date,
   for example `## 252.8.0 - Oct 30, 2026`. Retain an empty `## Unreleased` section with
   the four subsections listed above.
5. Run all tests with `.\gradlew.bat test`, without test filters, and run the Gradle
   verification task with `.\gradlew.bat verifyPlugin`.
6. Summarize the test and plugin-verification findings, including failures, warnings,
   and any incomplete checks. Recommend whether the version is ready for release;
   if it is not, state what must be addressed before release.
7. Wait for the user's explicit release approval. Only after approval, create a release
   commit, push it, and open a pull request against `main`. If already on `main`, commit
   and push after approval but skip the pull request.

## Commits and pushes

Commit or push only when explicitly requested by the user, or after the user confirms
the release in the workflow above. Keep commit messages short and concise, and include
the issue number when the change is based on a GitHub issue.

Never add `Co-authored-by:` trailers, AI/tool attribution (including Copilot or any other
assistant), or other metadata anywhere in any commit message. Before committing, inspect
the final commit message and remove any such text.
