# Site reading notes

Companion to [`src/yardcraft/site_data.cljc`](src/yardcraft/site_data.cljc) (facts) and [`src/yardcraft/site.cljc`](src/yardcraft/site.cljc) (rebuild). Fill this file as you learn the lot. **Do not invent measurements.**

## Orientation (modeling)

**World** (Blender):

- **+Y** → true north (`site-north` along +Y when present)
- **+X** → east when unrotated; after `site-root` rotation, the house reads relative to true north via `:site/north-offset-deg`
- **+Z** → up
- **Z=0** = constructed house platform (= RH00 datum stored as `:terrain/z0-rh00` when you set one)
- World origin (horizontal) = **house center** after a full rebuild with house facts

**Local** (under `site-root`, house-aligned builders) — the **house-NW** frame:

- **0,0** at the NW house corner
- **+X** along the house (typically parallel to the access road)
- **+Y** toward the access road
- Builders author geometry in house-NW; `site-root` parents and rotates so world +Y stays true north

No Blender compass — use `site-north` + the axis gizmo when present.

## Sources

List maps, surveys, hand sketches, and photos here as you collect them. Example overlays for learning the light-table workflow may live under `recipe/example-source-images/` — they are not survey truth for your lot.

## Locked facts

Add rows only when confirmed (tape, survey, or human-validated light-table capture):

| Fact | Value |
|---|---|
| *(none yet — template starts empty)* | |

## Open questions

- What is the lot polygon in house-NW?
- What is `:terrain/z0-rh00` (or equivalent datum) for Z=0?
- Road edge, driveway, terrace, and outbuilding dimensions?
