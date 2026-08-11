# TFC Lean Mesh Visualizer

Local hand-editor for **tool leaning** poses used by `LeaningToolRenderer` in [TFC-More-Floor-Storage](https://github.com/minerguy341/TFC-More-Floor-Storage).

Suggested home on your machine:

`C:\Users\jthue\Documents\AI-Assisted Projects\TFC Mesh visualizer`

Copy this whole `mesh-visualizer` folder there (or clone the branch and keep working from a copy). The editor is standalone — it does not need Minecraft running.

## Assets

`textures/tfc/` contains **TerraFirmaCraft** item/block sprites (from the `1.20.x` GitHub tree) for private local preview. See `NOTICE.txt` — **do not redistribute** this folder.

## Run

ES modules need a tiny static server:

- **Windows:** double-click `serve.bat`
- **macOS / Linux:** `chmod +x serve.sh && ./serve.sh`

Then open `http://127.0.0.1:8765/`.

## What you can edit (per category)

| Control | Maps to renderer |
|---|---|
| Lean / tilt | `LEAN_ANGLE` via `Axis.XP` |
| Wall offset Z | `WALL_OFFSET` / `CLEAR_WALL_OFFSET` / `CLOSER_WALL_OFFSET` |
| Center Y | `CENTER_Y` |
| Scale | `SCALE` |
| Upright turn | `UPRIGHT_TURN` (flat FIXED sprites) |
| Flip-facing turn | `FLIP_FACING_TURN` (`lean_flip_facing` tag) |
| Slot spacing | `SLOT_SPACING` |
| Extra rot / nudge | freehand — export as comments until wired into Java |

Categories match the mod tags:

- **Default** — axes / picks / shovels / hoes
- **Clear wall** — swords, maces, rods, spindle, firestarter
- **Flip facing** — saws, chisels
- **Closer wall** — knives, tuyeres

## Export back into the mod

1. Tune a category until the side view seat looks right (green wall face = `z = -0.5`).
2. **Export Java** → paste constants into `LeaningToolRenderer`.
3. Or **Save JSON** and keep `lean-mesh-poses.json` next to this app as your source of truth.

Edits auto-save to browser `localStorage`.

## Pose order (must stay in sync with Java)

```
translate(0.5, 0, 0.5)
yaw to face wall          // -Z toward wall
translate(lateral, y, z)
XP(-leanAngle)            // tip into wall
ZP(flipFacingTurn)?       // knives/chisels/tuyeres/saws
ZP(uprightTurn)?          // flat sprites
scale(scale)
```
