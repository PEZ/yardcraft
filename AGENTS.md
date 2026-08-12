# Yardcraft — Agent Orientation

Exploratory Blender workspace driven by **Basilisp** (Clojure-compatible Lisp on Python) via an nREPL server inside Blender ([basilisp-blender](https://github.com/ikappaki/basilisp-blender)).

## Setup (temporary)

> **Consumer clone only:** complete this section once, then **literally delete this entire Setup section** from your working copy. Upstream / template keep Setup forever.

1. **Introspect the harness** — detect which AI/editor stack is running (Cursor, VS Code, other). Note where agent skills are installed for that harness.
2. **Preferred path: Cursor + Calva + Backseat Driver**
   - This repo ships `.vscode/settings.json` with a Calva connect sequence named **`basilisp-blender`** and root `user.lpy` (`user/init!` adds `src` to `sys.path`).
   - Connect sequence: **`basilisp-blender`** (session key `basilisp-blender`) — not the generic basilisp picker alone.
   - After connect, `afterPrimaryReplConnectedCode` loads `user.lpy` and runs `(user/init!)`.
3. **Other harnesses** — if you are not on Cursor+Calva+Backseat Driver, **web-search** how that harness installs agent skills and connects an nREPL client to Blender; adapt the same goals (skills on disk, nREPL to basilisp-blender, `src` on `sys.path`).
4. **Install packaged skills** — **copy** (install) everything under this repo’s `recipe/skills/` into the harness skill location (for Cursor: typically `.cursor/skills/`). Keep `recipe/skills/` in the repo as the canonical package; do not empty it.
5. **Tooling floors**
   - **Blender ≥ 5.2.0 LTS**
   - **basilisp-blender** extension + Basilisp **≥ 0.5** overlay (see `recipe/skills/basilisp-blender/references/upgrade-basilisp.md`)
   - **Babashka** (`bb`) for host-side HTTP/fs
   - **Epupp** for live browser map UI exploration when ingesting national maps
6. **Shared dialect skills** — detect whether `clojure`, `babashka`, and `epupp` skills are already available in the harness; if missing, install from their upstream skill packages (Awesome Backseat Driver / Epupp docs) before Yardcraft site work.
7. **Verify empty site** — Project Directory = repo root; start nREPL; connect; `(require '[yardcraft.site :as site])` … `(site/ensure-site! site)` (or equivalent with referred `site`). Expect **site-root + sun/world/viewport defaults** only — no house/lot/terrain meshes until facts are filled.
8. **Delete this Setup section** from the consumer clone when the above is green.

## Project intent (evolving)

Explore yard / patio / parking options in Blender for **your** site.

- Human reading notes: [`site.md`](site.md); project entry: [`README.md`](README.md)
- Machine-readable: facts map `site` in [`src/yardcraft/site_data.cljc`](src/yardcraft/site_data.cljc) (`yardcraft.site-data`); domain builders in `yardcraft.site-*`; orchestration in [`src/yardcraft/site.cljc`](src/yardcraft/site.cljc) (`yardcraft.site`)

Do not invent site measurements. Extend `site` in `yardcraft.site-data` when something is confirmed. Flat `yardcraft.site-*` namespaces (not nested `yardcraft.site.data`) — Basilisp/Python packaging vs leaf `yardcraft.site`.

## Stack

| Piece | Role |
|---|---|
| Blender ≥ 5.2.0 LTS | Host app + `bpy` scene API |
| Basilisp | Lisp dialect (runtime in Blender); sources under `src/` use `.cljc` (Calva + CodeScene; structural edits still apply) |
| basilisp-blender | Eval helpers + nREPL served from Blender’s main loop; adds project root to `sys.path` (not `src/`) |
| `user.lpy` | `user/init!` adds `src/` to `sys.path` after Calva connect |
| `src/yardcraft/scratch.cljc` | Optional scratch / bootstrap notes |
| `basilisp.edn` | Marks the project root for Clojure-aware editors |
| `.nrepl-port` | Written by the nREPL server when it starts |
| Babashka (`bb`) | Host-side scripting REPL (HTTP, fs, unpack, measure assets) — prefer over bash/python |
| Epupp | Browser-side map UI exploration when discovering national map APIs |

Docs: [Basilisp](https://docs.basilisp.org/en/latest/) · [basilisp-blender](https://github.com/ikappaki/basilisp-blender)

## Skills to load

When working in this repo, load:

1. **`basilisp`** — dialect, Python interop, project layout
2. **`basilisp-blender`** — nREPL-in-Blender workflow, `bpy` patterns, safety
3. **`clojure`** — shared Clojure coding conventions (still apply; Basilisp is the host dialect)
4. **`babashka`** — when doing host-side scripting (downloads, fs, asset plumbing) outside Blender
5. **`epupp`** — when exploring map UIs in the browser
6. **`yardcraft-light-table`** — when tracing sketches/contours over a fitted overlay (light table)
7. **`yardcraft-design-suggestions`** — when switching/authoring design suggestions (EDN overlays, show!/show-base!, promote)
8. **`yardcraft-site-ui`** — when working on the View3D N-panel / Blender UI controls (`yardcraft.site-ui`)
9. **`yardcraft-fly-tour-create`** — when creating a new narrative camera fly tour from scratch
10. **`yardcraft-fly-tour-edit`** — when retiming, retargeting gaze, or tweaking an existing fly tour DSL
11. **`yardcraft-quote-plan`** — when writing contractor quote-plan SVG via `write-quote-plan!`
12. **`yardcraft-assets`** — when downloading optional GLBs from `assets/*/ATTRIBUTION.md`
13. **`sweden-lantmateriet-min-karta`** (under `recipe/skills/references/sweden-lantmateriet/`) — Swedish Min Karta / LM höjd; declare CRS SWEREF99 + RH00

## Agent operating model

```
λ yardcraft_agent.
  REPL_explore → visible_in_Blender → ask_human_feedback → promote_to_files_when_happy
  | partial_ensure-*! → sync-site-hierarchy!(site)  ; keep under site-root rotation
  | scene_state ≡ unknown_until_queried
  | destructive_ops → confirm_with_human
  | host_scripting → bb_REPL (¬bash/python one-offs)
  | .cljc_form_edits → structural_editing (same discipline as Clojure)
  | connect → basilisp-blender session ∧ user/init! first
```

### REPL → Blender check → promote

Default loop for scene/design work (also restated in the Yardcraft skills):

1. **Make it happen in the REPL** — eval small helpers so the change is visible in Blender. Prefer session Vars, `(comment …)` / RCF demos, and existing `ensure-*!` / `show!` paths over editing source first.
2. **Ask for feedback** — stop and ask the human to look in the Blender viewport (angle, fit, pacing, “does this read right?”). Do not assume the viewport looks good from REPL return values alone.
3. **Commit to files only when the human is happy** — then promote durable facts → `yardcraft.site-data`, builders → matching `yardcraft.site-*`, orchestration → `yardcraft.site`, suggestion EDN, fly-tour specs, etc. Until then, keep the experiment in the REPL / session.

Throwaway exploration stays in the REPL or [`src/yardcraft/scratch.cljc`](src/yardcraft/scratch.cljc); root [`scratch.lpy`](scratch.lpy) is basilisp-blender’s auto playground marker. Structural file edits still apply once promoting — just don’t promote early.

### Operating rules

1. **Assume an nREPL into Blender is available** when the human says so. Drive changes interactively so they are visible in Blender; promote only after the feedback step above. After connect, ensure `(user/init!)` ran so `src/` is on `sys.path` before requiring site namespaces.
2. **Host-side scripting uses the Babashka REPL.** Calva session key `bb` (jack-in / connect Babashka). Prefer `babashka.http-client`, `babashka.fs`, `babashka.process`, etc. over bash/`curl`/`python` one-liners for downloads, unpacking, and asset inspection. Blender/`bpy` work stays on the `basilisp-blender` session.
3. **Query before mutate.** List objects, collections, dimensions — then change.
4. **Keep experiments small.** Prefer named helper fns and `(comment …)` / `#_…` over one-shot scripts that nuke the scene.
5. **Re-parent after partial rebuilds.** Domain `ensure-*!` calls create meshes in house-NW coords. They only inherit `site-root`'s north-offset rotation after hierarchy adoption. After any partial rebuild (`ensure-terrace!`, `ensure-terrace-roof!`, etc.), call `(yardcraft.site/sync-site-hierarchy! site)` (then paint if needed). Skipping this leaves new objects world-aligned — looking “detached” until a full `(ensure-site! site)`. Prefer sync over full rebuild when iterating. Full `ensure-site!` already syncs via `finish-site-scene!`.
6. **Site objects** use the `site-` prefix. `(yardcraft.site/clear-site!)` removes **all** scene objects (not only `site-*`); default Cube/Light/Camera go away before rebuild.
7. **Do not invent site measurements.** Ask, or leave placeholders, until patio/parking facts are in [`site.md`](site.md) / [`src/yardcraft/site_data.cljc`](src/yardcraft/site_data.cljc).

## Site ingestion

Freestyle path from empty defaults to a lived-in model:

1. **National maps** — Sweden: [Min Karta](https://minkarta.lantmateriet.se/); Norway: [Norgeskart](https://norgeskart.no/). Use Epupp in the browser to discover APIs and sample geometry/heights.
2. **Country skills** — when a nation’s map stack stabilizes, author a skill under `recipe/skills/references/<country>-…/` that declares **CRS + vertical datum** up front (Sweden example: SWEREF99 TM + RH00 — `sweden-lantmateriet-min-karta`).
3. **Hand light table** — photograph or window-align sketches; fit via `:sketch/specs`; trace contours/edges (`yardcraft-light-table`). Example overlays may live under `recipe/example-source-images/`.
4. **Promote confirmed facts** into `site_data` / `site.md` only after viewport checks.

## Base → suggestions → fly

| Layer | What |
|---|---|
| **Base** | Survey / constructed facts in `site_data` + builders via `ensure-site!` |
| **Suggestions** | Design overlays (session → N-panel Show/Base → EDN under `src/yardcraft/suggestions/`) — see `yardcraft-design-suggestions` |
| **Fly / quote-plan** | Optional narrative camera (`yardcraft-fly-tour-*`) and contractor SVG (`yardcraft-quote-plan`) once base (or a shown suggestion) is worth presenting |

## Coding preferences

1. **Explicit `site` argument** — Do not add 0-arity wrappers that default to the global `site` var. Builders take the facts map as a required arg (`[s]`). Only orchestration (`yardcraft.site`) refers `site` from `yardcraft.site-data` and passes it in, e.g. `(ensure-site! site)`. Domain namespaces (`yardcraft.site-*`) receive `s` from callers; they do not require or refer the global.
2. **Destructuring** — Lean on Clojure/Basilisp destructuring (`:keys`, namespaced keys, nested maps/vectors). Prefer pulling needed fields via destructuring over repeated `(:key m)` / `get` digging when binding several values.
3. **Code Health** — CodeScene aspiration for `src/yardcraft/*.cljc` is **10.0**.
4. **Editor scripting** — Yardcraft stays on Calva + basilisp-blender + Babashka (+ Epupp for maps); do not require a separate VS Code extension-host Clojure runtime.

## Light table

Tracing elevation contours, driveway edges, or other lines over a fitted sketch overlay → load **`yardcraft-light-table`**.

## Camera fly tour

Narrative Follow Path + Track To tour in `yardcraft.site-fly` (`src/yardcraft/site_fly.cljc`). Data knobs: `tour-path-spec`, `tour-look-spec`, `tour-offset-keys`. Agents: **`yardcraft-fly-tour-create`** (greenfield) / **`yardcraft-fly-tour-edit`** (tweaks).

## Quote plan

Contractor dimensioned top-down SVG from site facts (not a Blender screenshot): `yardcraft.site-plan` / `write-quote-plan!`. Default output `out/quote-plan.svg`. Pass `(yardcraft.site-suggestions/effective-site site)` for the active `show!` suggestion, or `(effective-site site :suggestion-id)` to pin a suggestion. Skill: **`yardcraft-quote-plan`**. Opts: `{:show-angles? true}` (default false).

## Session bootstrap

After installing or updating the basilisp-blender extension, upgrade Basilisp to **≥ 0.5** into the extension `.local` site-packages (fixes [#1302](https://github.com/basilisp-lang/basilisp/issues/1302) Calva load-file / module aliases). Procedure: **`basilisp-blender`** skill → [references/upgrade-basilisp.md](recipe/skills/basilisp-blender/references/upgrade-basilisp.md).

1. Blender: Basilisp Project Directory = repo root; start nREPL (writes [`.nrepl-port`](.nrepl-port)).
2. Calva: connect sequence **`basilisp-blender`** (session key `basilisp-blender`) — not the generic basilisp picker alone.
3. Confirm `user/init!` (via connect sequence), then require `yardcraft.*`. Without `src/` on `sys.path`, `No module named 'yardcraft'`.
4. Host-side assets: Babashka session `bb`. Scene work: `basilisp-blender`.

Prefer `(.-ops bpy)` / `(.-context bpy)` over `bpy.ops/…` / `bpy.context/…` (clj-kondo). Details: **`basilisp-blender`** skill.

Once site is built: `(require '[yardcraft.site-ui :as ui])` then `(ui/register!)` — once per Blender session, adds the **Yardcraft** View3D N-panel. Re-register after a Blender restart. To reload UI code: `(require 'yardcraft.site-ui :reload)` then `(require '[yardcraft.site-ui :as ui])`, then `(ui/register!)` or `(ui/reload!)`.

## Key namespaces

| Ns / file | Role |
|---|---|
| `yardcraft.site` | Orchestration (`ensure-site!`, draw staging, sun setters) |
| `yardcraft.site-data` | Facts map `site` |
| `yardcraft.site-*` | Domain builders (house, lot, terrace, driveway, sketch, …) |
| `yardcraft.site-hierarchy` | `site-root` parenting + Outliner groups |
| `yardcraft.site-sketch` | Light-table / draft contours (see skill) |
| `yardcraft.site-suggestions` | Design suggestion overlays (`show!` / `show-base!` / `promote-plan`) |
| `yardcraft.site-ui` | View3D N-panel (`register!` / `unregister!`) — sun date/time, canopy, suggestion Show/Base |
| `yardcraft.site-plan` | Quote-plan SVG (`write-quote-plan!` → `out/quote-plan.svg`; use `effective-site`) |
| `yardcraft.site-fly` | Camera fly tour (`ensure-fly-tour!`; `:tour/*` + `:look-at/*` specs) |
| `src/yardcraft/suggestions/` | Durable suggestion EDN files (not survey truth) |
| `user.lpy` | `sys.path` bootstrap after connect |
