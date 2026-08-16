---
name: yardcraft-site-ui
description: >-
  Create, extend, reload, and maintain the Yardcraft Blender View3D N-panel UI
  (yardcraft.site-ui / site_ui.cljc). Use when the user mentions N-panel, site-ui,
  register!, unregister!, reload!, Yardcraft tab, Blender UI panel, View3D sidebar
  controls, sun date, time-of-day slider (0–24h), Set time, suggestion Show/Base,
  Fly cam, or demo-aware panel handlers after ensure-demo!.
---

# Yardcraft site UI (N-panel ↔ orchestration)

Complement RCFs with a View3D **Yardcraft** N-panel: thin Blender props/operators that call existing `yardcraft.site*` functions. Not a replacement for REPL exploration or promote rituals.

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL-in-Blender, `bpy`, Yardcraft session bootstrap
3. **`clojure`** — shared Clojure conventions (structural edits, REPL-first)

After connect: the `basilisp-blender` connect sequence runs `user/init!`; re-run if `src/` is missing from `sys.path` before requiring `yardcraft.*`. Session key: **`basilisp-blender`**. If Calva load-file alias bugs appear, Basilisp may be <0.5 — see basilisp-blender [upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md).

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

Confirm against the live file before extending:

| Control | Path |
|---|---|
| Sun date enum | `site/set-sun-date!` (demo: `demo/set-demo-date!`) |
| Time slider 0–24h (`FloatProperty` `TIME_ABSOLUTE` seconds) | preview on scrub → `site/preview-time-of-day!` (demo: `demo/preview-demo-time!`) |
| Slider `:text` | live HH:MM |
| **Set time** | `site/set-time-of-day!` (demo: `demo/set-demo-time!`) — commits sun aim + lounger re-orient |
| Suggestion enum (staged) + Show / Base | `sug/show!` / `sug/show-base!` — no promote / `set-base!` in panel |
| **Fly cam** | demo → `demo/ensure-orbit-fly!`; non-demo → `fly/ensure-fly-tour!` (narrative tour may be empty until authored — guard or no-op when `tour-path-spec` is empty) |

### Demo-aware handlers

When `(demo/demo-active?)` (demo objects present, e.g. after `(demo/ensure-demo!)`):

- Sun date, time scrub, and **Set time** route to `demo/set-demo-date!`, `demo/preview-demo-time!`, `demo/set-demo-time!` instead of `site/*`.
- **Fly cam** calls `demo/ensure-orbit-fly!` (orbit tour) instead of narrative `fly/ensure-fly-tour!`.
- `demo-stage-ui!` (and the one-shot `(demo/ensure-demo!)`) registers the panel via `(ui/register!)`, opens the VIEW_3D sidebar, and selects the Yardcraft tab — no separate register/show step needed for onboarding.

Non-demo: sun/time use `site` facts; **Fly cam** builds the narrative tour from `yardcraft.site-fly` (`ensure-fly-tour!`). Until a site-specific `tour-path-spec` / `tour-look-spec` is authored, the tour may be empty — panel code should guard before calling `ensure-fly-tour!`.

## RCF / reload contract

```clojure
(require 'yardcraft.site-ui :reload)
(require '[yardcraft.site-ui :as ui])
(ui/register!)
```

`(require '[ns :as alias] :reload)` does not bind `:as` — two-step required (basilisp verified quirk; same as site-suggestions).

Also: `(ui/unregister!)`, `(ui/reload!)` (unregister → reload ns → register). Call `(ui/register!)` once per Blender session after site is built; re-register after Blender restart.

## Adding a control

1. Prefer an operator button calling an existing named fn with `site`.
2. For props: update callback stays thin; heavy work behind **Set time** or a discrete enum change.
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
