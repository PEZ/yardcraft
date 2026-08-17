---
name: basilisp-blender
description: >-
  Basilisp inside Blender via basilisp-blender: nREPL from Blender's main loop,
  bpy interop patterns, project directory setup, scene-safe REPL workflows. Use
  when working with Blender, bpy, basilisp-blender, .lpy scripts that touch the
  scene, or an nREPL server running inside Blender.
---

# basilisp-blender

[basilisp-blender](https://github.com/ikappaki/basilisp-blender) runs Basilisp in Blender and serves nREPL so your editor (Calva/CIDER) can drive `bpy` live.

Requires the **`basilisp`** skill for dialect/interop fundamentals.

## When to use

- Blender scene automation or modeling from Basilisp
- nREPL connected to Blender (Output properties → nREPL panel)
- Reading/writing `.lpy` that `(:import bpy …)`

## Critical runtime constraint

Blender’s scripting API is **not thread-safe**. The nREPL server listens on a background thread, but client evaluations are **queued and drained on a `bpy` timer** on the main loop.

```
λ blender_nrepl.
  evals ≡ main_loop_timer_queue
  | ¬assume(immediate_parallel_bpy)
  | interval_sec controls drain cadence (default ~0.2s)
```

Slow evals block the UI; tiny iterative REPL steps feel better than giant scripts.

## Project directory

When the nREPL server starts with a **Basilisp Project Directory** set, that directory becomes cwd, is added to `sys.path`, and (if missing) gets:

- `basilisp.edn` — editor project marker
- `scratch.lpy` — playground
- `.nrepl-port` — port file for editor clients (overwritten each start)

## Yardcraft session bootstrap

**Install/upgrade (do-mode):** **Observe first** — already installed at the PEZ target? skip. Need install/upgrade? ask quit **only if Blender is running**, then CLI `extension install-file` (Install From Disk only when CLI fails) — [references/upgrade-basilisp.md](references/upgrade-basilisp.md).

Connect (this repo):

- Calva: connect sequence **`basilisp-blender`** (named in `.vscode/settings.json`; not generic `basilisp` alone); session key `basilisp-blender`. The sequence runs `user.lpy` / `(user/init!)` so `src/` is on `sys.path` — re-run `(user/init!)` only after a Blender restart or if `sys.path` lost `src/` before requiring `yardcraft.*`
- Other nREPL clients: connect via **`.nrepl-port`**, then `(load-file "user.lpy") (user/init!)`
- Sources are `.cljc` under `src/yardcraft/`; snake_case files → kebab-case namespaces
- Host-side asset work: Babashka session `bb`; Blender/`bpy` stays on `basilisp-blender`
- **VS Code family:** before agent-driven connect/demo, **this chat must be able to eval** — depth in [vscode-family.md](../yardcraft-setup/references/vscode-family.md) (known first-open glitch → **Developer: Reload Window**)

Light-table / sketch overlay: load **`yardcraft-light-table`**.

## Agent workflow

```
λ blender_agent.
  scene/census → small_fn → eval → scene/render-check! → inspect_image → ask_human(Blender_viewport) → promote_files_when_happy
  | clear/delete/overwrite → human_confirm
  | prefer(named_fns ∧ comment_blocks ∧ session_Vars) > edit_source_first
  | execution_success ≠ visual_correctness
```

1. **Orient:** `(require '[yardcraft.scene :as scene])` then `(scene/census)` / `(scene/object-info n)`.
2. **Make it happen in the REPL:** small helpers / `(comment …)` / existing `ensure-*!` paths so the change is visible in Blender — prefer that over editing source first.
3. **Self-check visually:** `(scene/render-check!)` for the current fly/orbit frame; `(scene/render-check! path {:look-at \"site-…\"})` for a named part; `(scene/ui-check!)` for the N-panel. Put each PNG **in the chat** (absolute `:path` so it renders). A path string is not the handoff. Correct obvious mismatches before asking. Do not add a camera or move `site-fly-camera`.
4. **Ask for feedback:** stop and ask the human to check the Blender viewport (and panel). The inspection image(s) go in that same bubble. They complement rather than replace human judgment.
5. **Promote to files only when the human is happy** — durable facts/builders/specs into source; until then keep the experiment in the REPL / session.
6. **Units & coordinates:** Blender units; don’t invent real-world patio measurements — ask.

### Safe visual self-check

```
λ render_check.
  scene/census ∨ scene/object-info → scene/render-check! → inspect_actual_png
  | part_of_scene → {:look-at name-or-xyz}  ; current fly/orbit frame → bare render-check!
  | ¬DIY_camera ¬move(site-fly-camera)
  | png_appears_in_chat(:path) before(human_handoff)  ; ¬path_string_only ¬prose_only
  | render-check! restores(camera ∧ render_settings)
  | compare: two paths, same look-at, only design state changes; restore suggestion/base after
  | inspect_REPL_errors_after(side_effects)
  | human_viewport_judgment remains_required
```

1. Query first: `(require '[yardcraft.scene :as scene])` then `(scene/census)` or `(scene/object-info n)`. Note active suggestion/base if you will compare.
2. **Current frame** (what the fly/orbit is showing): `(scene/render-check!)` or `(scene/render-check! other-path)`.
3. **A named part** (sundial, stair, a furniture group): `(scene/render-check! path {:look-at \"site-sundial-face\"})` or `{:look-at [x y z] :distance 8}`. Perch stays on the current camera’s line to the target; a temp camera does the still; the fly/scene camera is restored. Do not `camera-add` or relocate `site-fly-camera`.
4. For Show vs Base, keep the same `:look-at` (or the same bare call); change only the design state; two paths; restore the prior suggestion/base after.
5. Inspect what is actually visible against the spatial request: identity/label, direction, adjacency, orientation, and placement. Object existence, coordinates, successful returns, and error-free evaluation prove execution—not visual intent. Check `:ok?` / `:error`.
6. **The PNG must appear in the visitor chat** (absolute `:path`, same bubble as the ask) — including both Show and Base when you compared them. Do not hand off with a path string or a prose summary of the render. Same rule for `ui-check!`. How: **`yardcraft-setup`** → [hello-conversation.md](../yardcraft-setup/references/hello-conversation.md) **Chat images**. The human still judges the Blender viewport.

```clojure
(require '[yardcraft.scene :as scene])
(scene/census)
(scene/object-info "site-sundial-face")
(scene/render-check! "/tmp/yardcraft-visual-check-sundial.png"
                     {:look-at "site-sundial-face"})
;; => {:ok? true :path \"…\" :look-at […]} — put that PNG in chat; fly keeps flying
```

### N-panel self-check

```
λ ui_check.
  register! ∨ demo-stage-ui! → scene/ui-check! → inspect_png → ask_human(panel)
  | tight → (scene/ui-check!) ; Hello look-over-here → {:region :ui :pad 160}
  | auto_trim empty below tabs ; :trim-pad n / :height n / :trim false
  | ¬whole_window ¬DIY_screenshot
  | png_appears_in_chat(:path) before(panel_handoff)  ; ¬path_string_only
```

Sidebar must already be open (`demo-stage-ui!` / `viewport/show-n-panel!`). Default crop follows the live tabs/controls (grows when you add UI). `:trim-pad` (default 24) keeps a sliver below; `:height` forces a top crop; `:trim false` is the full sidebar. Failures: `:no-window`, `:no-view3d`, `:no-ui-region`, `:bad-region`.

```clojure
(scene/ui-check!)
(scene/ui-check! "/tmp/yardcraft-ui-check.png" {:region :ui :pad 160})
;; => {:ok? true :path \"…\" :category \"Yardcraft\"} — put that PNG in chat
```

## bpy interop cheatsheet

Prefer **property access** (`(.-object (.-ops bpy))`) in this workspace — lint-clean under clj-kondo. Slash forms like `bpy.ops/object` and `bpy.context/object` work at runtime in Basilisp (Python attribute path) but clj-kondo flags them as unresolved namespaces; that is a tooling mismatch, not a Basilisp bug. `bpy.context` is a Context object, not an importable module (`(import bpy.context)` fails).

```clojure
(ns scratch
  (:import bpy math))

;; operators take keyword args after ** (Basilisp compiler syntax; excluded in .clj-kondo/config.edn)
(.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
(.primitive-cube-add (.-mesh (.-ops bpy)) **
                     :size 2
                     :location [0 0 1])

;; data & context
(.-object (.-context bpy))
(.-objects (.-data bpy))
(.-location (.-object (.-context bpy)))
(set! (.-use-nodes mat) true)
```

`bpy.ops` often needs correct mode/context; if an ops call fails, inspect mode and active object before retrying.

## Safety defaults for this workspace

- No mass `delete` / “clear all meshes” unless the human asks
- No overwriting `.blend` files unless asked
- Exploratory geometry: name objects clearly (`patio-…`, `parking-…`) so variants stay distinguishable

## Verified quirks

Same env as **basilisp** skill: Blender ≥ 5.2.0 LTS, Calva `basilisp-blender`, Python 3.13. File-def reset on `:reload` is the dialect reason session registries need a process-global home.

- **`bpy` module attrs survive Basilisp ns `:reload`:** `(setattr bpy "_yardcraft_…" v)` / `getattr` / `hasattr`. Yardcraft uses `_yardcraft_session_suggestions` and `_yardcraft_active_suggestion` in `site_suggestions.cljc`. Put reload-durable session registries here — not in file-level `def` atoms alone.
- **`scene.yardcraft` PropertyGroup Python identity changes** across `(ui/register!)` / unregister cycles — **re-fetch** after UI rebuild; do not stash the props object.
- **EnumProperty items bake at PropertyGroup build time.** After changing the suggestion registry that feeds enums, call `(ui/register!)`. Keyword ids → underscore enum ids (`:quirk-enum-probe` → `"quirk_enum_probe"`).
- **`sys.modules["yardcraft"]` nil tombstone:** if the key is present with value `None`, require can fail (`'NoneType' … '__path__'`). Fix: `(.pop (.-modules sys) "yardcraft" nil)` then require again (`src/` on path). Details in verified-quirks.

### Yardcraft UI notes (code-backed)

- Show ops: on failure `.report` ERROR + `#py #{"CANCELLED"}` (silent `FINISHED` hides errors) — see `yardcraft.site-ui` show suggestion op.
- Draw sync must not force Base when active id is nil (wipes staged enum selection) — `sync-suggestion-enum-from-active!`.
- Prefer promoting helpers to files + `:reload` over large REPL-only `in-ns` / `defn-` private helpers when the session gets flaky.

Depth + explicit non-claims (`:reload-all` RecursionError not invariant): [references/verified-quirks.md](references/verified-quirks.md).

## Progressive disclosure

| Reference | Load when |
|---|---|
| [references/nrepl-and-setup.md](references/nrepl-and-setup.md) | Install, panel, manual `server_start`, logging |
| [references/upgrade-basilisp.md](references/upgrade-basilisp.md) | Extension zip / Basilisp version — PEZ pre-upstream zip bundles ≥ 0.5.1 (#1302) |
| [references/bpy-patterns.md](references/bpy-patterns.md) | Materials, ops, `scene/census` + aimed `render-check!` + `ui-check!`, torus example notes |
| [references/api.md](references/api.md) | `nrepl-server-start`, `class-make*` |
| [references/verified-quirks.md](references/verified-quirks.md) | bpy session attrs, scene.yardcraft identity, EnumProperty bake, sys.modules nil tombstone |

## Upstream

- Repo: https://github.com/ikappaki/basilisp-blender
- API: https://github.com/ikappaki/basilisp-blender/blob/main/API.md
- Blender Python API: https://docs.blender.org/api/current/

Until [ikappaki/basilisp-blender#14](https://github.com/ikappaki/basilisp-blender/pull/14) lands, Yardcraft uses the [PEZ release](https://github.com/PEZ/basilisp-blender/releases/tag/v0.5.0-basilisp-0.5.1) that bundles Basilisp 0.5.1 — see [references/upgrade-basilisp.md](references/upgrade-basilisp.md).
