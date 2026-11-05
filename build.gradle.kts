import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("org.jetbrains.changelog") version "2.5.0"
    id("org.jetbrains.intellij.platform") version "2.20.0"
    id("java")
    id("jacoco")
}

// Each variant compiles its own terminal runner against its platform.
class PlatformVariant(
    val platformVersion: String,
    val sinceBuild: String,
    val untilBuild: String,
    val verifiedVersions: List<String>,
    val testBundledPlugins: List<String>,
)

val platformVariants = mapOf(
    "251" to PlatformVariant(
        platformVersion = "2025.1",
        sinceBuild = "251",
        untilBuild = "253.*",
        verifiedVersions = listOf("2025.1", "2025.2", "2025.3"),
        testBundledPlugins = listOf("intellij.grid.plugin", "intellij.charts", "com.intellij.platform.images"),
    ),
    "261" to PlatformVariant(
        platformVersion = "2026.1",
        sinceBuild = "261",
        untilBuild = "263.*",
        verifiedVersions = listOf("2026.1", "2026.2", "2026.3"),
        testBundledPlugins = listOf(),
    ),
)
val platformVariantName = properties("platformVariant").get()
val platformVariant = platformVariants[platformVariantName]
    ?: throw GradleException("Unknown platformVariant '$platformVariantName', expected one of ${platformVariants.keys}")
val platformVersion = properties("platformVersion").orElse(platformVariant.platformVersion)

val pluginVersion = environment("GIT_TAG_NAME").orElse("0.0.1-dev").get()
group = properties("pluginGroup").get()
// Marketplace needs a distinct version for each compatibility range.
version = "$pluginVersion+$platformVariantName"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Version declarations
    val gsonVersion = "2.15.0"
    val sentryVersion = "8.63.0"
    val junitVersion = "6.1.3"
    val junit4Version = "4.13.2"
    val junitPlatformVersion = "6.1.3"
    val mockitoVersion = "5.24.0"
    val assertjVersion = "3.27.7"
    val pluginVerifierVersion = "1.411"

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
        phpstorm(platformVersion)
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

        // 2025.1 tests need explicit database plugin dependencies, see
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2165
        testBundledPlugins(platformVariant.testBundledPlugins)
    }
}

java {
    toolchain {
        // Both build platforms (2025.1 and 2026.1) use Java 21.
        languageVersion.set(JavaLanguageVersion.of(21))
        // Matches the distribution used on CI; some other vendors (e.g. the Microsoft build)
        // break the instrumentCode task, see JetBrains/gradle-intellij-plugin#1240.
        vendor.set(JvmVendorSpec.AZUL)
    }
}

sourceSets {
    main {
        java.srcDir("src/platform$platformVariantName/java")
    }
    test {
        java.srcDir("src/platform$platformVariantName/test")
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
            sinceBuild = platformVariant.sinceBuild
            untilBuild = platformVariant.untilBuild
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
        // The 251 runner needs terminal hooks deprecated in 2025.3.
        if (platformVariantName == "261") {
            failureLevel.set(failureLevel.get() + VerifyPluginTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES)
        }
        ignoredProblemsFile = file("ignoredProblems.txt")
        // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#mutePluginVerifierProblems
        freeArgs = listOf(
            "-mute",
            "TemplateWordInPluginId,ForbiddenPluginIdPrefix,TemplateWordInPluginName"
        )
        ides {
            // Every release line of the supported range, since the Docker plugin APIs this plugin
            // implements change between releases; the other IDEs at both ends of the range.
            platformVariant.verifiedVersions.forEach { version ->
                create(IntelliJPlatformType.PhpStorm, version)
            }
            listOf(platformVariant.platformVersion, if (platformVariantName == "251") "2025.3" else "2026.3").forEach { version ->
                create(IntelliJPlatformType.WebStorm, version)
                create(IntelliJPlatformType.DataGrip, version)
                create(IntelliJPlatformType.IntellijIdeaUltimate, version)
            }
            if (platformVariantName == "251") {
                // Latest 2025.3 patch release.
                create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.3.6.1")
            }
        }
    }

    changelog {
        version.set(pluginVersion)
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
