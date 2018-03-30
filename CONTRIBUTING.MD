# CONTRIBUTING
You can fork this project within GitHub, as described on http://help.github.com.
This fork will show up on your own github profile, and can be checked out to your local machine.
After doing any changes in the project and commiting in to your fork repository - make sure, you are up-to-date with upstream.
After that, create a pull request.

### Some guidance for a successful pull request

- Open an issue to request a new feature, please do not just open a PR for an un-requested new feature
- Follow the single-purpose principle: Only one fix or feature in one PR
- Write a test for newly introduced features
- Please follow/apply a proper code style that fits to the existing one
- Do not change/commit the _src/main/resources/META-INF/plugin.xml_

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
