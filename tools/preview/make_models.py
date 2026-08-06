"""Emit Minecraft models of a clay pile straight from ClayPileLayout's numbers.

Each item the block entity holds becomes one lump, positioned by the same arithmetic
ClayPileRenderer uses and spun by the same jitter hash, so what these render is what
the game will draw. Two departures, both forced by the model format being boxes only:
ClayLumpGeometry's frustum is approximated by two stacked boxes, and the small tilt
off level is dropped because an element may only rotate about one axis.
"""
import json
import sys

# --- mirrors common/.../ClayPileLayout.java -----------------------------------
GRID = [5, 4, 3, 3, 2, 1]
CUMULATIVE = [25, 41, 50, 59, 63, 64]
SPAN = [0.75, 0.56, 0.38, 0.38, 0.20, 0.0]
MERGED_SPAN = [1.75, 1.25, 0.78, 0.78, 0.44, 0.16]
HEIGHT = [0.0, 0.155, 0.31, 0.465, 0.62, 0.775]
MAX_ITEMS = CUMULATIVE[-1]

# --- mirrors common/.../ClayLumpGeometry.java ---------------------------------
LUMP_WIDTH = 0.25
LUMP_HEIGHT = 0.1875
LUMP_TAPER = 0.05
UV_MIN, UV_MAX = 0.25, 0.75

MAX_SPIN = 25.0  # ClayPileRenderer.MAX_SPIN


def layer_of(index):
    for layer, cum in enumerate(CUMULATIVE):
        if index < cum:
            return layer
    return len(CUMULATIVE) - 1


def index_in_layer(index):
    layer = layer_of(index)
    return index if layer == 0 else index - CUMULATIVE[layer - 1]


def offset_in_layer(layer, coordinate):
    grid = GRID[layer]
    return 0.0 if grid == 1 else (coordinate / (grid - 1) - 0.5) * SPAN[layer]


def offset_in_merged_layer(layer, coordinate):
    grid = 2 * GRID[layer]
    return (coordinate / (grid - 1) - 0.5) * MERGED_SPAN[layer]


def jitter(seed, index, channel):
    """ClayPileRenderer.jitter, in 32-bit unsigned arithmetic to match Java's int."""
    mask = 0xFFFFFFFF
    h = (seed * 31 + index) & mask
    h = (h * 31 + channel) & mask
    h ^= h >> 15
    h = (h * 0x2C1B3C6D) & mask
    h ^= h >> 12
    return ((h & 0xFFFF) / 32767.5) - 1.0


def block_pos_hash(x, y, z):
    """net.minecraft.core.Vec3i.hashCode."""
    return ((y + z * 31) * 31 + x) & 0xFFFFFFFF


def lump(cx, cz, layer, spin):
    """One item: two stacked boxes standing in for ClayLumpGeometry's frustum."""
    y0 = HEIGHT[layer] * 16
    tall = LUMP_HEIGHT * 16
    base_half = LUMP_WIDTH * 16 / 2
    top_half = base_half - LUMP_TAPER * 16
    uv = [UV_MIN * 16, UV_MIN * 16, UV_MAX * 16, UV_MAX * 16]
    faces = {d: {"uv": uv, "texture": "#clay"} for d in ("up", "north", "south", "west", "east")}
    rotation = {"origin": [round(cx, 3), round(y0, 3), round(cz, 3)], "axis": "y", "angle": round(spin, 2)}

    out = []
    for half, ylo, yhi in ((base_half, y0, y0 + tall / 2), (top_half, y0 + tall / 2, y0 + tall)):
        out.append({
            "from": [round(cx - half, 3), round(ylo, 3), round(cz - half, 3)],
            "to": [round(cx + half, 3), round(yhi, 3), round(cz + half, 3)],
            "rotation": rotation,
            "faces": faces,
        })
    return out


def single_pile(count, seed):
    """A pile standing on its own: offsets measured from the block's own centre."""
    out = []
    for i in range(count):
        layer = layer_of(i)
        within = index_in_layer(i)
        grid = GRID[layer]
        x = (0.5 + offset_in_layer(layer, within % grid)) * 16
        z = (0.5 + offset_in_layer(layer, within // grid)) * 16
        out += lump(x, z, layer, jitter(seed, i, 0) * MAX_SPIN)
    return out


def merged_quadrant(quadrant_x, quadrant_z, seed):
    """One block's 64 items, drawn as its quadrant of a pyramid spanning the 2x2.

    This is ClayPileRenderer's merged branch verbatim: offsets are measured from
    the centre of the two by two, which sits at the (1 - quadrant) corner of the
    block, and the block's own origin is then added back on.
    """
    out = []
    for i in range(MAX_ITEMS):
        layer = layer_of(i)
        within = index_in_layer(i)
        grid = GRID[layer]
        column, row = within % grid, within // grid
        local_x = (1 - quadrant_x) + offset_in_merged_layer(layer, quadrant_x * grid + column)
        local_z = (1 - quadrant_z) + offset_in_merged_layer(layer, quadrant_z * grid + row)
        out += lump((quadrant_x + local_x) * 16, (quadrant_z + local_z) * 16, layer,
                    jitter(seed, i, 0) * MAX_SPIN)
    return out


def merged_directly():
    """The same pyramid described in one go, as a check on the quadrant maths."""
    out = []
    for layer, n in enumerate(GRID):
        for row in range(2 * n):
            for column in range(2 * n):
                out += lump(16 + offset_in_merged_layer(layer, column) * 16,
                            16 + offset_in_merged_layer(layer, row) * 16, layer, 0.0)
    return out


def wrap(elements):
    return {"texture_size": [16, 16], "textures": {"clay": "mfs:block/clay_lump"},
            "elements": elements}


def centres(elements):
    return sorted(tuple(round((f + t) / 2, 3) for f, t in zip(e["from"], e["to"]))
                  for e in elements)


if __name__ == "__main__":
    out_dir = sys.argv[1]

    merged = []
    for quadrant_x in (0, 1):
        for quadrant_z in (0, 1):
            merged += merged_quadrant(quadrant_x, quadrant_z,
                                      block_pos_hash(quadrant_x, 64, quadrant_z))

    assert len(merged) == 4 * MAX_ITEMS * 2, len(merged)
    assert centres(merged) == centres(merged_directly()), "quadrant maths does not tile the pyramid"

    seed = block_pos_hash(0, 64, 0)
    for name, elements in [("pile_half", single_pile(MAX_ITEMS // 2, seed)),
                           ("pile_full", single_pile(MAX_ITEMS, seed)),
                           ("pile_merged", merged)]:
        with open(f"{out_dir}/{name}.json", "w") as fh:
            json.dump(wrap(elements), fh, indent=1)
        print(f"{name}: {len(elements) // 2} lumps")
    print("quadrants tile the merged pyramid exactly")
