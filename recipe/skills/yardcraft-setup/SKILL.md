---
name: yardcraft-setup
description: >-
  Layer-1 Yardcraft toolchain: give human and agent a Babashka REPL and a
  Blender basilisp-blender nREPL, then staged demo (demo-stage-*!). Hello voice/turns live in
  references/hello-conversation.md; VS Code family depth in
  references/vscode-family.md. Use when setup incomplete, Hello in layer 1,
  or installing/connecting bb, Blender, basilisp-blender, or the demo.
---

# Yardcraft setup (layer 1 — ingredients / toolchain)

Orchestrates getting from a fresh clone to a live **Human ⊗ AI ⊗ REPLs** loop (`bb` + `basilisp-blender`) and a visible welcome demo. Depth for dialect, `bpy`, and UI lives in composable skills — this skill **orients and sequences**, it does not swallow them.

The README example chat is a **press release of the kind of experience**, not a script to replay. Run **OODA** strong and **silent**; human voice + Hello turns: [references/hello-conversation.md](references/hello-conversation.md).

## When to use

- Human says **Hello** (or equivalent) and Observe says layer 1 / setup incomplete
- Missing project skills, Babashka / `bb` REPL, Blender, basilisp-blender, an nREPL client the AI can use, or demo
- Resuming mid-setup: read progress in `AGENTS.md`, close the next gap only

## Hello

**Execute** [references/hello-conversation.md](references/hello-conversation.md) (voice + Hello job boundary). This skill is the layer-1 procedure only — do not restate voice law here.

## Prerequisites (load as beats unlock)

| Skill | When |
|---|---|
| **`basilisp`** | Before dialect / interop questions |
| **`basilisp-blender`** | Before Blender nREPL / `bpy` / PEZ zip install details |
| **`babashka`** | Host-side install, downloads, `bb` REPL work (install upstream if missing) |
| **`clojure`** | When editing `.cljc` / Clojure forms (install upstream if missing) |
| **`yardcraft-site-ui`** | After demo (demo registers the panel); when extending N-panel |
| **`yardcraft-design-suggestions`** | Suggestions system (register / Show / Base); layer-1 demo smoke uses it (§8b) |

## Layer-1 OODA

```
λ yardcraft_setup.
  Observe(harness ∧ PATH ∧ installs ∧ REPL_sessions ∧ AGENTS_progress ∧ human)
  → Orient(layer_1 ∧ common_vs_situational)
  → Decide(next_gap)
  → Act(do_mode ∨ instructions_only)
  → update(AGENTS.md phase ∧ progress)
  | Hello ∧ layer_1 → this_skill
  | success ≡ staged_demo visible ∧ human_feedback
  | ¬replay(README_chat) | ¬empty_ensure-site!_as_win
  | Human ⊗ AI ⊗ REPLs ≡ bb ∧ basilisp-blender
  | composable_skills ≡ load_when_needed (¬swallow)
```

**Common (goal):** packaged skills in harness · Babashka (`bb` REPL) for human + agent · Blender + basilisp-blender nREPL up · an nREPL client the AI can use through its harness · staged welcome demo (`demo-stage-*!`).

**Harness adapter (situational):** same common goal everywhere. **VS Code family** (Cursor, VS Code + Copilot, forks) → load [references/vscode-family.md](references/vscode-family.md) when that path unlocks (Calva + Calva Backseat Driver). **Anything else** → Observe / web-search / wing; do not enumerate editor stacks. Setup is **not** “the Cursor recipe with a footnote.”

**Also situational:** do vs instructions-only, OS/`PATH`, Blender already present, project skill-dir location, `clojure` CLI → LSP unblock (VS Code family only — in the reference).

## Progress

As each beat completes, **update `AGENTS.md` Phase / progress checkboxes** so the next Observe (new chat, reload) sees what is done. Leaving layer 1 means the playbook should stop sounding like full Hello setup.

## Workflow (observe gaps — skip what is already green)

### 1. Install skills **in the project**

Prefer the **project** harness skill location (Observe where this harness loads project skills — often a repo-local skills dir) so the clone carries what the agent needs.

1. **Yardcraft skills:** copy everything under `recipe/skills/` into that project location. Keep `recipe/skills/` as the canonical package — do not empty it.
2. **General skills** (separate beat / separate user-facing line): ensure **`babashka`** (and **`clojure`** when form-editing needs it) are installed **in the project** skill location too (copy/link from upstream harness packages if needed).
3. Observe settings if the harness uses a different project skill root; **ask the human** only if still unsure — with visitor-plain wording.
4. Mark progress in `AGENTS.md` (internal).

### 2. nREPL client the AI can use

**Goal:** this chat can evaluate on `bb` and on Blender’s nREPL.

Three states (do not collapse them):

