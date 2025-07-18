plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.github.pixel365"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("org.jetbrains:marketplace-zip-signer:0.1.8")
    intellijPlatform {
        goland("2025.1")
        bundledPlugin("org.jetbrains.plugins.go")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    signPlugin {
        certificateChain.set(
            providers.environmentVariable("GOVERAGE_CERT").map { file(it).readText() }
        )
        privateKey.set(
            providers.environmentVariable("GOVERAGE_PRIVATE_KEY").map { file(it).readText() }
        )
        password.set(providers.environmentVariable("GOVERAGE_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.environmentVariable("GOVERAGE_TOKEN"))
    }
}
