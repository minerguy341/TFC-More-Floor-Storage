# TFC More Floor Storage

Experimental TerraFirmaCraft addon (Minecraft **1.20.1** / Forge **47.3.x**) for denser **clay floor piles**, plus optional ingot-mesh tweaks.

## Clay pile (custom block)

Shift-click **vanilla clay balls** to place a dedicated `clay_pile` block (not the metal ingot pile).

| | |
|--|--|
| Capacity | **128** (double an ingot pile’s 64) |
| Layout | **4×4** neat grid per layer, **8** layers |
| Mesh | Half-length square blobs (`7×4×7.5` texels), clay texture |
| Origin | First blob flush to the **block corner** (no centering / criss-cross) |

Click without shift to take clay off the top of the stack. Air-click knapping is unchanged.

## Metal ingot mesh (optional)

Mixins can still override TFC ingot / double-ingot bar sizes via `config/tfcmorefloorstorage-client.toml`. Defaults match stock TFC. Press **F3+T** after edits.

## Dev

```bash
./gradlew runClient
```

Requires JDK **17**. TFC **3.2.23** via CurseMaven; Patchouli at runtime.
