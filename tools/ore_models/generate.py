"""Extrapolate a ground-style model for each of TFC's 36 graded ore items.

TFC hand-authored a `groundcover/<ore>` model for the twelve small ore pieces - the ones that
litter the ground - but the poor, normal and rich items of the same ores have only a flat
`item/generated` icon. This builds the missing models.

Geometry comes from that ore's own groundcover model, so each ore keeps its native silhouette,
grown by grade. How much it grows is not invented: TFC's own heating recipes melt these items down
to 10, 15, 25 and 35 mB for small, poor, normal and rich, identically for all twelve ores, so a
grade's model is built to that many times the small piece's voxel count. Growth thickens the native
boxes upward, biggest footprint first, which piles the chunk into a mound rather than a slab.

UVs are chosen, not copied. For every face, each candidate window of the required size in the
graded sprite is scored against the palette the native ground model actually puts on screen
(from analyse.py), and the closest fully-opaque windows win. That is what keeps a rich hematite
chunk reading as hematite rather than as the grey stone that surrounds the ore in its icon.
"""
import json
import pathlib
import statistics
import sys

ORES = ["native_copper", "native_gold", "native_silver", "hematite", "cassiterite", "bismuthinite",
        "garnierite", "malachite", "magnetite", "limonite", "sphalerite", "tetrahedrite"]
GRADES = ["poor", "normal", "rich"]

# How many of the best-scoring windows to rotate between, so faces vary instead of all matching
WINDOW_CHOICES = 6
# Weight on matching the reference saturation, relative to plain RGB distance
SATURATION_WEIGHT = 140.0
# Reward for a window with internal contrast. Matching the mean colour alone gives flat, dead faces;
# at 2.0 the generated models land on the native models' own contrast (~35 stddev of luma)
CONTRAST_BONUS = 2.0

# Millibuckets of metal each grade melts down to, from TFC's own heating recipes. The same for every
# graded ore, and what the models' voxel counts are scaled against.
GRADE_YIELD = {"small": 10, "poor": 15, "normal": 25, "rich": 35}

# A chunk this tall stops reading as something lying on the ground
MAX_HEIGHT = 5
# Keep the cluster clear of the block edges, so neighbouring piles do not touch
BORDER = 5
# How much a candidate voxel is rewarded for having neighbours, against how much it is penalised
# for being high up. Spreading has to beat stacking or a small ore just grows into a cube.
SPREAD = 1.0
HEIGHT_PENALTY = 0.5


def saturation(colour):
    return (max(colour) - min(colour)) / (max(colour) + 1e-6)


def mean_of(texels):
    return tuple(sum(t[i] for t in texels) / len(texels) for i in range(3))


def score_window(texels, reference_mean, reference_saturation):
    mean = mean_of(texels)
    distance = sum((a - b) ** 2 for a, b in zip(mean, reference_mean)) ** 0.5
    saturation_gap = abs(saturation(mean) - reference_saturation)
    luma = [0.299 * t[0] + 0.587 * t[1] + 0.114 * t[2] for t in texels]
    contrast = statistics.pstdev(luma) if len(luma) > 1 else 0.0
    return distance + SATURATION_WEIGHT * saturation_gap - CONTRAST_BONUS * contrast


def ranked_windows(sprite, width, height, reference_mean, reference_saturation):
    """Fully-opaque windows of the given size, best palette match first."""
    px = sprite.load()
    width = max(1, int(round(width)))
    height = max(1, int(round(height)))
    candidates = []
    for y in range(sprite.height - height + 1):
        for x in range(sprite.width - width + 1):
            texels = [px[i, j] for j in range(y, y + height) for i in range(x, x + width)]
            if any(t[3] == 0 for t in texels):
                continue  # a hole in a lump reads as a hole, not as shape
            candidates.append((score_window(texels, reference_mean, reference_saturation), x, y))
    candidates.sort()
    return [(x, y) for _, x, y in candidates[:WINDOW_CHOICES]]


def voxelise(elements):
    """The unit cells a set of boxes occupies. A set, so overlapping boxes are counted once."""
    cells = set()
    for element in elements:
        (x0, y0, z0), (x1, y1, z1) = element["from"], element["to"]
        for x in range(int(x0), int(x1)):
            for y in range(int(y0), int(y1)):
                for z in range(int(z0), int(z1)):
                    cells.add((x, y, z))
    return cells


def scatter(cell):
    """A stable pseudo-random value per cell, to keep growth from looking machined."""
    h = (cell[0] * 73856093) ^ (cell[1] * 19349663) ^ (cell[2] * 83492791)
    return ((h ^ (h >> 13)) & 0xFFFF) / 65535.0


