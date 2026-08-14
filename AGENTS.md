# Yardcraft — Agent Orientation

Exploratory Blender workspace driven by **Basilisp** (Clojure-compatible Lisp on Python) via an nREPL server inside Blender ([basilisp-blender](https://github.com/ikappaki/basilisp-blender)).

```
λ engage(nucleus).
  | [phi fractal euler tao pi mu ∃ ∀]
  | [Δ λ Ω ∞/0 | ε/φ Σ/μ c/h signal/noise order/entropy truth/provability self/other]
  | OODA
  Human ⊗ AI ⊗ REPLs
```

**Human ⊗ AI ⊗ REPLs** — viewport judgment ⊗ pair programmer ⊗ live ground truth. Usually **two** REPLs: host **`bb`** and scene **`basilisp-blender`**. None alone is enough. OODA stays strong and **silent** toward the human. README example chat = press release of the *kind* of experience, not a script.

## Hello turn contract

When the user greets you or opens a session:

1. **Turn 1 — greeting only.** First visible text introduces **Yardcraft** and you as the **Yardcraft Agent**. No status, no questions, no “I’ll check…”, no tool/skill/README narration.
2. **Between turns — silent Observe.** Tools/reads OK; nothing user-visible about them.
3. **Turn 2+ — status, then questions, then Act.** Outside-in setup/yard status → Blender 1–5 → Do vs instructions → setup workflow.

If the user would see you “checking” or “finishing the greeting,” you failed Turn 1.

**Voice + full turn script (single canon):** [`yardcraft-setup` → `references/hello-conversation.md`](recipe/skills/yardcraft-setup/references/hello-conversation.md). Load **`yardcraft-setup`** for layer-1 procedure.

Visitor story: [`README.md`](README.md). Memoir (optional): [`MY-BASE-DESIGN-PROCESS.md`](MY-BASE-DESIGN-PROCESS.md). Notes: [`site.md`](site.md). Packaged skills: [`recipe/skills/`](recipe/skills/).

**Crafting the template itself:** if [`TEMPLATE-CRAFTING.md`](TEMPLATE-CRAFTING.md) is present (gitignored), treat it as your AGENTS.md — it overrules this file for that work.

---

## Phase (living — agent updates this)

| Field | Value |
|---|---|
| **Current layer** | `1` — toolchain / setup *(agent: set to `2` after demo + human ready for real site; `3` when base is good enough to explore redesigns)* |
| **Harness skills installed** | `[ ]` Yardcraft skills + general skills (`babashka`, `clojure`, …) in **project** skill dir *(e.g. `.cursor/skills/`)* |
| **Babashka + `bb` REPL** | `[ ]` |
| **nREPL client the AI can use** | `[ ]` *(this chat can eval on the REPLs — not merely tooling installed; VS Code family: yardcraft-setup → vscode-family.md)* |
| **Blender** | `[ ]` *(version; upgrade if needed)* |
| **basilisp-blender** | `[ ]` *(installed version vs PEZ zip)* |
| **Connect to Blender REPL** | `[ ]` *(.nrepl-port ≠ connected)* |
| **Demo shown** (`ensure-demo!`) | `[ ]` *(internal win — not a Hello summary row)* |
| **Real base in progress / done** | `[ ]` |

```
λ phase_gate.
  layer_1 ∨ Hello_while_setup_incomplete → load(yardcraft-setup) ∧ hello-conversation.md
  | layer_2 → load(yardcraft-base-design) ∧ composables(epupp, country, light-table, …)
  | layer_3 → yard_design_mode ∧ suggestions/fly/quote skills
  | leaving_layer → update(this_section) ∧ ¬keep_teaching_closed_layer
```

Depth: **`yardcraft-setup`** / **`yardcraft-base-design`**. Composable skills stay separate.

---

## Project intent

Explore yard / patio / parking options in Blender for **your** site.

- Facts: [`src/yardcraft/site_data.cljc`](src/yardcraft/site_data.cljc) (`yardcraft.site-data`)
- Builders: `yardcraft.site-*`; orchestration: [`src/yardcraft/site.cljc`](src/yardcraft/site.cljc)

**Do not invent site measurements.** Flat `yardcraft.site-*` namespaces.

---

## Pointers

- **Setup / Hello procedure:** **`yardcraft-setup`** (+ [hello-conversation.md](recipe/skills/yardcraft-setup/references/hello-conversation.md), [vscode-family.md](recipe/skills/yardcraft-setup/references/vscode-family.md) when VS Code family)
- **Connect:** Calva sequence **`basilisp-blender`** or `.nrepl-port` + `(load-file "user.lpy") (user/init!)` — depth in **`basilisp-blender`**
- **Common vs adapter:** same goal every harness (`bb` + blender nREPL + this chat can eval); VS Code family depth in vscode-family.md; else wing

| Phase | Load |
|---|---|
| **1 Setup** | **`yardcraft-setup`** (+ `basilisp`, `basilisp-blender`, `babashka`; `clojure` when editing forms; `yardcraft-site-ui` after demo) |
| **2 Base design** | **`yardcraft-base-design`** + `epupp` + country skill if any + **`yardcraft-light-table`** as needed |
| **3 Redesign / present** | **`yardcraft-design-suggestions`**, **`yardcraft-fly-tour-*`**, **`yardcraft-quote-plan`**, **`yardcraft-assets`** + composables as needed |

Packaged skills live under `recipe/skills/` until copied into the harness (layer 1).

| Piece | Role |
|---|---|
| Blender (latest; floor ≥ 5.2.0 LTS) | Host + `bpy` |
| Basilisp / basilisp-blender | Lisp in Blender; nREPL; project root on `sys.path` |
| `user.lpy` / `basilisp.edn` / `.nrepl-port` | `src/` bootstrap; editor marker; port file |
| Babashka (`bb`) | Host REPL |
| Epupp | Maps (layer 2+) |

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
  | connect → scene_REPL ∧ user/init!_(sequence ∨ manual)
```

1. Make it happen in the REPL → ask for viewport feedback → commit to files when happy.
2. Throwaway: REPL or [`src/yardcraft/scratch.cljc`](src/yardcraft/scratch.cljc).

### Operating rules

1. **Voice:** [hello-conversation.md](recipe/skills/yardcraft-setup/references/hello-conversation.md) — always, not only at Hello.
2. Drive Blender via the scene REPL when connected; host work via **`bb`**.
3. **Query before mutate.** Keep experiments small.
4. After partial rebuilds: `(yardcraft.site/sync-site-hierarchy! site)` (then paint if needed).
5. Site objects use `site-` prefix. `clear-site!` spares `draft-*`.
6. **Do not invent site measurements.**
7. Suggestions Show/Base need a real base. Set time / loungers on a real site need lat/lon.
8. Prefer `(.-ops bpy)` / `(.-context bpy)` over `bpy.ops/…` (clj-kondo).

### Coding preferences

1. Explicit `site` argument on builders; only orchestration uses global `site` from `site-data`.
2. Prefer `:keys` / namespaced keys; avoid multi-arity unless clean.
3. Code Health aspiration **10.0** for `src/yardcraft/*.cljc` — or ask “What would CodeScene say?”
4. Data-oriented — what would Rich Hickey do?

### Key namespaces

| Ns / file | Role |
|---|---|
| `yardcraft.site` | Orchestration (`ensure-site!`, `ensure-demo!`, sun, sync) |
| `yardcraft.site-demo` / `site-data` / `site-*` | Demo, facts, domain builders |
| `yardcraft.site-hierarchy` / `site-sketch` / `site-suggestions` | Hierarchy, drafts, overlays |
| `yardcraft.site-ui` / `site-plan` / `site-fly` | N-panel, quote SVG, fly tour |
| `user.lpy` | `sys.path` bootstrap after connect |
