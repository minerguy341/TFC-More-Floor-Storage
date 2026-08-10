import java.util.Properties

/**
 * Convention plugin applied by every `versions/<mc>` subproject.
 *
 * It wires up a single-loader (NeoForge) build for one Minecraft version, sourced from the shared
 * `common/src/main` tree plus any per-version overrides in `versions/<mc>/src/main`. Overrides win:
 * dropping a file at the same relative path inside a version directory replaces the shared copy for
 * that version only, which is how API breaks between Minecraft versions are absorbed.
 */

plugins {
    java
    id("net.neoforged.moddev")
}

// ---------------------------------------------------------------------------------------------
// Properties
// ---------------------------------------------------------------------------------------------

// Per-version properties are read explicitly rather than relying on Gradle's project-property
// resolution, so that `versions/<mc>/gradle.properties` is unambiguously the source of truth.
val versionProperties = Properties().apply {
    val file = projectDir.resolve("gradle.properties")
    require(file.isFile) { "Missing ${file.absolutePath}" }
    file.inputStream().use { load(it) }
}

fun versionProp(key: String): String = versionProperties.getProperty(key)?.takeIf { it.isNotBlank() }
    ?: error("Missing property '$key' in $projectDir/gradle.properties")

fun optionalVersionProp(key: String): String? = versionProperties.getProperty(key)?.takeIf { it.isNotBlank() }

fun rootProp(key: String): String = providers.gradleProperty(key).orNull?.takeIf { it.isNotBlank() }
    ?: error("Missing property '$key' in the root gradle.properties")

val modId = rootProp("modId")
val modName = rootProp("modName")
val modVersion = System.getenv("VERSION") ?: rootProp("modVersion")

val mcVersion = versionProp("minecraftVersion")
val neoForgeVersion = versionProp("neoForgeVersion")
val tfcVersion = versionProp("tfcVersion")
val javaVersion = versionProp("javaVersion")

// ---------------------------------------------------------------------------------------------
// Source layout: shared tree overlaid with per-version overrides
// ---------------------------------------------------------------------------------------------

val commonDir = rootProject.layout.projectDirectory.dir("common/src/main")
val versionDir = layout.projectDirectory.dir("src/main")

val mergeJavaSources = tasks.register<Sync>("mergeJavaSources") {
    description = "Overlays this version's Java overrides onto the shared sources"
    // Later sources overwrite earlier ones, so the version-specific tree wins on a path collision.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(commonDir.dir("java"))
    from(versionDir.dir("java"))
    into(layout.buildDirectory.dir("mergedSources/java"))
}

val mergeResources = tasks.register<Sync>("mergeResources") {
    description = "Overlays this version's resource overrides onto the shared resources"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(commonDir.dir("resources"))
    from(versionDir.dir("resources"))
    into(layout.buildDirectory.dir("mergedSources/resources"))
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    description = "Expands neoforge.mods.toml and friends with the versions for this build"
    val replacements = mapOf(
        "modId" to modId,
        "modName" to modName,
        "modVersion" to modVersion,
        "modAuthors" to rootProp("modAuthors"),
        "modLicense" to rootProp("modLicense"),
        "modDescription" to rootProp("modDescription"),
        "modIssueTracker" to rootProp("modIssueTracker"),
        "minecraftVersionRange" to (optionalVersionProp("minecraftVersionRange") ?: "[$mcVersion]"),
        "neoForgeVersionRange" to (optionalVersionProp("neoForgeVersionRange") ?: "[$neoForgeVersion,)"),
        "tfcVersionRange" to (optionalVersionProp("tfcVersionRange") ?: "[$tfcVersion,)")
    )
    inputs.properties(replacements)
    expand(replacements)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(commonDir.dir("templates"))
    from(versionDir.dir("templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.named("main") {
    java.setSrcDirs(emptyList<Any>())
    java.srcDir(mergeJavaSources)
    resources.setSrcDirs(emptyList<Any>())
    resources.srcDir(mergeResources)
    resources.srcDir(generateModMetadata)
}

// ---------------------------------------------------------------------------------------------
// Build
// ---------------------------------------------------------------------------------------------

base {
    archivesName.set("$modId-neoforge-$mcVersion")
    group = rootProp("modGroup")
    version = modVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    mavenLocal()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
    exclusiveContent {
        forRepository { maven("https://www.cursemaven.com") }
        filter { includeGroup("curse.maven") }
    }
    maven("https://maven.blamejared.com") // Patchouli, which TerraFirmaCraft requires at runtime
}

neoForge {
    // A plain setter, not a Property - ModDevExtension.getVersion() returns String - so this has to be
    // an assignment. `version.set(...)` silently resolves to Project.version instead and fails to compile.
    version = neoForgeVersion

    optionalVersionProp("parchmentVersion")?.let { mappings ->
        parchment {
            minecraftVersion.set(optionalVersionProp("parchmentMinecraftVersion") ?: mcVersion)
            mappingsVersion.set(mappings)
        }
    }

    runs {
        configureEach {
            jvmArguments.addAll("-XX:+IgnoreUnrecognizedVMOptions", "-ea")
        }
        register("client") {
            client()
            gameDirectory.set(file("run/client"))
        }
        register("server") {
            server()
            gameDirectory.set(file("run/server"))
            programArgument("--nogui")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    // TerraFirmaCraft - a hard dependency, compiled and run against.
    //
    // Deliberately not transitive. The Modrinth maven generates a POM from whatever the project lists
    // as dependencies on Modrinth, so pulling TFC transitively drags its own copies of Patchouli and
    // friends onto the run classpath alongside the ones declared here. Two jars of the same mod in a
    // dev run get loaded as two mods, and setup then runs twice - which surfaces a long way from the
    // cause, as "Multiblock tfc:bloomery already registered" out of Patchouli.
    implementation("maven.modrinth:terrafirmacraft:$tfcVersion") { isTransitive = false }

    // So every mod on the run classpath is one this file names. Patchouli is required by TFC itself.
    optionalVersionProp("patchouliVersion")?.let { runtimeOnly("vazkii.patchouli:Patchouli:$it") }
}
