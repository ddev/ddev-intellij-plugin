import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("org.jetbrains.changelog") version "2.5.0"
    id("org.jetbrains.intellij.platform") version "2.17.0"
    id("java")
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

        // The test runtime does not resolve dependencies of bundled plugins, which leaves the
        // database plugin disabled in tests without the grid plugin and its chart dependencies, see
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2165
        testBundledPlugins("intellij.grid.plugin", "intellij.charts", "com.intellij.platform.images")
    }
}

java {
    toolchain {
        // Must match the Java version of the oldest supported platform (2025.1 -> 21).
        languageVersion.set(JavaLanguageVersion.of(21))
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
            untilBuild = "263.*"
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
            // Every release line of the supported range, since the Docker plugin APIs this plugin
            // implements change between releases; the other IDEs at both ends of the range.
            listOf("2025.1", "2025.2", "2025.3", "2026.1", "2026.2", "263.5701.46").forEach { version ->
                create(IntelliJPlatformType.PhpStorm, version)
            }
            listOf("2025.1", "2026.2").forEach { version ->
                create(IntelliJPlatformType.WebStorm, version)
                create(IntelliJPlatformType.DataGrip, version)
                create(IntelliJPlatformType.IntellijIdeaUltimate, version)
            }
        }
    }

    changelog {
        groups.empty()
        versionPrefix.set("")
        repositoryUrl = properties("pluginRepositoryUrl")
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    processResources {
        // Read by the error reporter, which cannot query its own plugin descriptor on every
        // supported platform version.
        val version = pluginVersion
        inputs.property("version", version)
        filesMatching("ddev-integration.properties") {
            expand("version" to version)
        }
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
