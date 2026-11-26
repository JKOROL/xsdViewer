import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.3")
        testFramework(TestFrameworkType.Platform)
        // 2026.3 moved idea_rt.jar (needed by the test framework) into the Java plugin
        testBundledPlugin("com.intellij.java")

        // Add plugin dependencies for compilation here, for example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        // Keep 2026.1+ users while compiling against 2026.3
        ideaVersion { sinceBuild = "261" }
    }
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2026.1")
            create(IntelliJPlatformType.IntellijIdea, "2026.3")
        }
    }
    publishing {
        // Marketplace personal access token, never committed: set INTELLIJ_PUBLISH_TOKEN before running publishPlugin
        token = providers.environmentVariable("INTELLIJ_PUBLISH_TOKEN")
    }
}
