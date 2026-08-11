# TFC More Floor Storage

Experimental TerraFirmaCraft addon (Minecraft **1.20.1** / Forge **47.3.x**) for denser **clay floor piles**, plus optional ingot-mesh tweaks.

## Clay pile (custom block)

Shift-click **pileable clays** to place a dedicated `clay_pile` block (not the metal ingot pile):

- `minecraft:clay_ball`
- `tfc:kaolin_clay`
- `tfc:fire_clay`

| | |
|--|--|
| Capacity | **128** (double an ingot pile’s 64) |
| Layout | **4×4** neat grid per layer, **8** layers |
| Mesh | Half-length square blobs (`7×4×7.5` texels) |
| Origin | First blob flush to the **block corner** (no centering / criss-cross) |
| Textures | Per piece: vanilla clay / white kaolin / fire clay block |

Mixed clay types in one pile are allowed (like mixed metal ingot piles). Click without shift to take from the top. Air-click knapping is unchanged.

## Tool leaning (from Claude branch)

Aim at the **side of a solid block** with a leanable tool and press TFC’s floor storage key (**V** by default) to prop the tool against the wall (Vintage Story style). Up to **4** tools per block.

- Tag: `tfcmorefloorstorage:leanable` (includes `#tfc:usable_on_tool_rack` plus vanilla tool tags)
- Click a leaned tool to take it back
- Toggle / range: `config/tfcmorefloorstorage-server.toml`

## Metal ingot mesh (optional)

Mixins can still override TFC ingot / double-ingot bar sizes via `config/tfcmorefloorstorage-client.toml`. Defaults match stock TFC. Press **F3+T** after edits.

## Dev

```bash
./gradlew runClient
```

Requires JDK **17**. TFC **3.2.23** via CurseMaven; Patchouli at runtime.
