---
name: yardcraft-fly-tour-create
description: >-
  Create a new narrative camera fly tour from scratch in the Yardcraft Blender
  site model. Use when designing a first-time fly animation, authoring a greenfield
  site_fly tour, hybrid Follow Path + Track To setup, camera path narrative,
  tour-path-spec, tour-look-spec, or building site-fly-path from landmarks.
---

# Yardcraft fly tour — create (greenfield narrative)

Author a data-driven camera fly: Bezier path + look-at empty + TRACK_TO camera. All tour data lives in `src/yardcraft/site_fly.cljc` (`yardcraft.site-fly`).

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL-in-Blender, `bpy`, Yardcraft session bootstrap
3. **`clojure`** — shared Clojure conventions (structural edits, REPL-first)

After connect: confirm `user/init!` added `src/` to `sys.path` before requiring `yardcraft.*`. REPL session: **`basilisp-blender`**. Clojure form edits → structural editing.

DSL reference: [tour-dsl.md](../yardcraft-fly-tour-edit/references/tour-dsl.md)

## Architecture

| Piece | Object / mechanism |
|---|---|
| Path | `site-fly-path` — 3D Bezier; camera follows via FOLLOW_PATH `offset_factor` |
| Gaze | `site-fly-lookat` — empty; camera TRACK_TO `-Z` / `UP_Y` |
| Camera | `site-fly-camera` — hybrid Follow Path + Track To |
| Hierarchy | `site-fly-*` excluded from `site-root` (`site_hierarchy` `root-excluded-name?`) |

Coordinates: waypoints authored in **house-NW**; `house-nw->world` applies `site-root` matrix + house-center offset. **Do not invent site measurements** — sample from site facts, footprints, and surface helpers.

## Division of labor

| Role | Owns |
|---|---|
| Human | Narrative intent, landmark choices, pacing judgment watching playback |
| Agent | REPL sampling + rebuild cycle; draft/iterate specs; finalize `site_fly.cljc` only when human happy |

```
λ fly_create_loop.
  REPL_sample → rebuild(ensure-fly-tour!) → ask_human(playback) → finalize_DSL_when_happy
  | prefer(REPL_comment ∨ session_draft) → file_edit → always_ask_before_done
```

## Director workflow (CREATE)

### 1. Gather narrative

Ask the human: entry point, key beats (door, terrace, seating, canopy, …), dwell moments, aerial vs walking segments.

### 2. Sample landmarks (REPL)

Before authoring XY, query live site:

- Road / driveway surface Z at candidate points (`lot/road-surface-z`, `driveway/driveway-surface-z`)
- Footprints: doors, terrace edges, outbuildings, furniture, canopy roof
- Deck top, stair treads for `:tour/stair` step indices

### 3. Author path (`tour-path-spec`)

Pick surface kind per waypoint — Z is derived, not hand-tuned (except `:tour/fly`):

| Kind | When |
|---|---|
| `:tour/road` | Standing on the access road |
| `:tour/driveway` | Standing on driveway |
| `:tour/deck` | Walking on terrace slab |
| `:tour/stair` | Stair tread (`step-i` 0 = first below deck) |
| `:tour/fly` | Aerial — absolute Z in house-NW |

### 4. Author gaze (`tour-look-spec`)

`[frame [:look-at/…]]` rows. **Repeated target across frames = dwell.** See [tour-dsl.md](../yardcraft-fly-tour-edit/references/tour-dsl.md) for `:look-at/*` forms.

New target: add `look-*` helper from footprint/facts + branch in `resolve-look-at`.

### 5. Author motion timing (`tour-offset-keys`)

Frame → normalized path offset. **Plateaus** (same offset, later end frame) park the camera for pauses while gaze continues. Align plateaus with dwell frames in `tour-look-spec`.

### 6. Build and watch

```clojure
(require '[yardcraft.site-fly :as fly])
(fly/ensure-fly-tour! site)   ; rebuild path, lookat keys, camera; enters fly view
```

Space to play; scrub timeline. Yardcraft panel **Fly cam** re-enters camera view. **Ask the human** to watch playback — do not treat pacing/framing as good from REPL return values alone.

### 7. Iterate → finalize when happy

Adjust path XY, look frames, offset plateaus via REPL rebuild cycle (draft in `(comment …)` / session when practical; file edits OK while iterating). After each rebuild, ask for viewport feedback. Treat DSL in `site_fly.cljc` as done only when the human is happy.

Empty facts: tour helpers should no-op or fail clearly until landmarks exist — do not invent path coordinates.

## Reload after edits

```clojure
(require '[yardcraft.site-fly :as fly] :reload)
(fly/ensure-fly-tour! site)
```

Calva load-file is also fine once Basilisp ≥0.5 (see basilisp-blender [upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md)).

## Invariants

- **Data over machinery.** Greenfield work edits `tour-path-spec`, `tour-look-spec`, `tour-offset-keys` — not constraint setup code.
- **Fly objects stay outside site-root.** Partial site rebuilds need `sync-site-hierarchy!` for site meshes; fly tour is independent.
- **Surface kinds encode height.** Wrong altitude → wrong `:tour/*` kind, not raw Z (except `:tour/fly`).
- **Retiming / gaze tweaks on existing tour** → load **`yardcraft-fly-tour-edit`** skill.
