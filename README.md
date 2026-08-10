# TFC More Floor Storage

Experimental TerraFirmaCraft addon (Minecraft **1.20.1** / Forge **47.3.x**) focused on manipulating the **procedural ingot pile meshes** TFC draws in code.

## Why this exists

TFC ingot piles are not JSON/OBJ models. Blockstates point at custom geometry loaders:

- `assets/tfc/models/block/ingot_pile.json` → `{ "loader": "tfc:ingot_pile" }`
- `assets/tfc/models/block/double_ingot_pile.json` → `{ "loader": "tfc:double_ingot_pile" }`

Those loaders are `IngotPileBlockModel` / `DoubleIngotPileBlockModel`, which emit trapezoidal cuboids via `RenderHelpers.renderTexturedTrapezoidalCuboid` using each metal’s `softTextureId()`.

This mod mixins into those `render` methods and rebuilds the same layout with **config-driven sizes**.

## Defaults (vanilla TFC look)

| Pile | Grid / layer | Texel size (W×H×L) |
|------|--------------|--------------------|
| Ingot | 4×2, 8 per layer, alt. 90° | ~7×4×15 |
| Double ingot | 3×2, 6 per layer, alt. 90° | ~10×5×15 |

Capacity (`COUNT` / `DOUBLE_COUNT`) is **not** changed yet — only the mesh.

## Tweaking

Edit `config/tfcmorefloorstorage-client.toml` after first launch, then press **F3+T** (or relog) so static pile models rebuild.

Useful knobs:

- `enabled` — turn the override off to restore stock TFC meshes
- `sizeScale` — shrink/grow bars (try `0.85` for denser-looking piles)
- `layerHeightScale` — vertical packing
- `single_ingot.*` / `double_ingot.*` — per-axis texel sizes
- `matchVanillaScaleQuirk` — keep TFC’s nested `scale * (min + size)` math (`true`) or use cleaner bounds (`false`)

## Dev

```bash
./gradlew genIntellijRuns
./gradlew runClient
```

Requires a JDK **17** toolchain. TFC **3.2.23** is pulled from CurseMaven; Patchouli is a runtime dependency.
