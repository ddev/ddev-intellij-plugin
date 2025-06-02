import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("org.jetbrains.changelog") version "2.2.1"
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("java")
    id("org.sonarqube") version "6.2.0.5505"
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
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("io.sentry:sentry:8.12.0")

    val junitVersion = "5.12.2"
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.0")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.assertj:assertj-core:3.27.3")

    intellijPlatform {
        phpstorm(properties("platformVersion"))
        pluginVerifier("1.384")
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
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
            ide(IntelliJPlatformType.PhpStorm, properties("platformVersion").get())
            ide(IntelliJPlatformType.WebStorm, properties("platformVersion").get())
            ide(IntelliJPlatformType.DataGrip, properties("platformVersion").get())
            ide(IntelliJPlatformType.IntellijIdeaUltimate, properties("platformVersion").get())
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
