# CONTRIBUTING
You can fork this project within GitHub, as described on http://help.github.com.
This fork will show up on your own github profile, and can be checked out to your local machine.
After doing any changes in the project and committing in to your fork repository - make sure, you are up-to-date with upstream.
After that, create a pull request (PR).

### Some guidance for a successful pull request

- Open PR for main branch only
- Reference an issue in your PR, hence do not open a PR without a corresponding issue
- Follow the single-purpose principle: Only one fix or feature in one PR
- Write tests for newly introduced features or fixes
- Apply a proper code style that fits to the existing one
- Do not change the plugin metadata information (e.g. name, version, description, etc.) in _src/main/resources/META-INF/plugin.xml_

### Some guidance for opening an issue

- Follow the single-purpose principle: Only one issue per thread
- For a bug reports:
  - Provide as many detailed information about your setup as possible
  - Point out the expected behavior in comparison with the current behavior
  - If possible, describe the steps on how to reproduce
- For feature requests:
  - Search for duplicates/already existing feature request
  - Describe the wanted feature together with its use case and benefits
  - Provide examples if possible

### Compiling the source code

Since the project has been migrated to the Gradle and [Gradle IntelliJ plugin](https://github.com/JetBrains/gradle-intellij-plugin),
the build process is quite simple. To build the plugin (including tests) just execute:

```
gradle build
```
    
All required dependencies like IntelliJ SDK, Grammar-Kit, etc are downloaded in the background and triggered properly
during the build process. To start an IDE for manual testing, execute:

```
gradle runIdea
```
 
All of the gradle tasks can be connected to the IntelliJ debugger.

### Automated issue triage

The **Opened Issue Validator** workflow (`.github\workflows\opened-issue-validator.yaml`)
runs only when an issue is first opened with a title starting with `[Automated Report]`.
It never runs for `reopened` events and does not check the changelog.

Before any AI step, the issue body must contain the exact string
`id=net.seesharpsoft.intellij.plugins.csv, version=<latest_release_version>,`
using the latest published GitHub release version (without a leading `v` on the tag).
If this string is absent, the report is closed as an outdated-plugin report with
update instructions. Missing or different plugin metadata does not fall through
to AI. Only matching reports proceed to duplicate detection against the five
most recently opened issues that are still open, excluding the report itself and pull requests.

### Automated EAP compatibility checks

The **EAP** workflow (`.github\workflows\CronEAP.yml`) runs on a schedule or manually
with `IDEA_PLATFORM=IU`, `IDEA_VERSION=latest`, and Java 25. The Gradle plugin's
[installer version selector](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#latest-version)
chooses the newest published IDEA build across all channels, including EAP, Beta,
and release candidates. Between EAP cycles, it can select the latest stable release.
`IU` also identifies the unified IntelliJ IDEA distribution from 2025.3 onward.

### Publishing a stable release

Every push to `Stable`, including a merged pull request, runs the **Publish** workflow.
After publishing to JetBrains Marketplace succeeds, it creates a GitHub release and tag
using `pluginVersion` from `gradle.properties` (for example, `252.54.0`). The tag points
to the commit that triggered the workflow. The release includes the built plugin ZIP
and automatically generated GitHub release notes.
