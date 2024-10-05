import org.gradle.internal.impldep.org.testng.reporters.XMLUtils
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

version = properties("pluginVersion")

plugins {
    java
    id("idea")
    id("jacoco")
    id("com.github.kt3k.coveralls") version "2.12.0"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.changelog") version "2.2.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

repositories {
    mavenLocal()
    mavenCentral()

    intellijPlatform {
        snapshots()
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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

    intellijPlatform {
        create(platform, platformVersion)
        
        bundledPlugins(properties("platformBundledPlugins").split(','))
        
        instrumentationTools()
        pluginVerifier()
        zipSigner()
        
        jetbrainsRuntime()
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.JUnit5)
    }

    testImplementation("org.mockito:mockito-core:5.14.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        id = properties("pluginId")
        name = properties("pluginName")
        version = project.version as String
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = provider { null }
        }
        changeNotes.set(provider { changelog.renderItem(changelog.get(project.version as String), Changelog.OutputType.HTML) })
    }

    publishing {
        token.set(environment("JI_TOKEN", ""))
        channels.set(listOf(environment("JI_CHANNELS", "Testing")))
    }
}

changelog {
    val projectVersion = project.version as String
    version.set(projectVersion)
    header.set("[$projectVersion] - ${org.jetbrains.changelog.date()}")
    groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
    generateParser {
        sourceFile.set(file("src/main/java/net/seesharpsoft/intellij/plugins/csv/Csv.bnf"))
        pathToParser.set("/net/seesharpsoft/intellij/plugins/csv/parser/CsvParser.java")
        pathToPsiRoot.set("/net/seesharpsoft/intellij/plugins/csv/psi")
        targetRootOutputDir.set(file("src/main/gen"))
        purgeOldFiles.set(true)
    }
    generateLexer {
        sourceFile.set(file("src/main/java/net/seesharpsoft/intellij/plugins/csv/CsvLexer.flex"))
        targetOutputDir.set(file("src/main/gen/net/seesharpsoft/intellij/plugins/csv"))
        targetFile("CsvLexer")
        purgeOldFiles.set(false)
        
        dependsOn(generateParser)
    }
    compileJava {
        dependsOn(generateLexer)
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    processTestResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
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
