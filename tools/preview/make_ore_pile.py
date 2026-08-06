"""Preview a pile built from per-item lump models, the way PileRenderer draws it.

Applies the same transform chain as the renderer - PileLayout for the slot, then PileModels'
measured fit (centre the model on its slot, stand it on the layer, shrink only if oversized) -
so the result shows whether a given lump model actually seats correctly in a pile.

    python3 tools/preview/make_ore_pile.py <models-dir> <item> <count> <out.json>
    python3 tools/preview/make_ore_pile.py .../block/pile/tfc/ore rich_native_copper 64 /tmp/pile.json
"""
import json
import pathlib
import sys

# --- mirrors common/.../PileLayout.java ---------------------------------------
GRID = [5, 4, 3, 3, 2, 1]
CUMULATIVE = [25, 41, 50, 59, 63, 64]
SPAN = [0.75, 0.56, 0.38, 0.38, 0.20, 0.0]
HEIGHT = [0.0, 0.155, 0.31, 0.465, 0.62, 0.775]

# --- mirrors common/.../PileModels.java and PileRenderer.java -----------------
LUMP_WIDTH = 0.25       # LumpGeometry.WIDTH
REFERENCE_SPAN = 6 / 16
MAX_WIDTH = 0.30
MAX_SPIN = 25.0


def layer_of(index):
    for layer, cum in enumerate(CUMULATIVE):
        if index < cum:
            return layer
    return len(CUMULATIVE) - 1


def offset_in_layer(layer, coordinate):
    grid = GRID[layer]
    return 0.0 if grid == 1 else (coordinate / (grid - 1) - 0.5) * SPAN[layer]


def jitter(seed, index, channel):
    mask = 0xFFFFFFFF
    h = (seed * 31 + index) & mask
    h = (h * 31 + channel) & mask
    h ^= h >> 15
    h = (h * 0x2C1B3C6D) & mask
    h ^= h >> 12
    return ((h & 0xFFFF) / 32767.5) - 1.0


def fit(model):
    """PileModels.measure: the scale and offset that seat this model on a lump's spot."""
    lo = [min(e["from"][a] for e in model["elements"]) / 16 for a in range(3)]
    hi = [max(e["to"][a] for e in model["elements"]) / 16 for a in range(3)]
    width = max(hi[0] - lo[0], hi[2] - lo[2])
    scale = LUMP_WIDTH / REFERENCE_SPAN
    if width * scale > MAX_WIDTH and width > 0:
        scale = MAX_WIDTH / width
    return scale, (-(lo[0] + hi[0]) / 2, -lo[1], -(lo[2] + hi[2]) / 2)


def build(model, count, seed):
    scale, offset = fit(model)
    elements = []
    for i in range(count):
        layer = layer_of(i)
        within = i if layer == 0 else i - CUMULATIVE[layer - 1]
        grid = GRID[layer]
        slot = (0.5 + offset_in_layer(layer, within % grid),
                HEIGHT[layer],
                0.5 + offset_in_layer(layer, within // grid))
        rotation = {"origin": [round(c * 16, 3) for c in slot], "axis": "y",
                    "angle": round(jitter(seed, i, 0) * MAX_SPIN, 2)}
        for element in model["elements"]:
            placed = {}
            for corner in ("from", "to"):
                placed[corner] = [
                    round((slot[a] + scale * (element[corner][a] / 16 + offset[a])) * 16, 4)
                    for a in range(3)]
            elements.append({"from": placed["from"], "to": placed["to"],
                             "rotation": rotation, "faces": element["faces"]})
    return {"texture_size": [16, 16], "textures": dict(model["textures"]), "elements": elements}


if __name__ == "__main__":
    models_dir, item, count, out = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4]
    model = json.loads((pathlib.Path(models_dir) / f"{item}.json").read_text())
    pile = build(model, count, seed=0x5F3759DF)
    pathlib.Path(out).write_text(json.dumps(pile, indent=1))
    scale, offset = fit(model)
    print(f"{item}: {len(model['elements'])} boxes x {count} lumps = {len(pile['elements'])} boxes, "
          f"scale {scale:.3f}, offset ({offset[0]:.3f}, {offset[1]:.3f}, {offset[2]:.3f})")
