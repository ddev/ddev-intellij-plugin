import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("org.jetbrains.changelog") version "2.5.0"
    id("org.jetbrains.intellij.platform") version "2.17.0"
    id("java")
    id("org.sonarqube") version "7.3.1.8318"
    id("jacoco")
}

val pluginVersion = environment("GIT_TAG_NAME").orElse("0.0.1-dev").get()
group = properties("pluginGroup").get()
version = pluginVersion

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Version declarations
    val gsonVersion = "2.14.0"
    val sentryVersion = "8.47.0"
    val junitVersion = "6.1.1"
    val junit4Version = "4.13.2"
    val junitPlatformVersion = "6.1.1"
    val mockitoVersion = "5.23.0"
    val assertjVersion = "3.27.7"
    val pluginVerifierVersion = "1.408"

    // Implementation dependencies
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("io.sentry:sentry:$sentryVersion")

    // Test dependencies
    testImplementation("junit:junit:$junit4Version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")

    intellijPlatform {
        phpstorm(properties("platformVersion"))
        pluginVerifier(pluginVerifierVersion)
        zipSigner()
        testFramework(TestFrameworkType.Platform)

        bundledPlugins(
            "Docker",
            "NodeJS",
            "com.intellij.database",
            "com.jetbrains.php",
            "com.jetbrains.plugins.webDeployment",
            "org.jetbrains.plugins.node-remote-interpreter",
            "org.jetbrains.plugins.phpstorm-docker",
            "org.jetbrains.plugins.phpstorm-remote-interpreter",
            "org.jetbrains.plugins.remote-run",
            "org.jetbrains.plugins.terminal"
        )

        // The 2026.2 test runtime does not include everything the bundled plugins above
        // require, leaving them (and transitively this plugin) disabled in tests, see
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2165 and
        // https://youtrack.jetbrains.com/issue/IJPL-248701
        testBundledPlugins(
            // Library/platform modules split out into separate plugins in 2026.2, providing
            // (in order): javax.activation for Docker; the Services view and its navbar
            // dependency for Docker; structure view for Docker's main module; the test runner
            // and coverage chain for NodeJS and PHP; YAML for Docker compose; SSH for the
            // remote interpreter plugins.
            "intellij.libraries.misc.plugin",
            "intellij.execution.serviceView.plugin",
            "intellij.navbar.plugin",
            "intellij.structureView.plugin",
            "intellij.testRunner.plugin",
            "org.jetbrains.plugins.yaml",
            "intellij.ssh.plugin",
            "intellij.bookmarks.plugin",
            // Structural search for the PHP plugin; grid core for the database plugin.
            "intellij.structuralSearch.plugin",
            "intellij.grid.core.plugin"
        )
    }
}

java {
    toolchain {
        // Must match the Java version of the target platform (2026.1 -> 21, 2026.2 -> 25).
        languageVersion.set(JavaLanguageVersion.of(25))
        // Matches the distribution used on CI; some other vendors (e.g. the Microsoft build)
        // break the instrumentCode task, see JetBrains/gradle-intellij-plugin#1240.
        vendor.set(JvmVendorSpec.AZUL)
    }
}

intellijPlatform {
    autoReload.set(true)

    pluginConfiguration {
        name = properties("pluginName")
        changeNotes.set(provider {
            changelog.getOrNull(pluginVersion)
                ?.let { changelog.renderItem(it, Changelog.OutputType.HTML) }
        })

        ideaVersion {
            // Pin to the verified version line: this plugin implements non-stable Docker plugin
            // APIs (connection configurators) whose surface changes between releases, so
            // compatibility with a new IDE version must be verified before claiming it.
            untilBuild = properties("platformVersion").map { version ->
                if (version.matches(Regex("""\d{4}\.\d+"""))) {
                    val (year, release) = version.split('.')
                    "${year.takeLast(2)}$release.*"
                } else {
                    // EAP/snapshot coordinates start with the branch number, e.g. 262-EAP-SNAPSHOT
                    "${version.takeWhile(Char::isDigit)}.*"
                }
            }
        }
    }

    publishing {
        token.set(environment("JETBRAINS_MARKETPLACE_PUBLISHING_TOKEN"))
        if (environment("PUBLISH_CHANNEL").orNull != null) {
            channels.set(listOf(environment("PUBLISH_CHANNEL").get()))
        }
    }

    signing {
        certificateChain.set(environment("JETBRAINS_MARKETPLACE_SIGNING_KEY_CHAIN"))
        privateKey.set(environment("JETBRAINS_MARKETPLACE_SIGNING_KEY"))
        password.set(environment("JETBRAINS_MARKETPLACE_SIGNING_KEY_PASSWORD"))
    }

    pluginVerification {
        ignoredProblemsFile = file("ignoredProblems.txt")
        // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#mutePluginVerifierProblems
        freeArgs = listOf(
            "-mute",
            "TemplateWordInPluginId,ForbiddenPluginIdPrefix,TemplateWordInPluginName"
        )
        ides {
            create(IntelliJPlatformType.PhpStorm, properties("platformVersion"))
            create(IntelliJPlatformType.WebStorm, properties("platformVersion"))
            create(IntelliJPlatformType.DataGrip, properties("platformVersion"))
            create(IntelliJPlatformType.IntellijIdeaUltimate, properties("platformVersion"))
        }
    }

    changelog {
        groups.empty()
        repositoryUrl = properties("pluginRepositoryUrl")
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    /* Tests */
    test {
        ignoreFailures = System.getProperty("test.ignoreFailures")?.toBoolean() ?: false

        useJUnitPlatform()
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            xml.required.set(true)
        }
    }
}

/* SonarCloud */
tasks.sonarqube {
    dependsOn(tasks.build, tasks.jacocoTestReport)

    sonarqube {
        properties {
            property("sonar.projectKey", "php-perfect_ddev-intellij-plugin")
            property("sonar.organization", "php-perfect")
            property("sonar.host.url", "https://sonarcloud.io/")
        }
    }
}
