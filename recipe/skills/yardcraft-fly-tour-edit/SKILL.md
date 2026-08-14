---
name: yardcraft-fly-tour-edit
description: >-
  Edit an existing camera fly tour in the Yardcraft Blender site model. Use when
  retiming, changing look-at targets, adjusting path waypoints, pause plateaus,
  gaze dwells, driver or walk eye height, tour-offset-keys, tour-look-spec,
  scrubbing timeline feedback, or small DSL tweaks to site_fly.
---

# Yardcraft fly tour — edit (retime, gaze, path tweaks)

Tune an existing narrative fly by editing data vectors in `src/yardcraft/site_fly.cljc` (`yardcraft.site-fly`). Prefer DSL edits over machinery changes.

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL-in-Blender, `bpy`, Yardcraft session bootstrap
3. **`clojure`** — shared Clojure conventions (structural edits, REPL-first)

After connect: confirm `user/init!` added `src/` to `sys.path` before requiring `yardcraft.*`. REPL session: **`basilisp-blender`**. Structural edits for Clojure forms.

DSL reference: [tour-dsl.md](./references/tour-dsl.md)

## Primary knobs (edit data, not machinery)

| Vector | Edit for |
|---|---|
| `tour-path-spec` | XY, surface kind, stair `step-i`, aerial `:tour/fly` Z |
| `tour-look-spec` | Frame → `:look-at/*` target; repeats = dwells |
| `tour-offset-keys` | Motion timing; plateaus = path pauses |
| `driver-eye-m` / `walk-eye-m` | Standing eye height (road/driveway vs deck/stair) |

**Respect human DSL edits.** Humans often tweak `tour-look-spec` and `tour-offset-keys` directly in the file — faster than narrating small gaze changes. Do not "fix" narrative or framing choices without asking.

## Reload after edits

After every structural edit:

```clojure
(require 'yardcraft.site-fly :reload)
(require '[yardcraft.site-fly :as fly])
(fly/ensure-fly-tour! site)
```

Calva load-file is also fine once Basilisp ≥0.5 (see basilisp-blender [upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md)).

Playback: Space / timeline scrub; `(fly/view-fly-camera!)` or Yardcraft panel **Fly cam**.

```
λ fly_edit_loop.
  edit_DSL → reload → ensure-fly-tour! → ask_human(playback) → done_when_happy
  | REPL_return ≠ tour_reads_right
```

**Feedback gate:** after rebuild/playback, ask the human to watch the beat you changed. Do not consider retimes, gaze, or path tweaks done until they confirm.

## Common edit recipes

### Change what we look at

Edit the `[:look-at/…]` target on the relevant `tour-look-spec` row.

### Longer dwell on same subject

Keep the same `:look-at/*` across a wider frame span (duplicate or extend rows). Optionally widen the matching offset plateau so the camera stays parked.

### Pause longer on path

Widen plateau in `tour-offset-keys`: same offset value, later end frame. May need to shift later look frames and final end frame (last offset key sets scene `frame_end`).

### Look right then left

Sequence dwells with enough frames between keys. Align offset plateaus if the camera should stop during the beat.

### Path height wrong

Check surface kind (`:tour/deck` vs `:tour/driveway` vs `:tour/road` vs `:tour/stair`) — not absolute Z. Only `:tour/fly` takes explicit Z.

### New look target

Add `look-*` helper from footprint/facts, branch in `resolve-look-at`, use new `:look-at/*` form in `tour-look-spec`.

### Eye height feels wrong

Adjust `driver-eye-m` (road/driveway) or `walk-eye-m` (deck/stair) — affects all standing waypoints of that class.

## Verify timing changes

After retiming, sample known frames in Blender or REPL, then ask the human to scrub/play the same beats:

- During offset plateaus: camera position stable on path
- At look keyframes: gaze hits intended landmark
- Done only when the human is happy with pacing and framing

## Invariants

- **Edit vectors first.** `ensure-fly-tour!` rebuilds constraints from specs — rarely touch `add-fly-camera!` etc.
- **Fly objects outside site-root.** `sync-site-hierarchy!` after partial site rebuilds does not parent `site-fly-*`.
- **Greenfield narrative redesign** (new tour from scratch, new story arc) → load **`yardcraft-fly-tour-create`** skill.
