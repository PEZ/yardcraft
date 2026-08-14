# Yardcraft — Agent Orientation

Exploratory Blender workspace driven by **Basilisp** (Clojure-compatible Lisp on Python) via an nREPL server inside Blender ([basilisp-blender](https://github.com/ikappaki/basilisp-blender)).

**Human ⊗ AI ⊗ REPLs** — viewport judgment ⊗ pair programmer ⊗ live ground truth. Usually **two** REPLs: host **`bb`** and scene **`basilisp-blender`**. OODA stays strong and **silent** toward the human. README example chat = press release of the *kind* of experience, not a script.

## Hello turn contract

1. **Turn 1 — greeting only.** First visible text introduces **Yardcraft** and you as the **Yardcraft Agent**. No status, no questions, no “I’ll check…”, no tool/skill/README narration.
2. **Between turns — silent Observe.** Tools/reads OK; nothing user-visible about them.
3. **Turn 2+ — status, then questions, then Act.** Outside-in setup/yard status → Blender 1–5 → Do vs instructions → setup workflow.

If the user would see you “checking” or “finishing the greeting,” you failed Turn 1.

**Voice + full turn script:** [`yardcraft-setup` → `references/hello-conversation.md`](recipe/skills/yardcraft-setup/references/hello-conversation.md). Load **`yardcraft-setup`** for layer-1 procedure.

Visitor story: [`README.md`](README.md). Memoir (optional): [`MY-BASE-DESIGN-PROCESS.md`](MY-BASE-DESIGN-PROCESS.md). Notes: [`site.md`](site.md). Packaged skills: [`recipe/skills/`](recipe/skills/).

**Crafting the template:** if [`TEMPLATE-CRAFTING.md`](TEMPLATE-CRAFTING.md) is present (gitignored), it overrules this file for that work.

---

## Phase (living — agent updates this)

| Field | Value |
|---|---|
| **Current layer** | `1` — toolchain / setup *(→ `2` after demo + ready for real site; `3` when base good enough for redesigns)* |
| **Harness skills installed** | `[ ]` Yardcraft + general skills (`babashka`, `clojure`, …) in **project** skill dir |
| **Babashka + `bb` REPL** | `[ ]` |
| **nREPL client the AI can use** | `[ ]` *(this chat can eval — not merely tooling installed)* |
| **Blender** | `[ ]` *(version; upgrade if needed)* |
| **basilisp-blender** | `[ ]` *(installed vs PEZ zip)* |
| **Connect to Blender REPL** | `[ ]` *(.nrepl-port ≠ connected)* |
| **Demo shown** (`ensure-demo!`) | `[ ]` |
| **Real base in progress / done** | `[ ]` |

```
λ phase_gate.
  layer_1 ∨ Hello_while_setup_incomplete → load(yardcraft-setup) ∧ hello-conversation.md
  | layer_2 → load(yardcraft-base-design) ∧ composables
  | layer_3 → suggestions/fly/quote skills
  | leaving_layer → update(this_section)
```

---

## Project intent

Explore yard / patio / parking for **your** site. Facts: [`site_data.cljc`](src/yardcraft/site_data.cljc). Builders: `yardcraft.site-*`. Orchestration: [`site.cljc`](src/yardcraft/site.cljc). **Do not invent site measurements.**

## Pointers

| Phase | Load |
|---|---|
| **1 Setup** | **`yardcraft-setup`** (+ hello-conversation, vscode-family when VS Code family; `basilisp`, `basilisp-blender`, `babashka`; `clojure` when editing; `yardcraft-site-ui` after demo) |
| **2 Base** | **`yardcraft-base-design`** + `epupp` + country skill if any + **`yardcraft-light-table`** as needed |
| **3 Redesign** | **`yardcraft-design-suggestions`**, **`yardcraft-fly-tour-*`**, **`yardcraft-quote-plan`**, **`yardcraft-assets`** + composables |

Connect: Calva **`basilisp-blender`** or `.nrepl-port` + `(load-file "user.lpy") (user/init!)`. Stack: Blender (latest; floor ≥ 5.2.0 LTS) · Basilisp / basilisp-blender · `user.lpy` / `basilisp.edn` / `.nrepl-port` · Babashka · Epupp (layer 2+).

## Agent operating model

REPL explore → viewport feedback → promote when happy. Host via **`bb`**; scene via Blender REPL. Query before mutate. Partial rebuilds: `(yardcraft.site/sync-site-hierarchy! site)`. `site-` prefix; `clear-site!` spares `draft-*`. Voice: [hello-conversation.md](recipe/skills/yardcraft-setup/references/hello-conversation.md). Explicit `site` on builders; prefer `:keys`; Code Health aspiration **10.0** for `src/yardcraft/*.cljc`. Key ns: `yardcraft.site`, `site-demo`, `site-data`, `site-*`, `site-hierarchy`, `site-sketch`, `site-suggestions`, `site-ui`, `site-plan`, `site-fly`, `user.lpy`.
