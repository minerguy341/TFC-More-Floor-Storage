# Extrapolated ore models

TFC hand-authored a `tfc:block/groundcover/<ore>` model for each of the twelve small ore pieces -
the native deposits that litter the ground - but the poor, normal and rich items of those same ores
have only a flat `item/generated` icon. These scripts build the missing 36 models, in
`assets/morefloorstorage/models/block/ore/`.

- `analyse.py` measures what each native ground model actually puts on screen. A groundcover model
  does not show its whole sprite: every face carries a hand-picked UV rect, so weighting each rect
  by the area of the face carrying it gives the palette the model really reads as.
- `generate.py` builds a model per graded item. How much bigger each grade is is not invented:
  TFC's own heating recipes melt these items to 10 / 15 / 25 / 35 mB for small / poor / normal /
  rich, identically for all twelve ores, so a grade's model holds that many times the small piece's
  voxels - 1.5x, 2.5x and 3.5x.

  Growth happens on a voxel grid rather than by resizing boxes. The native cluster is voxelised,
  then voxels are added one at a time around it - each candidate must rest on something, so nothing
  floats - and scored to spread the chunk over the ground before piling it up. The result is merged
  back into non-overlapping boxes by greedy meshing. Two things fall out of working in voxels: the
  targets are hit exactly rather than overshot, and the model's summed box volume *is* its voxel
  count, with no double counting where boxes would otherwise engulf each other.

  `SPREAD` against `HEIGHT_PENALTY` is what stops an ore with a small footprint, like sphalerite,
  growing into a plain cube; `BORDER` keeps the cluster clear of the block edges. The current
  numbers put a rich chunk at roughly 6 wide by 3 tall.

  UVs are chosen rather than copied: every fully-opaque window of the required size in the graded
  sprite is scored against the native palette, and the best few are rotated between so faces vary.

```sh
python3 tools/ore_models/analyse.py  <tfc-checkout> tools/ore_models/palettes.json
python3 tools/ore_models/generate.py <tfc-checkout> tools/ore_models/palettes.json \
    common/src/main/resources/assets/morefloorstorage/models/block/ore
```

Two knobs matter. `SATURATION_WEIGHT` keeps a rich hematite chunk reading as hematite rather than as
the grey stone surrounding the ore in its icon. `CONTRAST_BONUS` is why the faces are streaky rather
than flat - matching only the mean colour produced dead-looking lumps for the low-saturation ores
(sphalerite, magnetite, tetrahedrite). At 2.0 the generated models land on the native models' own
contrast, about 35 stddev of luma, for an average palette distance of 10 - still far closer than the
~25 average (80 worst case, tetrahedrite) you get sampling the icon naively.

The models are not wired into the pile renderer yet; it still draws a generic frustum per lump.
