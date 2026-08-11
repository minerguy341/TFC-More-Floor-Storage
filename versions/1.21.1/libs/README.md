# Drop-in mods for the dev run

Any `.jar` in this directory is added to the run classpath and loaded as a mod. Nothing here is
compiled against, and nothing here ships in the built mod.

Use it for mods worth testing against that the build cannot fetch: ones behind a maven this network
blocks, ones that never publish to a maven, or a specific build being chased down. Everything the
build *can* fetch belongs in `gradle.properties` instead, so that a fresh clone runs the same way.

Jars here are not committed - see the `.gitignore` beside this file.
