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
MAX_BOX_HEIGHT = 6


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


def graded_geometry(elements, grade):
    """The ore's own boxes, thickened until the chunk holds this grade's share of metal."""
    target = sum(volume(e) for e in elements) * GRADE_YIELD[grade] / GRADE_YIELD["small"]
    grown = [{"from": list(e["from"]), "to": list(e["to"]),
              "faces": {d: dict(f) for d, f in e.get("faces", {}).items()}} for e in elements]
    # Biggest footprint first, so the bulk of the chunk rises and the chips stay chips
    order = sorted(range(len(grown)), key=lambda i: -footprint(grown[i]))
    while sum(volume(e) for e in grown) < target:
        raised = False
        for i in order:
            if sum(volume(e) for e in grown) >= target:
                break
            box = grown[i]
            if box["to"][1] - box["from"][1] >= MAX_BOX_HEIGHT:
                continue
            box["to"][1] += 1
            raised = True
        if not raised:
            break  # everything is as tall as it is allowed to get
    return grown


def footprint(element):
    return ((element["to"][0] - element["from"][0])
            * (element["to"][2] - element["from"][2]))


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
            if direction not in element.get("faces", {}):
                continue
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
