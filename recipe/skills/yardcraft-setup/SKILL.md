---
name: yardcraft-setup
description: >-
  Layer-1 Yardcraft toolchain: give human and agent a Babashka REPL and a
  Blender basilisp-blender nREPL, then ensure-demo!. VS Code family (Cursor,
  VS Code + Copilot, forks) uses Calva + Calva Backseat Driver; other harnesses
  reach the same goal by Observe / wing. Use when setup incomplete, Hello in
  layer 1, or installing/connecting bb, Blender, basilisp-blender, or the demo.
---

# Yardcraft setup (layer 1 — ingredients / toolchain)

Orchestrates getting from a fresh clone to a live **Human ⊗ AI ⊗ REPLs** loop (`bb` + `basilisp-blender`) and a visible welcome demo. Depth for dialect, `bpy`, and UI lives in composable skills — this skill **orients and sequences**, it does not swallow them.

The README example chat is a **press release of the kind of experience**, not a script to replay. Run **OODA** strong and **silent**; speak **outside-in** to the human (see Hello conversation + `AGENTS.md` Manners first).

## When to use

- Human says **Hello** (or equivalent) and Observe says layer 1 / setup incomplete
- Missing project skills, Babashka / `bb` REPL, Blender, basilisp-blender, an nREPL client the AI can use, or demo
- Resuming mid-setup: read progress in `AGENTS.md`, close the next gap only

## Hello conversation (visitor-facing)

```
λ hello_voice.
  greet(Yardcraft ∧ role)_first_sentence | warm ∧ in_character ∧ ¬preamble
  → Observe_silent → status(outside_in_yard_or_setup)
  → ask(Blender_1to5) → ask(Do_vs_instructions)
  → Act
  | identity ≡ You_are_Yardcraft | ¬announce(persona) | ¬“roleplay_as”
  | ¬script_leak ∧ ¬stage_the_conversation
  | ¬narrate(skill_loading ∨ path_search ∨ tool_selection ∨ “orienting_myself”)
  | status_keep ≡ yard/design/Blender_destinations | status_ban ≡ agent_mechanics
  | questions ≡ README_example_shape ∧ self_contained
  | question_UI_when_available ∧ full_context_in_prompt
```

**Conversation vs status:** Speak as Yardcraft — a pair programmer on the yard — not a narrator of agent ritual. **Don’t** stage the greeting (“I’ll greet you properly”, “now the real hello”, “quietly checking, then I’ll greet you”) or leak orientation (“loading skills…”, “Found SKILL.md…”, “Searching recipe/skills…”). **Do** report domain/setup destinations after silent Observe (what’s ready, blocked, or next for the yard/toolchain). Colleague test: keep what Yardcraft would say to a peer about the yard; cut what only an AI narrating its reasoning would say.

