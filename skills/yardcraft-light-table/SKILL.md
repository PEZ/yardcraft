---
name: yardcraft-light-table
description: >-
  Cover light-table / sketch-overlay workflow for tracing elevation contours and
  driveway edges over fitted hand sketches in the Yardcraft Blender site model.
  Use when the user mentions light table, sketch overlay, draft contours,
  elevation lines, site-sketch, capture-draft-contour, tracing over a
  photographed sketch, or example-source-images.
---

# Yardcraft light table (sketch → facts)

Trace real-world lines (elevation contours, driveway edges) over a photographed hand sketch fitted to the lot — a light table in the viewport.

Example overlays (when present) live under `example-source-images/` (e.g. elevation sketch, lot/road photo). Wire them via `:sketch/specs` in `yardcraft.site-data`; do not invent lot geometry from the examples alone.

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL-in-Blender, `bpy`, Yardcraft session bootstrap
3. **`clojure`** — shared Clojure conventions (structural edits, REPL-first)

Tooling: `src/yardcraft/site_sketch.cljc` (`yardcraft.site-sketch`, aliased `sketch` in `yardcraft.site`); draw staging in `yardcraft.site` (hide helpers: `yardcraft.site-draw`).

## Division of labor

| Role | Owns |
|---|---|
| Agent | Stage / seed / capture / rebuild in the REPL; promote captured values to `site_data` only when human happy |
| Human | Trace and adjust curves in Blender (top ortho); judges captured fit |

```
λ light_table_loop.
  REPL_stage → human_trace → REPL_capture → ask_human(viewport) → promote_site_data_when_happy
  | persist-site! ≡ session_only | file_facts ≡ after_human_ok
```

## Hand-window light table (no photo yet)

When you only have a paper sketch or a map on screen: place the physical sketch (or a tablet/window showing the map) behind/beside the monitor, or use a translucent overlay photo under `example-source-images/`. Fit once with `:sketch/specs` (`:corner-px` ↔ known lot corners), then trace as below. Prefer a fitted overlay over freehand world-space drawing.

## Stage → Trace → Capture

### 1. Stage (agent)

Works right after `clear-site!` once facts support massing:

```clojure
(prepare-contour-draw! site)                    ; fast massing (no terrain), flat pad, top-ortho
(sketch/ensure-sketch! site :elevation-lines)   ; fitted opaque overlay — the light table
(sketch/ensure-sketch! site :some-map-sketch)   ; any :sketch/specs entry
(sketch/seed-draft-contours-from-site! site)    ; editable curves from :terrain/contours
(sketch/seed-draft-contour! "draft-contour-49") ; or a blank curve (opens Edit Mode)
```

### 2. Trace (human)

Top ortho over the sketch; curves are 2D and z-locked. **E** extrude, **G** grab; toggle reference massing with `show/hide-draw-structures!`.

### 3. Capture (agent)

```clojure
(sketch/draft-contour-xy site 49)       ; preview points (house-NW, cm-rounded)
(sketch/draft-bezier-xy site "draft-road-inner") ; XY traces: handles honored + simplified
(sketch/capture-draft-contour! site 49) ; upsert into :terrain/contours + persist-site!
(ensure-site! site)                     ; rebuild on the captured data
(sketch/hide-drafts!)                   ; tuck the light table away
```

## Invariants

- **Fit is computed, not eyeballed.** `sketch-fit` best-fits rotation + uniform scale + translation from a spec's `:corner-px` ↔ `:lot/polygon-xy` (`:sketch/specs` in `yardcraft.site-data`) and reports per-corner residuals — treat large residuals as a bad fit, not as truth.
- **`draft-*` objects survive `clear-site!`.** The light table persists across rebuilds; `sketch/hide-drafts!` / `sketch/show-drafts!` toggle it.
- **`persist-site!` is session-only.** Capture updates the live `site` Var, not the file. After capture + rebuild, ask the human to check the viewport; promote captured values into `src/yardcraft/site_data.cljc` (structural edit) only when they are happy — the facts file stays canonical.
- **Do not invent site measurements.** Leave placeholders until confirmed in `site.md` / `site_data`.
