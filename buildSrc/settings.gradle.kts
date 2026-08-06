dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://maven.neoforged.net/releases") }
            filter { includeGroupAndSubgroups("net.neoforged") }
        }
    }
}

rootProject.name = "buildSrc"