def grow(cells, target):
    """Add voxels around the cluster until it holds `target` of them.

    Candidates must sit on something, so nothing floats, and are scored to spread the chunk over
    the ground before piling it up - otherwise an ore with a small footprint, like sphalerite,
    just grows into a cube.
    """
    cells = set(cells)
    while len(cells) < target:
        frontier = {}
        for (x, y, z) in cells:
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0)):
                candidate = (x + dx, y + dy, z + dz)
                if candidate in cells or candidate in frontier:
                    continue
                cx, cy, cz = candidate
                if cy < 0 or cy > MAX_HEIGHT - 1 or not (BORDER <= cx < 16 - BORDER and BORDER <= cz < 16 - BORDER):
                    continue
                if cy > 0 and (cx, cy - 1, cz) not in cells:
                    continue  # no floating lumps
                neighbours = sum(((cx + ax, cy + ay, cz + az) in cells)
                                 for ax, ay, az in ((1, 0, 0), (-1, 0, 0), (0, 1, 0),
                                                    (0, -1, 0), (0, 0, 1), (0, 0, -1)))
                frontier[candidate] = SPREAD * neighbours - HEIGHT_PENALTY * cy + scatter(candidate)
        if not frontier:
            break
        cells.add(max(frontier, key=lambda c: (frontier[c], c)))
    return cells


def to_boxes(cells):
    """Greedily merge voxels back into as few boxes as possible. The result never overlaps, so the
    model's summed box volume is exactly the voxel count."""
    remaining = set(cells)
    boxes = []
    while remaining:
        x0, y0, z0 = min(remaining, key=lambda c: (c[1], c[2], c[0]))
        x1 = x0
        while (x1 + 1, y0, z0) in remaining:
            x1 += 1
        z1 = z0
        while all((x, y0, z1 + 1) in remaining for x in range(x0, x1 + 1)):
            z1 += 1
        y1 = y0
        while all((x, y1 + 1, z) in remaining
                  for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)):
            y1 += 1
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    remaining.discard((x, y, z))
        boxes.append({"from": [x0, y0, z0], "to": [x1 + 1, y1 + 1, z1 + 1]})
    return boxes


def graded_geometry(elements, grade):
    """The ore's own cluster, grown on a voxel grid until it holds this grade's share of metal."""
    native = voxelise(elements)
    target = int(len(native) * GRADE_YIELD[grade] / GRADE_YIELD["small"] + 0.5)
    boxes = to_boxes(grow(native, target))
    faces = {d: {} for d in ("down", "up", "north", "south", "west", "east")}
    for box in boxes:
        box["faces"] = {d: dict(faces[d]) for d in faces}
    return boxes


def volume(element):
    return ((element["to"][0] - element["from"][0])
            * (element["to"][1] - element["from"][1])
            * (element["to"][2] - element["from"][2]))


def face_size(element, direction):
    """The UV rect size a face needs, in texture pixels, from the box's own dimensions."""
    dx = element["to"][0] - element["from"][0]
    dy = element["to"][1] - element["from"][1]
    dz = element["to"][2] - element["from"][2]
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz)}[direction]


def build(ore, grade, native_model, sprite, reference):
    elements = graded_geometry(native_model["elements"], grade)
    window_cache = {}
    out = []
    face_index = 0
    for element in elements:
        faces = {}
        for direction in ("down", "up", "north", "south", "west", "east"):
            width, height = face_size(element, direction)
            key = (int(round(width)), int(round(height)))
            if key not in window_cache:
                window_cache[key] = ranked_windows(sprite, key[0], key[1],
                                                   reference["model_mean"], reference["model_saturation"])
            windows = window_cache[key]
            if not windows:
                continue  # no opaque window this size; drop the face rather than punch a hole
            x, y = windows[face_index % len(windows)]
            face_index += 1
            faces[direction] = {"uv": [x, y, x + key[0], y + key[1]], "texture": "#0"}
        if faces:
            out.append({"from": list(element["from"]), "to": list(element["to"]), "faces": faces})

    texture = f"tfc:item/ore/{grade}_{ore}"
    return {
        "__comment__": f"Extrapolated by More Floor Storage from tfc:block/groundcover/{ore}; "
                       f"UVs chosen to match that model's palette",
        "textures": {"0": texture, "particle": texture},
        "elements": out,
    }


if __name__ == "__main__":
    from PIL import Image

    tfc = pathlib.Path(sys.argv[1]) / "src/main/resources/assets/tfc"
    palettes = json.loads(pathlib.Path(sys.argv[2]).read_text())
    out_dir = pathlib.Path(sys.argv[3])
    out_dir.mkdir(parents=True, exist_ok=True)

    written = 0
    for ore in ORES:
        native = json.loads((tfc / "models/block/groundcover" / f"{ore}.json").read_text())
        for grade in GRADES:
            sprite = Image.open(tfc / "textures/item/ore" / f"{grade}_{ore}.png").convert("RGBA")
            model = build(ore, grade, native, sprite, palettes[ore])
            (out_dir / f"{grade}_{ore}.json").write_text(json.dumps(model, indent=1) + "\n")
            written += 1
        print(f"{ore:<16} poor/normal/rich from {len(native['elements'])} native boxes")
    print(f"{written} models written to {out_dir}")