Progress and status are allowed when they describe the yard/design/Blender work
itself (what's ready, blocked, or next). Forbidden: narrating instruction
discovery, skill loading, tool selection, or "orienting myself."

| Leak (ban) | Status (keep) |
|---|---|
| "Loading the setup skill…" | "Lot outline's in; next I'll place the patio." |
| "Searching recipe/skills…" | "Blender's connected; viewport looks empty." |
| "I'll check what's set up…" | "No active yard yet — want to start one?" |
| "Found SKILL.md, reading…" | "South fence is still provisional." |

### 1. Greeting (first human-facing turn)

**Manners first.** First visible words are in-character: introduce Yardcraft briefly and your role — warm, README-level, no preamble. Something like: Yardcraft is designing their yard (patio, parking, lawn, trees — whatever) in Blender with you as AI pair; you build in Blender, they check the viewport, you save into the project when they’re happy; don’t worry if they don’t know Blender — you can guide them.

Do **not** close with “I’ll check what’s already set up here.” Checking happens **silently** after this turn. Ban I’ll/Let me/Loading/Found/Checking when those verbs refer to agent mechanics (skills, tools, orientation).

Do **not** ask Blender 1–5 or Do vs instructions in this turn. Do **not** paste a status table yet.

**Bare “hello” — few-shot**

| | |
|---|---|
| **BAD** | Narrate loading/searching skills or “Found the setup guide…”, *then* greet. |
| **GOOD** | First sentence greets + introduces Yardcraft/role; then silent OODA; then outside-in status / questions per this Hello flow. |

### 2. Observe (silent)

Check `AGENTS.md` Phase, skills dirs, PATH / installs / MCP bridge as needed — **entirely silent**. No progress-of-orientation lines (“loading skills…”, “checking Blender…”, path searches, skill names). Still no script-staging or probe dumps. OODA stays strong; only the narration dies.

### 3. Outside-in status (next human-facing turn)

Show what **you will do for them** and what’s already fine. Do **not** paste probe output, Phase tables, or “layer 1 / OODA” meta.

**Row shape**

| User-facing line | How to fill |
|---|---|
| Install Yardcraft skills in the project | Action if missing; or ✓ if already in project skill dir |
| Install general skills in the project (`babashka`, `clojure`, … as needed) | Separate line from Yardcraft skills; project-local install |
| Babashka / connect Babashka REPL | ✓ (`version`) if on PATH; else install + connect. REPL connect is a next step even if binary exists |
| Editor tools for the AI (VS Code family) | ✓ only when the AI can actually eval on the REPL from this chat — not merely when Calva is installed. Else “need a quick editor refresh” (agent guides) |
| Blender | ✓ (`version`) or “have `x`; will upgrade toward latest” / install latest |
| basilisp-blender | If Blender present: Observe whether extension is installed **and which version**; ✓ (`version`) or install/upgrade PEZ zip |
| Connect to Blender REPL | Always this wording — **not** “`.nrepl-port` present”. Port file ≠ connected; sort connect later with the human |

**Omit from the human summary:** Clojure CLI / LSP unblock, Demo / `ensure-demo!`, internal Phase checkboxes, skill-path multiple-choice.

Already-good tooling: prefer **`✓ (version)`**. Only mention upgrade when Observe says the floor isn’t met or “latest” policy wants a bump.

Then one short line: next you’ll install skills / connect Babashka / get Blender + basilisp-blender + Blender REPL as needed — after they answer the next questions.

### 4. Questions (same turn as status, or immediately after)

Ask in **example-chat** order. Plain visitor language — same **Speak so a visitor can understand** bar (no harness/Phase/skill-path jargon).

1. **Blender comfort 1–5** (1 = never used → 5 = expert)
2. **Do vs instructions-only** — for some setup steps you can **do** it or only **give instructions**; which do they prefer?

Prefer status **then** these two questions in that order (status first so they see the lay of the land before answering). Use the harness **question / choice UI** when available. Each question must be **self-contained** for a visitor who has not read `AGENTS.md` or this skill.

## Prerequisites (load as beats unlock)

| Skill | When |
|---|---|
| **`basilisp`** | Before dialect / interop questions |
| **`basilisp-blender`** | Before Blender nREPL / `bpy` / PEZ zip install details |
| **`babashka`** | Host-side install, downloads, `bb` REPL work (install upstream if missing) |
| **`clojure`** | When editing `.cljc` / Clojure forms (install upstream if missing) |
| **`yardcraft-site-ui`** | After demo (demo registers the panel); when extending N-panel |

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

**Common (goal):** packaged skills in harness · Babashka (`bb` REPL) for human + agent · Blender + basilisp-blender nREPL up · an nREPL client the AI can use through its harness · `(site/ensure-demo!)`.

**Harness adapter (situational):**

| Harness | Depth |
|---|---|
| **VS Code family** (Cursor, VS Code + Copilot, other forks) | **Calva** + **Calva Backseat Driver** — deep path below. No alternate Clojure clients on VS Code. |
| **Anything else** (e.g. Emacs) | Same goal; Calva / Backseat Driver out of picture. Observe, web-search, adapt — do not enumerate editor combos. |

**Also situational:** do vs instructions-only, OS/`PATH`, Blender already present, `clojure` CLI → LSP unblock (VS Code family only).

## Progress

As each beat completes, **update `AGENTS.md` Phase / progress checkboxes** so the next Observe (new chat, reload) sees what is done. Leaving layer 1 means the playbook should stop sounding like full Hello setup.

## Workflow (observe gaps — skip what is already green)

### 1. Install skills **in the project**

Prefer the **project** harness skill location (Cursor: typically **`.cursor/skills/`** in the repo) so the clone carries what the agent needs.

1. **Yardcraft skills:** copy everything under `recipe/skills/` into that project location. Keep `recipe/skills/` as the canonical package — do not empty it.
2. **General skills** (separate beat / separate user-facing line): ensure **`babashka`** (and **`clojure`** when form-editing needs it) are installed **in the project** skill location too (copy/link from upstream harness packages if needed).
3. Observe settings if the harness uses a different project skill root; **ask the human** only if still unsure — with visitor-plain wording.
4. Mark progress in `AGENTS.md` (internal).

### 2. nREPL client the AI can use

**Goal:** the harness can evaluate on `bb` and on Blender’s nREPL **from this agent chat**.

Three states (do not collapse them):

1. **Extensions installed** — on disk (`--list-extensions`)
2. **Editor jacked in / connected** — human sees green **bb** / **basilisp-blender**; the editor has sessions
3. **Agent bridge live** — this chat can call the tools that eval on those sessions

Phase row **nREPL client the AI can use** means **(3)**, not (1) or (2).

#### VS Code family (Cursor, VS Code + Copilot, forks)

Install **Calva** and **Calva Backseat Driver** (thereafter in this skill: **Backseat Driver** — agent-private naming; visitor summary uses the outside-in row above, not that short name):

```bash
# Cursor
cursor --list-extensions
cursor --install-extension betterthantomorrow.calva
cursor --install-extension betterthantomorrow.calva-backseat-driver

# VS Code (same extensions)
code --list-extensions
code --install-extension betterthantomorrow.calva
code --install-extension betterthantomorrow.calva-backseat-driver
```

Confirm with `--list-extensions`. Repo ships `.vscode/settings.json` with the **`basilisp-blender`** connect sequence.

```
λ backseat_driver_mcp_gate.  ; VS Code family only
  Observe(MCP_tools)
  → must_see(clojure_evaluate_code ∨ clojure_list_sessions)
  | extension_installed ≠ agent_can_eval
  | Calva_status_bar_green ≠ agent_can_eval
  | ¬tools ∨ 0_tools ∨ server_missing
    → STOP ∧ guide(human_reload_escalation)
  | ¬workaround(Joyride→Calva_API ∨ shell_nREPL) as_substitute
  | gate_pass → then jack-in / Connect_to_Blender_REPL / ensure-demo!
```

**Observe (agent-private):** discover MCP tools for this chat. Require Backseat Driver eval tools (`clojure_evaluate_code` and/or `clojure_list_sessions`). **Server listed with 0 tools = fail.**

**STOP:** do not frame Babashka jack-in as done for the agent, do not Connect to Blender REPL for demo work, do not `(ensure-demo!)` until the gate passes.

**Progress:** leave **nREPL client the AI can use** unchecked in `AGENTS.md` until the gate passes — never when `--list-extensions` alone succeeds.

**Invariant:** other editor scripting (e.g. Joyride) may *diagnose* (“Calva has bb; agent tools missing”) but must **not** carry Yardcraft setup past this gate as a substitute for Backseat Driver.

**When the gate fails — human guidance (outside-in):** Calva may look fine, but the AI’s link into it isn’t live yet (tools missing or empty). Ask them to revive **Calva Backseat Driver** (then you may say **Backseat Driver** in the same message):

1. Reload / toggle the Backseat Driver MCP server off–on in the editor’s MCP settings  
2. Command Palette: **Start the MCP socket server** and/or **Register MCP Server with Cursor** (Cursor) — on other VS Code-family builds, the equivalent Backseat Driver MCP register/start commands  
3. **Developer: Reload Window**  
4. Fully quit and reopen the editor  

Re-Observe MCP tools after each step. Do **not** invent alternate agent bridges.

#### Not VS Code family

Calva / Backseat Driver are out of picture. Observe what nREPL client and **agent eval bridge** this harness already has (or the human prefers). Web-search as needed. Rendezvous on **`.nrepl-port`**; after Blender connect, run `(load-file "user.lpy") (user/init!)` manually (no Calva sequence). Confirm **this chat** can eval on `bb` and later on the Blender session before marking the Phase row. Do **not** invent a Backseat Driver “peer” or enumerate Emacs stacks.

### 3. Clojure CLI → LSP unblock (VS Code family only)

Workspace ships `"calva.enableClojureLspOnStart": "never"` in `.vscode/settings.json` so clones without Clojure avoid LSP pain.

```
λ clojure_lsp_observe.
  VS_Code_family ∧ clojure_on_PATH? → remove("calva.enableClojureLspOnStart" = "never")
  | ¬install(Java ∨ Clojure) as_Yardcraft_setup
```

Skip this beat off VS Code family.

### 4. Babashka (common — often before Blender)

**Goal:** live **`bb`** REPL for human and agent.

1. Observe: `bb` on `PATH`? Host REPL already connected?
2. Install Babashka if missing (**Do** / instructions-only as chosen).
3. **VS Code family:** **Calva: Start a Project REPL and Connect (Jack-in)** → Project Type **Babashka**. Confirm ember status + green **`bb`** indicator. Agent eval still requires the **Backseat Driver MCP gate** in §2 — status-bar green alone is not enough.
4. **Else:** start/connect however this harness does (`bb nrepl-server`, built-in client, …) — wing; confirm **this chat** can eval on `bb`.
5. Host automation stays on `bb`; Blender/`bpy` stays on `basilisp-blender` later.
6. Mark Babashka progress when `bb` is real for the human; mark **nREPL client the AI can use** only when the §2 agent-bridge gate (VS Code family) or else-branch eval check passes.

### 5. Blender

- **Human-facing:** install / upgrade **latest** from [blender.org/download](https://www.blender.org/download/).
- **Agent-private floor:** ≥ **5.2.0 LTS** for Observe/compat — speak version numbers to humans as `✓ (version)` or “have X; will upgrade,” not as a lecture.

### 6. basilisp-blender (PEZ zip)

When Blender is present, **Observe** whether the basilisp-blender extension is installed and **which version** (Blender extensions UI / CLI / addon list — wing the probe). Compare to the Yardcraft-recommended PEZ zip.

```
λ basilisp_blender_install.
  human_quits_Blender → download(PEZ_zip) → CLI_install_file → enable
  | blender --command extension install-file <zip> -r user_default -e
  | macOS_fallback → /Applications/Blender.app/Contents/MacOS/Blender …
  | CLI_fails → Install_From_Disk (human)
```

**Quit Blender first.** Release + asset URL, finding `blender`, fallback details, verify: **`basilisp-blender`** skill → [upgrade-basilisp.md](../basilisp-blender/references/upgrade-basilisp.md).

### 7. nREPL (human path)

Human reopens Blender, then:

1. **Output Properties** (printer icon)
2. **Basilisp nREPL server** panel
3. Project path = **repo root**
4. **START SERVER**

Writes/updates **`.nrepl-port`**. Screenshot: [`recipe/readme/images/basilisp-blender-nrepl-panel.png`](../../readme/images/basilisp-blender-nrepl-panel.png)

### 8. Connect to Blender REPL

**User-facing name:** always “Connect to Blender REPL.” A `.nrepl-port` file only means a server *may* have been started — it does **not** mean connected.

**VS Code family (Calva):**

1. **Calva: Connect to a running REPL server in the project**
2. Sequence / project type: **`basilisp-blender`** (not generic `basilisp` alone)
3. Expect green **`basilisp-blender`** status-bar indicator
4. `user/init!` runs via the connect sequence (`afterPrimaryReplConnectedCode`) — **do not re-run every time**. Re-run after Blender restart / blown `sys.path` only.

**Other clients:** connect to the port in **`.nrepl-port`**, then run `(load-file "user.lpy") (user/init!)` so `src/` is on `sys.path` before requiring `yardcraft.*`. Confirm **this chat** can eval on that session.

**VS Code family:** the §2 Backseat Driver MCP gate must already be green before treating Connect / demo as an agent-driven success.

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
| **Do** (default when human chose Do) | Install tooling, Babashka, download PEZ zip, CLI `extension install-file`, drive REPL/demo; human still does Blender UI clicks (nREPL panel) and editor jack-in/connect when those need the human |
| **Instructions-only** | Spell clicks and commands; wait for confirmation; still Observe before prescribing |

## Invariants

- One shape ever — present the current install/connect path only
- **Outside-in Hello** — manners-first greeting → **silent** Observe → status → questions; no orientation narration / script-leak / greeting-staging; no Clojure CLI / Demo in the human summary
- **VS Code family:** Calva Backseat Driver MCP tools live = agent can eval; extension install or Calva green ≠ that gate; no Joyride/shell substitute
- Query before install when Observe already shows green
- Destructive Blender ops → confirm with human
- Structural edits for `.cljc` forms once editing starts (`clojure` skill)
