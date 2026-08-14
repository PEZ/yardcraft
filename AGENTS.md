# Yardcraft — Agent Orientation

Exploratory Blender workspace driven by **Basilisp** (Clojure-compatible Lisp on Python) via an nREPL server inside Blender ([basilisp-blender](https://github.com/ikappaki/basilisp-blender)).

```
λ engage(nucleus).
  | [phi fractal euler tao pi mu ∃ ∀]
  | [Δ λ Ω ∞/0 | ε/φ Σ/μ c/h signal/noise order/entropy truth/provability self/other]
  | OODA
  Human ⊗ AI ⊗ REPLs
```

**Human ⊗ AI ⊗ REPLs** — viewport judgment ⊗ pair programmer ⊗ live ground truth. Yardcraft usually needs **two** REPLs: host **`bb`** (Babashka) and scene **`basilisp-blender`**. None alone is enough.

**OODA** — Observe system · PATH · installs · REPL sessions · this file’s Phase · the human → Orient (layer + skills) → Decide → Act → **update Phase/progress below**. The README example chat is a **press release of the kind of experience**, not a script to replay.

**One shape ever** — present only the current recipe shape in docs/skills you edit. Past lives stay in git.

Visitor story: [`README.md`](README.md). Base-design memoir (optional): [`MY-BASE-DESIGN-PROCESS.md`](MY-BASE-DESIGN-PROCESS.md). Human notes: [`site.md`](site.md). Canonical skills: [`recipe/skills/`](recipe/skills/).

---

## Phase (living — agent updates this)

| Field | Value |
|---|---|
| **Current layer** | `1` — toolchain / setup *(agent: set to `2` after demo + human ready for real site; `3` when base is good enough to explore redesigns)* |
| **Harness skills installed** | `[ ]` copy `recipe/skills/` → harness (Cursor: `.cursor/skills/`) |
| **Babashka + `bb` REPL** | `[ ]` |
| **Calva + Backseat Driver** | `[ ]` |
| **Blender + basilisp-blender nREPL** | `[ ]` |
| **Connected `basilisp-blender`** | `[ ]` |
| **Demo shown** (`ensure-demo!`) | `[ ]` |
| **Real base in progress / done** | `[ ]` |

```
λ phase_gate.
  layer_1 ∨ Hello_while_setup_incomplete → load(yardcraft-setup)
  | layer_2 → load(yardcraft-base-design) ∧ composables(epupp, country, light-table, …)
  | layer_3 → yard_design_mode ∧ suggestions/fly/quote skills
  | leaving_layer → update(this_section) ∧ ¬keep_teaching_closed_layer
```

Depth for layer 1 and 2 lives in **`yardcraft-setup`** and **`yardcraft-base-design`**. Composable skills stay separate (do not fold light-table into base-design).

---

## Project intent

Explore yard / patio / parking options in Blender for **your** site.

- Facts map `site` in [`src/yardcraft/site_data.cljc`](src/yardcraft/site_data.cljc) (`yardcraft.site-data`)
- Domain builders in `yardcraft.site-*`; orchestration in [`src/yardcraft/site.cljc`](src/yardcraft/site.cljc)

**Do not invent site measurements.** Extend `site` when something is confirmed. Flat `yardcraft.site-*` namespaces (not nested `yardcraft.site.data`).

---

## Common vs situational ingredients

**Common (close these gaps when Observe says missing):** packaged skills in harness, Babashka + `bb` REPL, Calva + Backseat (Cursor path), Blender, basilisp-blender, connect sequence **`basilisp-blender`**, then `(yardcraft.site/ensure-demo!)` for the early win.

**Situational:** editor/harness, do vs instructions-only, OS/`PATH`, Blender already installed, country/map stack, sketches vs APIs, **`clojure` on PATH → remove** `.vscode` `"calva.enableClojureLspOnStart": "never"` (do **not** install Java/Clojure as Yardcraft setup).

**Blender wording:** with humans say **latest** ([blender.org/download](https://www.blender.org/download/)). Agent-private floor for Observe/compat: **≥ 5.2.0 LTS** at time of writing — don’t lecture versions unless checking or troubleshooting.

**basilisp-blender:** [PEZ zip v0.5.0-basilisp-0.5.1](https://github.com/PEZ/basilisp-blender/releases/tag/v0.5.0-basilisp-0.5.1) (bundles Basilisp ≥ 0.5.1; temporary until [upstream PR #14](https://github.com/ikappaki/basilisp-blender/pull/14)). **Quit Blender** before `blender --command extension install-file <zip> -r user_default -e`. Details: **`basilisp-blender`** skill → [upgrade-basilisp.md](recipe/skills/basilisp-blender/references/upgrade-basilisp.md).

**nREPL (humans):** Output Properties (printer icon) → **Basilisp nREPL server** → project path = this repo → **START SERVER**. Screenshot: [`recipe/readme/images/basilisp-blender-nrepl-panel.png`](recipe/readme/images/basilisp-blender-nrepl-panel.png).

**Connect:** Calva → *Connect to a running REPL server in the project* → **`basilisp-blender`** (not generic `basilisp` alone). Connect sequence runs `(user/init!)` / `user.lpy`. Re-run `user/init!` only after Blender restart or a blown `sys.path`.

**Early win:** `(require '[yardcraft.site :as site])` then `(site/ensure-demo!)` — letters, furniture, sundial, orbit fly, Yardcraft panel. Ask what they see. Empty `(ensure-site! …)` is for later real-base / insufficient-facts work, not the Hello delight check.

**Demo → base:** when ready for a real lot, leave demo; gather facts; `(ensure-site! site)` / empty path as appropriate. `clear-site!` removes scene objects but **keeps `draft-*`** light-table drafts. Load **`yardcraft-base-design`**.

---

## Stack

| Piece | Role |
|---|---|
| Blender (latest; floor ≥ 5.2.0 LTS) | Host app + `bpy` |
| Basilisp | Lisp in Blender; sources under `src/` as `.cljc` |
| basilisp-blender | nREPL from Blender’s main loop; project root on `sys.path` (not `src/`) |
| `user.lpy` | `user/init!` adds `src/` after Calva connect |
| `basilisp.edn` | Editor project marker |
| `.nrepl-port` | Written when nREPL starts |
| Babashka (`bb`) | Host HTTP/fs/process REPL |
| Epupp | Browser map UI exploration (layer 2+) |

Docs: [Basilisp](https://docs.basilisp.org/en/latest/) · [basilisp-blender](https://github.com/ikappaki/basilisp-blender)

---

## Skills to load (by phase)

| Phase | Load |
|---|---|
| **1 Setup** | **`yardcraft-setup`** (+ `basilisp`, `basilisp-blender`, `babashka`; `clojure` when editing forms; `yardcraft-site-ui` after demo) |
| **2 Base design** | **`yardcraft-base-design`** + `epupp` (install upstream if missing) + country skill if any + **`yardcraft-light-table`** as needed |
| **3 Redesign / present** | **`yardcraft-design-suggestions`**, **`yardcraft-fly-tour-*`**, **`yardcraft-quote-plan`**, **`yardcraft-assets`** — plus composables (e.g. light-table) when the work needs them |

Always available when relevant: `basilisp`, `basilisp-blender`, `clojure`, `babashka`, `sweden-lantmateriet-min-karta` (under `recipe/skills/references/sweden-lantmateriet/`).

Packaged skills live under `recipe/skills/` until copied into the harness (layer 1).

---

## Agent operating model

```
λ yardcraft_agent.
  OODA → Human ⊗ AI ⊗ REPLs
  | REPL_explore → visible_in_Blender → ask_human_feedback → promote_when_happy
  | partial_ensure-*! → sync-site-hierarchy!(site)
  | scene_state ≡ unknown_until_queried
  | destructive_ops → confirm_with_human
  | host_scripting → bb_REPL (¬bash/python one-offs)
  | .cljc_form_edits → structural_editing
  | connect → basilisp-blender ∧ user/init!_via_sequence
```

### REPL → Blender check → promote

1. **Make it happen in the REPL** — small helpers, session Vars, `(comment …)`, existing `ensure-*!` / `show!` paths.
2. **Ask for feedback** — human looks in the viewport; don’t assume return values look good.
3. **Commit to files when happy** — facts → `site-data`, builders → `site-*`, orchestration → `site`, suggestion EDN, fly specs, etc.

Throwaway work: REPL or [`src/yardcraft/scratch.cljc`](src/yardcraft/scratch.cljc). Root [`scratch.lpy`](scratch.lpy) is basilisp-blender’s playground marker.

### Operating rules

1. Drive Blender via **`basilisp-blender`** when connected; host work via **`bb`**.
2. **Query before mutate.**
3. **Keep experiments small.**
4. After partial rebuilds: `(yardcraft.site/sync-site-hierarchy! site)` (then paint if needed).
5. Site objects use `site-` prefix. `clear-site!` clears the scene but **spares `draft-*`**.
6. **Do not invent site measurements.**
7. **Suggestions Show/Base** need a real base — not the empty demo / empty template.
8. **Set time / loungers** on a real site need lat/lon; demo ships geo for that delight.
9. Prefer `(.-ops bpy)` / `(.-context bpy)` over `bpy.ops/…` (clj-kondo).

---

## Site ingestion (layer 2)

1. **National maps** — Sweden: [Min Karta](https://minkarta.lantmateriet.se/) + **`sweden-lantmateriet-min-karta`**. Norway: [Norgeskart](https://norgeskart.no/) — no packaged country skill yet; discover via Epupp and/or author `recipe/skills/references/<country>-…/` (declare CRS + vertical datum).
2. **Hand light table** — sketches / photos; **`yardcraft-light-table`**. Examples under `recipe/example-source-images/`.
3. **Promote** confirmed facts only after viewport checks. Memoir: [`MY-BASE-DESIGN-PROCESS.md`](MY-BASE-DESIGN-PROCESS.md) (optional, not a script).

---

## Base → suggestions → fly / quote (layer 3)

| Layer | What |
|---|---|
| **Base** | Survey facts + `ensure-site!` |
| **Suggestions** | Overlays via `show!` / `show-base!` / EDN under `src/yardcraft/suggestions/` |
| **Fly / quote-plan** | Narrative fly (`yardcraft-fly-tour-*`; panel Fly cam no-ops cleanly until a tour is authored) and contractor SVG (`yardcraft-quote-plan`). Quote from `yardcraft.site` REPL: `(plan/write-quote-plan! (sug/effective-site site))` — needs filled facts, not empty template |

UI: `(require '[yardcraft.site-ui :as ui])` `(ui/register!)` — once per Blender session (demo already registers). Panel: sun date, time slider, **Set time**, suggestion Show/Base, **Fly cam**.

---

## Coding preferences

1. **Explicit `site` argument** — builders take facts `[s]`; only orchestration refers global `site` from `site-data`.
2. **Destructuring** — prefer `:keys` / namespaced keys over repeated digging.
3. **Code Health** — CodeScene aspiration for `src/yardcraft/*.cljc` is **10.0**.
4. **Editor scripting** — Calva + basilisp-blender + Babashka (+ Epupp for maps); no separate extension-host Clojure runtime required for Yardcraft.

## Key namespaces

| Ns / file | Role |
|---|---|
| `yardcraft.site` | Orchestration (`ensure-site!`, `ensure-demo!`, sun, sync) |
| `yardcraft.site-demo` | Welcome demo scene |
| `yardcraft.site-data` | Facts map `site` |
| `yardcraft.site-*` | Domain builders |
| `yardcraft.site-hierarchy` | `site-root` parenting |
| `yardcraft.site-sketch` | Light-table / drafts |
| `yardcraft.site-suggestions` | Design overlays |
| `yardcraft.site-ui` | View3D N-panel — Set time, Show/Base, Fly cam |
| `yardcraft.site-plan` | Quote-plan SVG |
| `yardcraft.site-fly` | Narrative fly tour |
| `user.lpy` | `sys.path` bootstrap after connect |
