import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

/**
 * Shortcut for <code>project.findProperty(key).toString()</code>.
 */
fun properties(key: String) = project.findProperty(key).toString()
/**
 * Shortcut for <code>System.getenv().getOrDefault(key, default).toString()</code>.
 */
fun environment(key: String, default: String) = System.getenv().getOrDefault(key, default).toString()

val pluginVersion = providers.gradleProperty("pluginVersion").map { version ->
    require(Regex("""\d{3}\.\d+\.\d+""").matches(version)) {
        "pluginVersion must follow VVV.X.x (for example, 252.53.0), but was '$version'."
    }
    version
}

version = pluginVersion.get()

plugins {
    java
    id("idea")
    id("jacoco")
    id("com.github.kt3k.coveralls") version "2.12.2"
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.intellij.platform.grammarkit") version "2.19.0"
    id("org.jetbrains.changelog") version "2.5.0"
}

repositories {
    mavenLocal()
    mavenCentral()

    intellijPlatform {
        snapshots()
        defaultRepositories()
    }
}

jacoco {
    toolVersion = "0.8.16"
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "src/main/gen")
        }
        resources {
            srcDirs("src/main/resources")
        }
    }
    test {
        java {
            srcDirs("src/test/java")
        }
        resources {
            srcDirs("src/test/resources")
        }
    }
}

val platform = environment("IDEA_PLATFORM", properties("platform"))
val platformVersion = environment("IDEA_VERSION", properties("platformVersion"))

dependencies {
    implementation("net.seesharpsoft.sharping:sharping-commons:0.21.0")
    implementation("com.formdev:flatlaf:3.7.2")

    intellijPlatform {
        create(platform, platformVersion)
        
        bundledPlugins("Git4Idea", "org.jetbrains.plugins.github")
        bundledModule("intellij.spellchecker")
        
        pluginVerifier()
        zipSigner()
        
        jetbrainsRuntime()
        javaCompiler()

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("org.mockito:mockito-core:5.24.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

val requiredJavaVersion =
    if (intellijPlatform.productInfo.buildNumber.substringBefore('.').toInt() >= 262) 25 else 21

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(requiredJavaVersion))
    sourceCompatibility = JavaVersion.toVersion(requiredJavaVersion)
    targetCompatibility = JavaVersion.toVersion(requiredJavaVersion)
}

val generatorJavaLauncher = javaToolchains.launcherFor(java.toolchain)

intellijPlatform {
    pluginConfiguration {
        id = properties("pluginId")
        name = properties("pluginName")
        version = project.version as String
        ideaVersion {
            sinceBuild.set(pluginVersion.map { it.substringBefore('.') })
            untilBuild = provider { null }
        }
        changeNotes.set(provider { changelog.renderItem(changelog.get(project.version as String), Changelog.OutputType.HTML) })
    }

    publishing {
        token.set(environment("JI_TOKEN", ""))
        channels.set(listOf(environment("JI_CHANNELS", "Testing")))
    }

    pluginVerification {
        // The published Marketplace plugin ID cannot be renamed.
        freeArgs.addAll("-mute", "TemplateWordInPluginId")
        ides {
            create(
                environment("VERIFY_IDEA_PLATFORM", platform),
                environment("VERIFY_IDEA_VERSION", platformVersion)
            )
        }
    }
}

changelog {
    val projectVersion = project.version as String
    version.set(projectVersion)
    header.set("[$projectVersion] - ${org.jetbrains.changelog.date()}")
    groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(requiredJavaVersion)
    }

    generateParser {
        javaLauncher.set(generatorJavaLauncher)
        sourceFile.set(file("src/main/java/net/seesharpsoft/intellij/plugins/csv/Csv.bnf"))
        pathToParser.set("net/seesharpsoft/intellij/plugins/csv/parser/CsvParser.java")
        pathToPsiRoot.set("net/seesharpsoft/intellij/plugins/csv/psi")
        targetRootOutputDir.set(file("src/main/gen"))
        purgeOldFiles.set(true)
    }
    generateLexer {
        javaLauncher.set(generatorJavaLauncher)
        sourceFile.set(file("src/main/java/net/seesharpsoft/intellij/plugins/csv/CsvLexer.flex"))
        targetRootOutputDir.set(file("src/main/gen"))
        pathToClass.set("net/seesharpsoft/intellij/plugins/csv/CsvLexer.java")
        purgeOldFiles.set(false)
        
        dependsOn(generateParser)
    }
    compileJava {
        dependsOn(generateLexer)
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    processTestResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    test {
        configure<JacocoTaskExtension> {
            isEnabled = true
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }

        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        classDirectories.setFrom(instrumentCode)
        reports {
            xml.required = true
            html.required = true
        }
    }

    jacocoTestCoverageVerification {
        dependsOn(test)
        classDirectories.setFrom(instrumentCode)
    }
}
