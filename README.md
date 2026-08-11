# More Floor Storage

A [TerraFirmaCraft](https://github.com/TerraFirmaCraft/TerraFirmaCraft) addon about getting more use out
of the ground you walk on.

- **Piles.** TFC's ingot pile mechanic, applied to other things. Sneak-click the ground with a pile-able
  item and it stacks into a pile of up to 64 rather than being placed as a single loose item. The pile
  builds itself into a stepped pyramid as it fills, and empties from the top down. Two kinds ship:

  | Pile | Takes |
  | --- | --- |
  | Clay pile | clay balls, TFC fire clay, kaolin clay, unfired bricks |
  | Ore pile | TFC's small native deposits - the ones littering the ground - and its poor, normal and rich graded ores |

  Only piles of the same kind merge with each other, though a pile of any kind can be built on top of any
  merged group.

  **Merging.** Piles standing in a filled rectangle become one pile - anything from a 2x2 up to a 5x5,
  including oblongs like 2x3 and 3x5. They share one outline, one pyramid spanning the whole rectangle,
  and one capacity, and an item added to or taken from any of them goes to whichever member most needs
  it. Each block still keeps its own items and draws them as its cell of the larger pyramid, so nothing
  moves between them.

  A pile beside a completed rectangle leaves it alone and stays loose until it completes a bigger one:
  the biggest rectangle in a run of touching piles is taken as one pile, then the biggest in what is
  left, and so on. That is what keeps two overlapping pyramids from disagreeing about where the apex is.
  A run of more than 25 piles, or one spread more than five blocks in either direction, is more than one
  pile can be and does not merge at all.

  **How much fits** depends on what is holding the heap in. In the open a heap slumps into a pyramid and
  a block holds 64; a walled side lets it stay wider for longer, up to 150 a block with all four sides
  walled. A merged group is measured by its own perimeter, and a side only counts if it is walled along
  its whole length - so a group filling a pit is a straight column, and knocking one block out of the pit
  wall drops it to what three sides can hold. Whatever no longer fits trickles out through the gap.

  **Pits flood.** Sneak-click material into a pit that is closed in on every side and it goes to the
  emptiest spot on the pit floor, starting a pile there if there is not one yet, so the whole floor comes
  up together the way poured material would. One gap anywhere in the walls and it is a dip in the open
  ground rather than a pit, and material stays where it was put.

  **Stacking.** Piles do not stack on top of each other - a pyramid does not balance on the point of
  another one. Build upwards either against a wall, or on a merged group with every member full, which is
  flat enough on top to take a next tier. Take an item back off any member and it is no longer full,
  dropping whatever was resting on it.
- **Leaning tools.** Aim at the side of a solid block with a tool in hand and press TFC's floor storage
  key to stand the tool up against the wall, in the style of Vintage Story. Up to four tools lean side by
  side in one block, and the block has no collision, so you can walk right through them.
- **Sneak to pick up.** Sneak + TFC's floor storage key takes an item back *out* of whatever floor storage
  is under your crosshair - any of this mod's piles, leaning tools, TFC's placed items and shelves, and
  TFC's ingot and double ingot piles - instead of placing another item down.

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

### Mods in the dev run

Anything with a `<name>Version` property in `versions/<mc>/gradle.properties` is fetched and put on the
run classpath. Jade is there both to test against - it is the tooltip mod most likely to disagree with a
block that draws its own contents - and because the mod integrates with it.

For a mod the build cannot fetch - behind a blocked maven, never published to one, or a specific build
being chased - drop the jar in `versions/<mc>/libs` instead. It joins the run classpath the same way and
is not committed.

### Optional integrations

`compat/jade` is compiled only when `jadeVersion` is set, and Jade is `compileOnly`, so it never reaches
anyone's game through this mod. Jade finds the plugin by annotation scan and nothing else refers to those
classes, so without Jade installed they are never loaded.

Blanking `jadeVersion` drops the dependency **and** the source, which is also how a Minecraft version
with no Jade build yet still builds. The mapping from property to package is `optionalCompat` in the
convention plugin; add an entry there to make another integration optional the same way.

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
`useItemOn` gained extra parameters. Those touch `PileBlock`, `LeaningToolBlock`, and
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
| `piles.enableClayPiles` | `true` | Clay stacks into piles |
| `piles.enableOrePiles` | `true` | Ore stacks into piles |
| `tool_leaning.enableToolLeaning` | `true` | Tools lean against walls |
| `floor_storage.enableSneakPickup` | `true` | Sneak + the floor storage key picks up |
| `floor_storage.sneakPickupWholeStack` | `false` | Empty a whole pile in one press |
| `floor_storage.interactionRange` | `5.0` | How far the server looks for floor storage |

## Data pack hooks

- `#morefloorstorage:clay_pile_items` - what counts as clay. Defaults to clay balls, TFC's fire clay,
  kaolin clay, and unfired bricks.
- `#morefloorstorage:ore_pile_items` - what counts as ore. Defaults to `#tfc:small_ore_pieces` and
  `#tfc:metal_ores`. TFC's non-metal ore items - gems, coal, sulfur and friends - are deliberately left
  out; add `#tfc:ore_pieces` to this tag if you want them piling too.

  Adding a third kind of pile is a block, a block entity type, a tag, and one line in
  `MFSInteractions.PILE_KINDS`. Everything else - layout, merging, rendering, pickup - is shared.
- `#morefloorstorage:leanable` - what can be leaned against a wall. Defaults to the `#c:tools` family.

### Lump models

By default a lump in a pile is half a TerraFirmaCraft ingot - the same bar with the same bevel, cut to
half its length and drawn at half scale, through TFC's own `RenderHelpers`, so it is textured and shaded
exactly the way its ingot piles are. Courses are laid square rather than scattered, and turned across the
one below them.

There are two ways to change what an item looks like, both of them just a file. Give an item **real
geometry** by putting a block model at
`assets/morefloorstorage/models/block/pile/<item namespace>/<item path>.json` - so
`tfc:ore/rich_native_copper` picks up
`assets/morefloorstorage/models/block/pile/tfc/ore/rich_native_copper.json`. Everything under that
directory is loaded, from this mod or from a resource pack, so adding or replacing one needs no code and
no registration; a pack that writes to the same path overrides ours.

The model is measured, not assumed. It is seated by its own bounding box - centred on its spot in the
pile and standing on the layer - so it need not be centred in its block or built at any particular size.
A model six pixels across renders four pixels wide, a bigger one renders bigger, and only a model wide
enough to crowd its neighbours gets shrunk to fit. That means the size difference between a poor and a
rich chunk survives into the pile. Items drawn from a model of their own are also scattered a little, so
a heap of ore reads as a heap rather than as a grid.

Or give an item just a **texture**, by putting a model with no geometry in it at the same path:

```json
{ "parent": "block/block", "textures": { "particle": "minecraft:block/clay" } }
```

That keeps the built-in bar and wraps it in that texture instead of the item's icon, which matters
because an inventory icon is usually a blob with empty corners and looks like one when it is wrapped
round a bar. Clay, kaolin and fire clay ship this way. Anything with neither file falls back to its own
icon.

36 models ship for TFC's graded ores; see [tools/ore_models](tools/ore_models) for how they were built,
and `tools/preview/make_ore_pile.py` to preview a pile of any of them outside the game.

## Licence

MIT. See [LICENSE](LICENSE).
