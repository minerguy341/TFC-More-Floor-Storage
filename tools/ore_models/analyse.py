"""Work out what palette each of TFC's groundcover ore models actually puts on screen.

A groundcover model does not show its whole sprite: every face carries a hand-picked UV rect,
so the colours you see are a weighted subset of the texture. Weighting each rect by the area of
the face carrying it gives the palette the model really reads as - which is the target the
extrapolated graded models have to hit.
"""
import json
import pathlib
import sys
from collections import Counter

ORES = ["native_copper", "native_gold", "native_silver", "hematite", "cassiterite", "bismuthinite",
        "garnierite", "malachite", "magnetite", "limonite", "sphalerite", "tetrahedrite"]
GRADES = ["poor", "normal", "rich"]

TFC = pathlib.Path(sys.argv[1]) / "src/main/resources/assets/tfc"


def load_sprite(name):
    from PIL import Image
    return Image.open(TFC / "textures/item/ore" / f"{name}.png").convert("RGBA")


def ground_model(ore):
    return json.loads((TFC / "models/block/groundcover" / f"{ore}.json").read_text())


def face_area(element, direction):
    """Area of one face of a box, in square model units."""
    dx = element["to"][0] - element["from"][0]
    dy = element["to"][1] - element["from"][1]
    dz = element["to"][2] - element["from"][2]
    return {"north": dx * dy, "south": dx * dy, "east": dz * dy, "west": dz * dy,
            "up": dx * dz, "down": dx * dz}[direction]


def sampled_palette(model, sprite):
    """Area-weighted colour histogram of the texels a model's UVs actually reach."""
    px = sprite.load()
    weights = Counter()
    for element in model["elements"]:
        for direction, face in element.get("faces", {}).items():
            u1, v1, u2, v2 = face["uv"]
            weight = face_area(element, direction)
            for y in range(int(min(v1, v2)), max(int(max(v1, v2)), int(min(v1, v2)) + 1)):
                for x in range(int(min(u1, u2)), max(int(max(u1, u2)), int(min(u1, u2)) + 1)):
                    if 0 <= x < sprite.width and 0 <= y < sprite.height:
                        colour = px[x, y]
                        if colour[3] > 0:
                            weights[colour[:3]] += weight
    return weights


def sprite_palette(sprite):
    px = sprite.load()
    weights = Counter()
    for y in range(sprite.height):
        for x in range(sprite.width):
            colour = px[x, y]
            if colour[3] > 0:
                weights[colour[:3]] += 1
    return weights


def mean_colour(weights):
    total = sum(weights.values()) or 1
    return tuple(sum(c[i] * w for c, w in weights.items()) / total for i in range(3))


def saturation(colour):
    return (max(colour) - min(colour)) / (max(colour) + 1e-6)


def distance(a, b):
    return sum((x - y) ** 2 for x, y in zip(a, b)) ** 0.5


if __name__ == "__main__":
    out = {}
    print(f"{'ore':<16}{'model cols':>11}{'model mean':>18}{'sat':>6}   "
          f"{'small mean':>18}{'sat':>6}   {'normal mean':>18}{'sat':>6}")
    for ore in ORES:
        model = ground_model(ore)
        small = load_sprite(f"small_{ore}")
        used = sampled_palette(model, small)
        used_mean = mean_colour(used)

        small_all = sprite_palette(small)
        normal_all = sprite_palette(load_sprite(f"normal_{ore}"))

        out[ore] = {
            "model_palette": {"%02X%02X%02X" % c: w for c, w in used.most_common()},
            "model_mean": used_mean,
            "model_saturation": saturation(used_mean),
        }
        fmt = lambda c: "(%3.0f,%3.0f,%3.0f)" % c
        print(f"{ore:<16}{len(used):>11}{fmt(used_mean):>18}{saturation(used_mean):>6.2f}   "
              f"{fmt(mean_colour(small_all)):>18}{saturation(mean_colour(small_all)):>6.2f}   "
              f"{fmt(mean_colour(normal_all)):>18}{saturation(mean_colour(normal_all)):>6.2f}")

    pathlib.Path(sys.argv[2]).write_text(json.dumps(out, indent=1))
