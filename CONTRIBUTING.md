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

Swing table refresh timing tests inject `net.seesharpsoft.intellij.util.ManualUpdateScheduler`
from the test sources through the table model factory.
Advance its virtual clock to exercise the production 50-ms delay and cancellation/coalescing logic
without sleeps; the scheduler queues every request, so coalescing remains the model's responsibility.
The default scheduler still refreshes immediately in other unit tests.

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
