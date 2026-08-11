---
name: yardcraft-site-ui
description: >-
  Create, extend, reload, and maintain the Yardcraft Blender View3D N-panel UI
  (yardcraft.site-ui / site_ui.cljc). Use when the user mentions N-panel, site-ui,
  register!, unregister!, reload!, Yardcraft tab, Blender UI panel, View3D sidebar
  controls, sun date/time scrub, canopy checkbox, suggestion Show/Base buttons,
  or framing view operators in the Yardcraft panel.
---

# Yardcraft site UI (N-panel ↔ orchestration)

Complement RCFs with a View3D **Yardcraft** N-panel: thin Blender props/operators that call existing `yardcraft.site*` functions. Not a replacement for REPL exploration or promote rituals.

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL-in-Blender, `bpy`, Yardcraft session bootstrap
3. **`clojure`** — shared Clojure conventions (structural edits, REPL-first)

After connect: confirm `user/init!` added `src/` to `sys.path` before requiring `yardcraft.*`. Session key: **`basilisp-blender`**. If Calva load-file alias bugs appear, Basilisp may be <0.5 — see basilisp-blender [upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md).

Tooling: `src/yardcraft/site_ui.cljc` (`yardcraft.site-ui`, alias `ui`).

## Division of labor

| Role | Owns |
|---|---|
| Agent | Extend / prototype via REPL `(ui/register!)` / reload; thin handlers → named orchestration; commit UI source as done only after human panel check |
| Human | Uses the panel in Blender; judges viewport + panel UX; confirms destructive / promote work still via RCF |

```
λ site_ui_loop.
  REPL_register! → exercise_control → ask_human(panel ∧ viewport) → promote_source_when_happy
  | ¬done ≡ file_edit_only
```

Find the UI: **3D Viewport → N (sidebar) → tab Yardcraft**.

## Architecture

- Single ns `yardcraft.site-ui` → `src/yardcraft/site_ui.cljc`.
- Explicit `(register!)` / `(unregister!)` / `(reload!)` — **not** on ns load.
- Thin handlers → existing orchestration (`yardcraft.site`, `yardcraft.site-terrace`, `yardcraft.site-suggestions`, `yardcraft.site-viewport`, `yardcraft.site-fly`, …).
- Site via `yardcraft.site-data` `site` (orchestration-adjacent). Domain / site namespaces must **never** require `site-ui`.
- `bpy-mod` via `(python/__import__ "bpy")` — reload-safe; never rely on a stale `(:import bpy)` gensym.
- Operators return `#py #{"FINISHED"}` (not a Basilisp PersistentSet).
- PropertyGroup: put `bpy.props` in the class dict **and** `__annotations__` (Blender ≥ 5.2.0 LTS).
- Panels / Operators: `basilisp-blender.utils/class-make*` with `^{:default …}` for `bl_*` attrs.
- Scene props live on `scene.yardcraft` — **re-fetch** after `(ui/register!)` / unregister; identity is not stable.
- `suppress-updates?*` while seeding props on register (and when programmatically resetting enum state).
- Enum ids: ASCII alphanumeric + underscore only (hyphens and leading digits fail). Map suggestion keywords ↔ underscores; date enums use named ids → ISO maps.

## Current controls

Confirm against the live file before extending. As of skill authoring:

| Control | Path |
|---|---|
| Sun date enum | `site/set-sun-date!` |
| Time scrub (`FloatProperty` `TIME_ABSOLUTE` seconds) | preview → `site/preview-time-of-day!`; **Apply** → `site/set-time-of-day!` (loungers + persist) |
| Slider `:text` | live HH:MM; keep Apply for commit |
| Canopy checkbox | `terrace/set-terrace-roof-covering-visible!` |
| Suggestion enum (staged) + Show / Base | `sug/show!` / `sug/show-base!` — no promote / `set-base!` in panel |
| View | `viewport/frame-lot-top!`, `frame-lot-top-house!`, `frame-house-south!`, `frame-house-east!` |
| Fly cam | `fly/view-fly-camera!` |

## RCF / reload contract

```clojure
(require 'yardcraft.site-ui :reload)
(require '[yardcraft.site-ui :as ui])
(ui/register!)
```

Basilisp trailing `:reload` may drop `:as` — two-step required (same as site-suggestions).

Also: `(ui/unregister!)`, `(ui/reload!)` (unregister → reload ns → register). Call `(ui/register!)` once per Blender session after site is built; re-register after Blender restart.

## Adding a control

1. Prefer an operator button calling an existing named fn with `site`.
2. For props: update callback stays thin; heavy work behind **Apply** or a discrete enum change.
3. Register new classes in `register!` `classes` vector; unregister reverse order.
4. Seed under `suppress-updates?*`.
5. REPL-verify: register twice (idempotent) + exercise the control in the panel.
6. **Ask the human** to try the control in the Yardcraft N-panel and check the viewport — treat UI source as done only when they are happy.
7. **Out of scope for panel:** `ensure-site!`, quote-plan, sketch, `promote-plan`, `set-base!`.

## Gotchas

- Numeric slider value text cannot be fully blanked (Blender widget); use `TIME_ABSOLUTE` + HH:MM as prop `:text`.
- `EnumProperty` items: pass a static list at class build (`(suggestion-items nil nil)`), not a Basilisp fn as RNA callback.
- Do not invent site measurements.
- Structural edits for `.cljc` forms.
