plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

// TODO
// The property is injected at runtime via the ORG_GRADLE_PROJECT_ env-var convention — no secrets in the Gradle script.
//intellijPlatform {
//    publishing {
//        token = providers.gradleProperty("intellijPlatformPublishingToken")
//    }
//}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Djunit.platform.launcher.interceptors.enabled=false")
}

// The plugin has no settings registered for Search Everywhere indexing; skip the full headless IDE launch.
tasks.named("buildSearchableOptions") { enabled = false }
tasks.named("prepareJarSearchableOptions") { enabled = false }
tasks.named("jarSearchableOptions") { enabled = false }

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.1")
    }
}
