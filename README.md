# More Floor Storage

A [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft) addon about getting more use out
of the ground you walk on.

- **Clay piles.** TFC's ingot pile mechanic, applied to clay. Sneak-click the ground with clay and it
  stacks into a pile of up to 64 rather than being placed as a single loose item. The pile builds itself
  into a stepped pyramid as it fills, and empties from the top down.
- **Leaning tools.** Aim at the side of a solid block with a tool in hand and press TFC's floor storage
  key to stand the tool up against the wall, in the style of Vintage Story. Up to four tools lean side by
  side in one block, and the block has no collision, so you can walk right through them.
- **Sneak to pick up.** Sneak + TFC's floor storage key takes an item back *out* of whatever floor storage
  is under your crosshair - clay piles, leaning tools, TFC's placed items and shelves, and TFC's ingot and
  double ingot piles - instead of placing another item down.

Single loader (NeoForge), built for multiple Minecraft versions from one shared source tree.

## Building

```sh
./gradlew :versions:1.21.1:build     # one version
./gradlew buildAll                   # every version under versions/
./gradlew collectJars                # every version's jar, gathered into build/libs
./gradlew :versions:1.21.1:runClient # dev client
```

TerraFirmaCraft is resolved from the Modrinth maven, and Patchouli (which TFC requires at runtime) from
the BlameJared maven. Both are configured in the convention plugin; no manual jar dropping is needed.

## Project layout

```
common/src/main/           shared across every Minecraft version
  java/                      mod source
  resources/                 assets, data, mixin config
  templates/                 neoforge.mods.toml, expanded with per-version numbers at build time
versions/<mc>/             one build target per Minecraft version
  gradle.properties          the versions this target builds against
  build.gradle.kts           applies the mfs.mod-version convention plugin
  src/main/{java,resources}  optional, per-version overrides
buildSrc/                  the mfs.mod-version convention plugin
```

`versions/<mc>/src/main` is an **overlay** on `common/src/main`. At build time the two are merged, and a
file in the version tree replaces the shared file at the same relative path. That is how a Minecraft API
break is absorbed: copy the one affected class into the version that needs a different implementation and
leave every other version on the shared copy.

### Adding a Minecraft version

1. `mkdir -p versions/<mc>` and copy `build.gradle.kts` from an existing version verbatim.
2. Write `versions/<mc>/gradle.properties` with that version's `minecraftVersion`, `neoForgeVersion`,
   `javaVersion`, `tfcVersion`, and optionally `parchmentVersion` / `patchouliVersion`.
3. Build it. Anything that does not compile goes into `versions/<mc>/src/main/java` as an override.

`settings.gradle.kts` picks up any directory under `versions/` that has a `gradle.properties`, so there is
nothing else to register.

Things known to need an override past 1.21.1: `ItemInteractionResult` was folded back into
`InteractionResult` in 1.21.2, `DirectionProperty` became `EnumProperty<Direction>`, and `updateShape` /
`useItemOn` gained extra parameters. Those touch `ClayPileBlock`, `LeaningToolBlock`, and
`MFSInteractions`.

## How the keybind is shared with TFC

TFC sends its "place an item on the floor" packet whenever the floor storage key is held, without checking
whether the player is sneaking. `TFCPlaceBlockKeyMixin` takes that key event over at the head of TFC's
handler, but only while sneaking, so sneak + the key means "pick up" and an ordinary press still falls
through to TFC untouched. Leaning is hung off the same key without a mixin: TFC ignores anything that is
not the top face of a block, and leaning only ever targets a side face.

Both gestures send an empty packet and the server re-traces the player's own look vector, so a client can
ask to lean or pick up but never dictate what it gets.

## Configuration

`serverconfig/morefloorstorage-server.toml`, per world:

| Option | Default | |
| --- | --- | --- |
| `clay_piles.enableClayPiles` | `true` | Clay stacks into piles |
| `tool_leaning.enableToolLeaning` | `true` | Tools lean against walls |
| `floor_storage.enableSneakPickup` | `true` | Sneak + the floor storage key picks up |
| `floor_storage.sneakPickupWholeStack` | `false` | Empty a whole pile in one press |
| `floor_storage.interactionRange` | `5.0` | How far the server looks for floor storage |

## Data pack hooks

- `#morefloorstorage:clay_pile_items` - what counts as clay. Defaults to clay balls, TFC's fire clay,
  kaolin clay, and unfired bricks.
- `#morefloorstorage:leanable` - what can be leaned against a wall. Defaults to the `#c:tools` family.

## Licence

MIT. See [LICENSE](LICENSE).
