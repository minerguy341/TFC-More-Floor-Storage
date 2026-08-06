pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://maven.neoforged.net/releases") }
            filter { includeGroupAndSubgroups("net.neoforged") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "TFC-More-Floor-Storage"

// Every directory under `versions/` that carries its own gradle.properties is an active build target.
// Adding support for a new Minecraft version is therefore a matter of creating the directory - no edits here.
file("versions")
    .listFiles()
    ?.filter { it.isDirectory && File(it, "gradle.properties").isFile }
    ?.sortedBy { it.name }
    ?.forEach { include("versions:${it.name}") }