1. **Client / extensions installed** — tooling on disk  
2. **Editor jacked in / connected** — human sees a live REPL session (e.g. green **bb** / **basilisp-blender**)  
3. **This chat can eval** — the tools that drive those sessions are available here  

Phase row **nREPL client the AI can use** means **(3)**, not (1) or (2).

```
λ agent_can_eval_gate.
  Observe(this_chat_can_eval_on_bb_and_later_blender)
  | installed ≠ connected ≠ this_chat_can_eval
  | ¬this_chat_can_eval → STOP ∧ guide(human_reload_window)
  | ¬invent_alternate_workarounds
  | gate_pass → then jack-in framing / Connect_to_Blender_REPL / demo_stages
```

**VS Code family:** load [references/vscode-family.md](references/vscode-family.md) for install, tool Observe, and connect details. **When this chat cannot drive the REPLs yet** (known first-open glitch): in **plain chat** (not the question UI), ask for Command Palette → **Developer: Reload Window**, then “Tell me when that’s done.” Re-Observe. More steps only if reload fails — in that reference. Keep human wording free of internals (no “MCP”, no “bridge”, no extension short-names unless escalation needs them).

**Question UI:** only for real choice questions (see [hello-conversation.md](references/hello-conversation.md)). Action gates (Reload Window, quit Blender, jack-in, nREPL panel, …) stay in plain chat.

**Other harnesses:** Observe what nREPL client and how this chat can eval (or the human prefers). Web-search as needed. Rendezvous on **`.nrepl-port`**; after Blender connect, `(load-file "user.lpy") (user/init!)` if no auto sequence. Confirm **this chat** can eval before marking the Phase row. Do not invent a VS Code-shaped stack.

### 3. Babashka (common — often before Blender)

**Goal:** live **`bb`** REPL for human and agent.

1. Observe: `bb` on `PATH`? Host REPL already connected?  
2. Install Babashka if missing (**Do** / instructions-only as chosen).  
3. Connect human + agent to `bb` however this harness does (VS Code family: jack-in steps in [vscode-family.md](references/vscode-family.md)).  
4. Host automation stays on `bb`; Blender/`bpy` stays on `basilisp-blender` later.  
5. Mark Babashka progress when `bb` is real for the human; mark **nREPL client the AI can use** only when the §2 “this chat can eval” gate passes.

### 4. Blender

