plugins {
    `kotlin-dsl`
}

dependencies {
    // The plugin marker, so the precompiled script plugin below can do `id("net.neoforged.moddev")`
    implementation("net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:2.0.107")
}
