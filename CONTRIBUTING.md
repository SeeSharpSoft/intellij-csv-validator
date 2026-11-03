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

### Publishing a stable release

Every push to `Stable`, including a merged pull request, runs the **Publish** workflow.
After publishing to JetBrains Marketplace succeeds, it creates a GitHub release and tag
using `pluginVersion` from `gradle.properties` (for example, `252.54.0`). The tag points
to the commit that triggered the workflow. The release includes the built plugin ZIP
and automatically generated GitHub release notes.

Prepare a new plugin version and its changelog entry before each push to `Stable`;
an existing release tag cannot be reused for a different version or commit. The workflow
uses the built-in `GITHUB_TOKEN` with `contents: write` permission for GitHub Releases
and the existing `JI_TOKEN` secret for JetBrains Marketplace.
