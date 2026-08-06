# Clay pile previews

Renders the clay pile's geometry outside the game, so the layout can be judged without a
Minecraft client. `make_models.py` mirrors the constants in `PileLayout` and
`LumpGeometry` and emits one Minecraft model per case, which
[Pixel-Art-Aide](https://github.com/minerguy341/Pixel-Art-Aide) renders in 2:1 iso.

It also asserts that the four `merged_quadrant` calls tile the merged pyramid exactly - the
same invariant `PileRenderer`'s merged branch relies on.

```sh
git clone https://github.com/minerguy341/Pixel-Art-Aide /tmp/aide
pip install -r /tmp/aide/requirements.txt

python3 tools/preview/make_models.py /tmp/piles
(cd /tmp/aide && python3 -m aide render /tmp/piles/../../tools/preview/clay_lump.pxg -o /tmp/piles/clay_lump.png)
for m in pile_half pile_full pile_merged; do
  (cd /tmp/aide && python3 -m aide model /tmp/piles/$m.json --tex clay=/tmp/piles/clay_lump.png --scale 200 -o /tmp/piles/$m.png)
done
```

`make_ore_pile.py` previews a pile built from per-item lump models, applying the same transform chain as
the renderer. `clay_lump.pxg` is a stand-in for whatever clay item is in the pile; in game the lump is
textured from the middle of that item's own sprite.

Two things the preview cannot show, both because model elements are axis-aligned boxes:
the frustum is approximated by two stacked boxes, and the per-lump tilt is dropped
(an element may only rotate about one axis). The yaw jitter is reproduced exactly.
