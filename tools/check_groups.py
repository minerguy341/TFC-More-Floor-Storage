"""Transcription of PileGroup.at and the merged layout, checked over exhaustive/random arrangements."""
import random
from collections import deque
from itertools import product

MAX_SPAN = 5
MAX_MEMBERS = MAX_SPAN * MAX_SPAN


def touching(piles, start):
    found, seen, pending = [], {start}, deque([start])
    while pending:
        cell = pending.popleft()
        found.append(cell)
        if len(found) > MAX_MEMBERS:
            return None
        x, z = cell
        for nxt in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if nxt in piles and nxt not in seen:
                seen.add(nxt)
                pending.append(nxt)
    return found


def largest_rectangle(present, width, depth):
    best = None
    for x in range(width):
        for z in range(depth):
            for span_x in range(2, MAX_SPAN + 1):
                if x + span_x > width:
                    break
                for span_z in range(2, MAX_SPAN + 1):
                    if z + span_z > depth:
                        break
                    if not all(present[x + dx][z + dz]
                               for dx in range(span_x) for dz in range(span_z)):
                        break  # nothing deeper at this width is filled either
                    if best is None or span_x * span_z > best[2] * best[3] \
                            or (span_x * span_z == best[2] * best[3] and span_x > best[2]):
                        best = (x, z, span_x, span_z)
    return best


def group_at(piles, pos):
    """Returns (originX, originZ, spanX, spanZ) or None."""
    x, z = pos
    if not any(n in piles for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))):
        return None
    run = touching(piles, pos)
    if run is None or len(run) < 4:
        return None
    min_x = min(c[0] for c in run); max_x = max(c[0] for c in run)
    min_z = min(c[1] for c in run); max_z = max(c[1] for c in run)
    width, depth = max_x - min_x + 1, max_z - min_z + 1
    if width > MAX_SPAN or depth > MAX_SPAN:
        return None
    present = [[False] * depth for _ in range(width)]
    for cx, cz in run:
        present[cx - min_x][cz - min_z] = True
    while True:
        best = largest_rectangle(present, width, depth)
        if best is None:
            return None
        gx, gz, span_x, span_z = min_x + best[0], min_z + best[1], best[2], best[3]
        if gx <= x < gx + span_x and gz <= z < gz + span_z:
            return (gx, gz, span_x, span_z)
        for dx in range(best[2]):
            for dz in range(best[3]):
                present[best[0] + dx][best[1] + dz] = False


# ---------------------------------------------------------------- agreement
def check_agreement(piles):
    for pos in piles:
        group = group_at(piles, pos)
        if group is None:
            continue
        gx, gz, span_x, span_z = group
        for member in product(range(gx, gx + span_x), range(gz, gz + span_z)):
            assert member in piles, f"{group} claims a non-pile {member}"
            other = group_at(piles, member)
            assert other == group, f"{pos} says {group}, member {member} says {other}"


random.seed(7)
shapes = 0
for _ in range(4000):
    width, depth = random.randint(1, 7), random.randint(1, 7)
    piles = {(x, z) for x in range(width) for z in range(depth)
             if random.random() < random.choice([0.5, 0.75, 0.95, 1.0])}
    if piles:
        check_agreement(piles)
        shapes += 1
# and every solid rectangle up to 7x7, the case that has to work
for width, depth in product(range(1, 8), repeat=2):
    piles = {(x, z) for x in range(width) for z in range(depth)}
    check_agreement(piles)
print(f"agreement holds over {shapes} random arrangements and 49 solid rectangles")

# ---------------------------------------------------------------- expectations
def expect(piles, pos, want, why):
    got = group_at(set(piles), pos)
    assert got == want, f"{why}: at {pos} got {got}, wanted {want}"

expect([(0, 0)], (0, 0), None, "a lone pile is not a group")
expect([(0, 0), (1, 0)], (0, 0), None, "two side by side do not merge")
expect([(x, z) for x in range(2) for z in range(2)], (0, 0), (0, 0, 2, 2), "a two by two merges")
expect([(x, z) for x in range(3) for z in range(3)], (1, 1), (0, 0, 3, 3), "a three by three merges")
expect([(x, z) for x in range(5) for z in range(5)], (4, 4), (0, 0, 5, 5), "a five by five merges")
expect([(x, z) for x in range(3) for z in range(2)], (2, 1), (0, 0, 3, 2), "a three by two merges")
# the one the user hit: a pile started beside a square must leave the square alone
square_plus_one = [(x, z) for x in range(2) for z in range(2)] + [(2, 0)]
expect(square_plus_one, (0, 0), (0, 0, 2, 2), "a square keeps its shape when a pile appears beside it")
expect(square_plus_one, (2, 0), None, "the extra pile stays loose")
expect([(x, z) for x in range(6) for z in range(5)], (0, 0), None, "wider than five does not merge")
print("all shape expectations hold")

# ---------------------------------------------------------------- merged layout
GRID = [5, 4, 3, 3, 2, 1]
LAYERS, BASE_GRID, SPACING, ITEM_HALF = 6, 5, 0.1875, 0.125


def grid_of(layer, walls):
    return min(BASE_GRID, GRID[layer] + walls)


def offset_in_merged_layer(layer, coordinate, walls, span):
    grid = span * grid_of(layer, walls)
    return 0.0 if grid == 1 else (coordinate / (grid - 1) - 0.5) * ((grid - 1) * SPACING)


def merged_inset_of(layer, walls, span):
    grid = span * grid_of(layer, walls)
    half = (grid - 1) * SPACING / 2 + ITEM_HALF
    return max(0, round(16 * (span / 2 - half)))


worst = 0.0
for span_x, span_z in product(range(2, MAX_SPAN + 1), repeat=2):
    for walls in range(5):
        for layer in range(LAYERS):
            grid = grid_of(layer, walls)
            seen = set()
            for cell_x, cell_z in product(range(span_x), range(span_z)):
                for column, row in product(range(grid), repeat=2):
                    cx, cz = cell_x * grid + column, cell_z * grid + row
                    assert (cx, cz) not in seen, "two blocks claim the same spot in the merged grid"
                    seen.add((cx, cz))
                    # renderer: local coordinate within this block
                    x = (span_x / 2 - cell_x) + offset_in_merged_layer(layer, cx, walls, span_x)
                    z = (span_z / 2 - cell_z) + offset_in_merged_layer(layer, row + cell_z * grid, walls, span_z)
                    # ...converted back to a position within the group footprint
                    gx, gz = x + cell_x, z + cell_z
                    assert -1e-6 <= gx - ITEM_HALF and gx + ITEM_HALF <= span_x + 1e-6, \
                        f"lump escapes the group in x: {gx} of {span_x}"
                    assert -1e-6 <= gz - ITEM_HALF and gz + ITEM_HALF <= span_z + 1e-6, \
                        f"lump escapes the group in z: {gz} of {span_z}"
                    # the shape has to contain it
                    inset = merged_inset_of(layer, walls, span_x) / 16
                    worst = max(worst, inset - (gx - ITEM_HALF))
            assert len(seen) == span_x * span_z * grid * grid, "the merged grid is not filled exactly"
print(f"merged grid tiles exactly for every span, wall count and layer "
      f"(lumps overhang the shape by at most {worst * 16:.2f}px)")
