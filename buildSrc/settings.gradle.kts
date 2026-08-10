dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // Not exclusiveContent: the plugin is on the Gradle Plugin Portal as well, and pinning the
        // group here makes the NeoForged maven a hard requirement just to compile buildSrc.
        maven("https://maven.neoforged.net/releases") {
            content { includeGroupAndSubgroups("net.neoforged") }
        }
    }
}

rootProject.name = "buildSrc"
