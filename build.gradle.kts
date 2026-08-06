// The root project is a container only - all real work happens in the `versions/<mc>` subprojects,
// which each apply the `mfs.mod-version` convention plugin from `buildSrc`.

tasks.register("buildAll") {
    group = "build"
    description = "Builds the mod jar for every active Minecraft version under versions/"
    dependsOn(subprojects.map { "${it.path}:build" })
}

tasks.register<Copy>("collectJars") {
    group = "build"
    description = "Copies every version's release jar into build/libs"
    dependsOn("buildAll")
    subprojects.forEach { from(it.layout.buildDirectory.dir("libs")) { include("*.jar"); exclude("*-sources.jar") } }
    into(layout.buildDirectory.dir("libs"))
}
