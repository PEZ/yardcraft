# Fly tour DSL reference (`yardcraft.site-fly`)

Data-driven camera tour in `src/yardcraft/site_fly.cljc`. Authored in **house-NW** coordinates; `ensure-fly-tour!` converts via `site-root` + house-center to world space.

## Three primary vectors

| Vector | Role |
|---|---|
| `tour-path-spec` | Camera path waypoints — `:tour/*` surface kinds or absolute `:tour/fly` |
| `tour-look-spec` | Gaze script `[frame [:look-at/…]]`; repeated targets = dwells |
| `tour-offset-keys` | Follow Path `offset_factor` keys `[frame offset]`; repeated offset = pause plateau |

Eye heights (private vars): `driver-eye-m` for road/driveway; `walk-eye-m` for deck/stair. Confirm values in the live file.

## Path kinds (`:tour/*`)

| Form | Z resolution |
|---|---|
| `[:tour/road x y]` | `road-surface-z` + `driver-eye-m` |
| `[:tour/driveway x y]` | `driveway-surface-z` + `driver-eye-m` |
| `[:tour/deck x y]` | deck top + `walk-eye-m` |
| `[:tour/stair x y step-i]` | stair tread `step-i` (0 = first below deck) + `walk-eye-m` |
| `[:tour/fly x y z]` | absolute model Z (aerial) |

## Look-at kinds (`:look-at/*`)

Resolved in `resolve-look-at` to a house-NW point `[x y z]` for the look-at empty. Extend with a `look-*` helper + branch when adding targets.

For `[:look-at/road x y]` / `[:look-at/driveway x y]`, **`x y` are the plan position of the gaze point**. Z = surface height at that XY + a fixed gaze height above the surface (see live `resolve-look-at` — typically ~1.2 m).

| Form | Typical use |
|---|---|
| `[:look-at/road x y]` | Gaze point on road strip |
| `[:look-at/driveway x y]` | Gaze point on driveway |
| `[:look-at/door-north]` | North door footprint (if present in facts) |
| `[:look-at/terrace-south x]` | Point along south terrace edge |
| `[:look-at/cafe]` | Seating / furniture cluster |
| `[:look-at/canopy]` | Terrace roof footprint |
| `[:look-at/tree "site-tree-…"]` | Planting from `:trees/plantings` |

Confirm available targets against `resolve-look-at` in `site_fly.cljc` — extend as your site grows.

## Offset plateaus

`tour-offset-keys` maps frames to normalized path offset (chord-length along resolved path). Same offset at consecutive frames parks the camera on the path while gaze can still animate.

## Scene objects

| Object | Role |
|---|---|
| `site-fly-path` | Bezier curve — Follow Path target |
| `site-fly-lookat` | Empty — Track To target |
| `site-fly-camera` | Camera with FOLLOW_PATH + TRACK_TO constraints |

Excluded from `site-root` parenting (`site_hierarchy` `site-fly-*` prefix). `sync-site-hierarchy!` does not affect fly objects.

## Rebuild

```clojure
(require 'yardcraft.site-fly :reload)
(require '[yardcraft.site-fly :as fly])
(fly/ensure-fly-tour! site)
```

Calva load-file works with Basilisp ≥0.5. Playback: Space / timeline scrub; `(fly/view-fly-camera!)` or Yardcraft panel **Fly cam**.
