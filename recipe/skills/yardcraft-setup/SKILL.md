---
name: yardcraft-setup
description: >-
  Layer-1 Yardcraft toolchain onboarding: harness detect, install packaged
  skills, Babashka + Calva/Backseat, Blender + basilisp-blender nREPL, connect
  basilisp-blender, show ensure-demo!. Use when Observe says setup incomplete,
  the human says Hello and the phase is layer 1, or when installing/connecting
  Babashka, Blender, basilisp-blender, Calva, or the welcome demo.
---

# Yardcraft setup (layer 1 — ingredients / toolchain)

Orchestrates getting from a fresh clone to a live **Human ⊗ AI ⊗ REPLs** loop (`bb` + `basilisp-blender`) and a visible welcome demo. Depth for dialect, `bpy`, and UI lives in composable skills — this skill **orients and sequences**, it does not swallow them.

The README example chat is a **press release of the kind of experience**, not a script to replay. Run **OODA**: Observe the machine, harness, PATH, REPLs, and progress → Orient to layer 1 → Decide the next gap → Act → mark progress.

## When to use

- Human says **Hello** (or equivalent) and Observe says layer 1 / setup incomplete
- Missing harness skills, Babashka, Calva/Backseat, Blender, basilisp-blender, nREPL, or demo
- Resuming mid-setup: read progress in `AGENTS.md`, close the next gap only

## Prerequisites (load as beats unlock)

| Skill | When |
|---|---|
| **`basilisp`** | Before dialect / interop questions |
| **`basilisp-blender`** | Before Blender nREPL / `bpy` / PEZ zip install details |
| **`babashka`** | Host-side install, downloads, `bb` REPL work (install upstream if missing) |
| **`clojure`** | When editing `.cljc` / Clojure forms (install upstream if missing) |
| **`yardcraft-site-ui`** | After demo (demo registers the panel); when extending N-panel |

Do **not** pull Joyride into setup narrative or install path.

## Layer-1 OODA

```
λ yardcraft_setup.
  Observe(harness ∧ PATH ∧ installs ∧ REPL_sessions ∧ AGENTS_progress ∧ human)
  → Orient(layer_1 ∧ common_vs_situational)
  → Decide(next_gap)
  → Act(do_mode ∨ instructions_only)
  → update(AGENTS.md phase ∧ progress)
  | Hello ∧ layer_1 → this_skill
  | success ≡ ensure-demo! visible ∧ human_feedback
  | ¬replay(README_chat) | ¬empty_ensure-site!_as_win
  | Human ⊗ AI ⊗ REPLs ≡ bb ∧ basilisp-blender
  | composable_skills ≡ load_when_needed (¬swallow)
```

**Common ingredients:** packaged skills in harness, Babashka (`bb` REPL), Calva + Backseat Driver (Cursor path), Blender, basilisp-blender nREPL, connect sequence **`basilisp-blender`**, `(site/ensure-demo!)`.

**Situational:** do vs instructions-only, OS/`PATH`, Blender already present, `clojure` CLI present → LSP unblock, other harnesses (web-search skill install + nREPL client).

## Progress

As each beat completes, **update `AGENTS.md` Phase / progress checkboxes** so the next Observe (new chat, reload) sees what is done. Leaving layer 1 means the playbook should stop sounding like full Hello setup.

## Workflow (observe gaps — skip what is already green)

### 1. Harness → install packaged skills

1. Detect the AI/editor stack (Cursor, VS Code, other) and its skill install location.
2. **Copy** everything under repo `recipe/skills/` into that location (Cursor: typically `.cursor/skills/`).
3. Keep `recipe/skills/` as the **canonical package** — do not empty or move it out of the recipe.
4. Ensure upstream **`babashka`** skill is in the harness (Awesome Backseat Driver / babashka plugin). Install **`clojure`** skill when form-editing work needs it — not required solely to finish Hello.
5. Mark progress in `AGENTS.md`.

### 2. Calva + Calva Backseat Driver (Cursor)

Prefer shell/`cursor` CLI — **not Joyride**:

```bash
cursor --list-extensions
cursor --install-extension betterthantomorrow.calva
cursor --install-extension betterthantomorrow.calva-backseat-driver
```

Confirm with `--list-extensions`. Other harnesses: install equivalent Clojure nREPL client + Backseat Driver (or peer) via that harness’s docs.

### 3. Clojure CLI Observe → LSP unblock only