- **Human-facing:** install / upgrade **latest** from [blender.org/download](https://www.blender.org/download/).
- **Agent-private floor:** ≥ **5.2.0 LTS** for Observe/compat — speak version numbers to humans as `✓ (version)` or “have X; will upgrade,” not as a lecture.

### 5. basilisp-blender (PEZ zip)

**Observe before Act — two gates:**

1. **Installed?** Probe whether basilisp-blender is already present and which version (CLI `extension list`, extensions dir under Blender’s user config, or Preferences → Get Extensions). Compare to the Yardcraft-recommended PEZ zip ([upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md)). If already the right package → mark ✓ and **skip** download/install. Do **not** reinstall “just in case.”
2. **Blender running?** Only if install/upgrade is actually needed: Observe whether a Blender process is running (OS process list / human). Ask them to **quit Blender** only when it **is** running. Do **not** ask to quit when Blender is not running.

```
λ basilisp_blender_install.
  Observe(installed? ∧ version) → already_ok? → skip
  | need_install_or_upgrade → Observe(Blender_running?)
  | running? → ask_quit_in_plain_chat ∧ wait
  | ¬running? → ¬ask_quit
  | then download(PEZ_zip) → CLI_install_file → enable
  | blender --command extension install-file <zip> -r user_default -e
  | macOS_fallback → /Applications/Blender.app/Contents/MacOS/Blender …
  | CLI_fails → Install_From_Disk (human)
```

Release + asset URL, finding `blender`, probes, fallback, verify: **`basilisp-blender`** skill → [upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md).

### 6. nREPL (human path)

Human reopens Blender, then:

1. **Output Properties** (printer icon)
2. **Basilisp nREPL server** panel
3. Project path = **repo root**
4. **START SERVER**

Writes/updates **`.nrepl-port`**.

**Show the panel screenshot in that ask** — same plain-chat message, absolute path. See [hello-conversation.md](references/hello-conversation.md) **Chat images**. Do not ask for START SERVER without the picture.

### 7. Connect to Blender REPL

**User-facing name:** always “Connect to Blender REPL.” A `.nrepl-port` file only means a server *may* have been started — it does **not** mean connected.

Connect with this harness’s nREPL client; confirm **this chat** can eval. After connect, `src/` must be on `sys.path` before `yardcraft.*` — Calva sequence runs `user/init!`; other clients: `(load-file "user.lpy") (user/init!)`. VS Code family connect clicks: [vscode-family.md](references/vscode-family.md). The §2 gate must already be green before treating connect/demo as agent-driven success.

### 8. Early win — demo (not empty site)

**Dramaturgy needs separate REPL evals.** Blender only paints when an eval returns. One `(ensure-demo!)` (or any single form that runs all stages) freezes, then pops in complete — that is **not** the win. Eval each **`demo-stage-*!`** as its **own** form; tell the human to **watch the viewport**.

```clojure
(require '[yardcraft.site-demo :as demo])
;; each line = its own eval — do not wrap in do/let or batch:
(demo/demo-stage-sun!)        ; 1/10 world + sun (June 21 06:00) + rendered
(demo/demo-stage-terrain!)    ; 2/10 lawn
(demo/demo-stage-yard!)       ; 3/10 YARD
(demo/demo-stage-brick!)      ; 4/10 brick deck
(demo/demo-stage-craft!)      ; 5/10 CRAFT
(demo/demo-stage-stairs!)     ; 6/10 stairs + railings
(demo/demo-stage-furniture!)  ; 7/10 furniture
(demo/demo-stage-sundial!)    ; 8/10 pedestal + sundial
(demo/demo-stage-ui!)         ; 9/10 Yardcraft panel
(demo/demo-stage-fly!)        ; 10/10 orbit fly
```

- Do **not** call `(site/ensure-demo!)` / `(demo/ensure-demo!)` for Hello — that convenience path is atomic (no dramaturgy).
- Do **not** reimplement stages in one mega-form.

Success for layer 1 is the **welcome demo** after that staged reveal (sun, letters, furniture, sundial, Yardcraft N-panel **open**, orbit fly **playing**). Empty `(ensure-site! …)` is for later real-base — not the Hello win.

**Ask the human what they see** in the Blender viewport (and panel). Point out Set time / lounger delight and Fly cam as demo-safe.

### 8b. Demo suggestions smoke (README beat — before handoff)

Load **`yardcraft-design-suggestions`** for the general register / Show / Base / self-verify loop. On the **welcome demo** scene (not empty `site`):

1. Ask openly for a redesign (README soft examples OK: *move something, or add a stair…*) — wait for **their** answer.
2. Invent geometry for that ask **in the REPL**. Session-register with **`:suggestion/domains #{:demo}`** (rebuilds welcome overlays — not `:terrace`/`:furniture`, which rebuild the real site).
3. `(ui/register!)` → agent **`show!` + `(scene/render-check! path {:look-at \"site-…\"})`**, then **`show-base!`** with the same `:look-at` (fix if wrong) → only then ask them to select → **Show** / **Base**. Depth: **`basilisp-blender`** Safe visual self-check.
4. Persist EDN under `src/yardcraft/suggestions/` when they want it kept.

Mark progress: demo shown. Then continue.

### 9. Hand off (one step beyond)

After demo (+ suggestions smoke), take the README **one step beyond**: tooling setup is done; invite them toward their **actual yard** (maps/sketches/APIs, optional memoir pointer, jump in when ready). Mark demo/progress in `AGENTS.md`. That closes Hello (see hello-conversation **Hello job open until**).

When **they** engage real-site work, leave layer 1, load **`yardcraft-base-design`**, and update phase.

## Do vs instructions-only

| Mode | Agent acts |
|---|---|
| **Do** (default when human chose Do) | Install tooling, Babashka, download PEZ zip, CLI `extension install-file`, drive REPL/demo; human still does Blender UI clicks (nREPL panel) and editor jack-in/connect when those need the human |
| **Instructions-only** | Spell clicks and commands; wait for confirmation; still Observe before prescribing |

## Progressive disclosure

| Reference | Load when |
|---|---|
| [references/hello-conversation.md](references/hello-conversation.md) | Hello / any human-facing voice — **mandatory** on layer-1 Hello |
| [references/vscode-family.md](references/vscode-family.md) | Harness is VS Code family — Calva, first-open wake-up, jack-in, connect, LSP |

## Invariants

- One shape ever — present the current install/connect path only
- **Hello / voice:** [hello-conversation.md](references/hello-conversation.md) only — do not restate or re-template the greeting in this skill
- **Common goal, situational adapter** — not Cursor-only; VS Code depth in vscode-family.md
- **This chat can eval** = drive the REPLs from here; install or status-bar green ≠ that gate
- Query before install when Observe already shows green
- **basilisp-blender:** Observe installed/version before download; ask quit Blender only if it is running (§5)
- Destructive Blender ops → confirm with human
- **Demo dramaturgy:** ten separate `demo-stage-*!` REPL evals (§8); not `(ensure-demo!)`
- **nREPL panel shot:** START SERVER ask includes the panel screenshot (hello-conversation **Chat images**)
- Structural edits for `.cljc` forms once editing starts (`clojure` skill)