Workspace ships `"calva.enableClojureLspOnStart": "never"` in `.vscode/settings.json` so clones without Clojure avoid LSP pain.

```
λ clojure_lsp_observe.
  clojure_on_PATH? → remove("calva.enableClojureLspOnStart" = "never")
  | ¬install(Java ∨ Clojure) as_Yardcraft_setup
```

If `clojure` is on `PATH`, remove that workspace setting so Calva clojure-lsp can auto-start. Do **not** install Java/Clojure as a Yardcraft setup step.

### 4. Babashka (common — often before Blender)

1. Observe: `bb` on `PATH`? Babashka REPL connected?
2. **Do mode:** install Babashka if missing; ask human to **Calva: Start a Project REPL and Connect (Jack-in)** → Project Type **Babashka**. Confirm ember REPL status + green **`bb`** indicator.
3. **Instructions-only:** give install + jack-in steps; wait for human confirmation.
4. Host automation stays on session `bb`; Blender/`bpy` stays on `basilisp-blender` later.
5. Mark progress when `bb` is real.

### 5. Blender

- **Human-facing:** install / upgrade **latest** from [blender.org/download](https://www.blender.org/download/).
- **Agent-private floor:** ≥ **5.2.0 LTS** for Observe/compat checks — speak version numbers only when checking or troubleshooting.

Calibrate Blender skill early (**1–5**) and **do vs instructions-only** — shapes how much you drive vs guide for UI-only steps.

### 6. basilisp-blender (PEZ zip)

Release: [PEZ v0.5.0-basilisp-0.5.1](https://github.com/PEZ/basilisp-blender/releases/tag/v0.5.0-basilisp-0.5.1)  
Asset: `basilisp_blender_extension-0.5.0.zip` from that tag.

```
λ basilisp_blender_install.
  human_quits_Blender → download(PEZ_zip) → CLI_install_file → enable
  | blender --command extension install-file <zip> -r user_default -e
  | macOS_fallback → /Applications/Blender.app/Contents/MacOS/Blender …
  | CLI_fails → Install_From_Disk (human)
  | details → basilisp-blender skill ∧ references/upgrade-basilisp.md
```

**Quit Blender first.** Then install+enable via CLI. Resolve `blender` on `PATH`; macOS app-bundle fallback above when bare `blender` is missing. **Install From Disk** only if CLI is missing or fails. Depth: **`basilisp-blender`** skill → [upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md).

### 7. nREPL (human path)

Human reopens Blender, then:

1. **Output Properties** (printer icon)
2. **Basilisp nREPL server** panel
3. Project path = **repo root**
4. **START SERVER**

Screenshot: [`recipe/readme/images/basilisp-blender-nrepl-panel.png`](../../readme/images/basilisp-blender-nrepl-panel.png)

### 8. Calva connect

1. **Calva: Connect to a running REPL server in the project**
2. Sequence / project type: **`basilisp-blender`** (not generic `basilisp` alone)
3. Expect green **`basilisp-blender`** status-bar indicator
4. `user/init!` runs via the connect sequence (`afterPrimaryReplConnectedCode`) — **do not re-run every time**. Re-run after Blender restart / blown `sys.path` only.

### 9. Early win — demo (not empty site)

```clojure
(require '[yardcraft.site :as site])
(site/ensure-demo!)
```

Success for layer 1 is the **welcome demo** (letters, furniture, sundial, orbit fly, Yardcraft panel). Demo **registers** the N-panel. Empty `(ensure-site! …)` is for later real-base / insufficient-facts work — not the Hello win.

**Ask the human what they see** in the Blender viewport (and panel). Point out Set time / lounger delight and Fly cam as demo-safe. Mark progress: demo shown.

### 10. Hand off

When the human is ready for real site facts, leave layer 1 and load **`yardcraft-base-design`**. Update `AGENTS.md` phase accordingly.

## Do vs instructions-only

| Mode | Agent acts |
|---|---|
| **Do** (default when human chose Do) | Install extensions, Babashka, download PEZ zip, CLI `extension install-file`, drive REPL/demo; human still does Blender UI clicks (nREPL panel) and Calva jack-in/connect when those need the human |
| **Instructions-only** | Spell clicks and commands; wait for confirmation; still Observe before prescribing |

## Invariants

- One shape ever — present the current install/connect path only
- Query before install when Observe already shows green
- Destructive Blender ops → confirm with human
- Structural edits for `.cljc` forms once editing starts (`clojure` skill)
