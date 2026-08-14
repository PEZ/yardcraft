# Harness / setup alignment plan

**Status:** implemented 2026-08-14 (PEZ locks: skills dir = Observe/ask; Phase = nREPL client AI can use / VS Code: Calva + Backseat).

## Clarification (restated)

- **Goal is common and harness-agnostic:** the agent works to give **itself and the human** two REPL connections — host **`bb`** and Blender **`basilisp-blender`** nREPL. Then demo / yard work. Human ⊗ AI ⊗ REPLs.
- **VS Code family** (Cursor, VS Code + Copilot, other forks) is **one adapter**: **Calva** is the supported Clojure/nREPL client there — no alternate VS Code clients prepared. **Backseat Driver** rides with Calva (agent REPL/edit tooling path). Ship extra-good depth here: `.vscode/settings.json`, connect sequences, `cursor`/`code` CLI installs.
- **Not VS Code** (Emacs, …): Calva/Backseat **out of picture**. Same goal; agent **wings details** — Observe, web-search, adapt. No editor-combo encyclopedia.
- README example chat = one happy-path film (Cursor + Calva). Example ≠ the supported universe.

## Current state (findings)

**F1 — Functional gap (the only place centering actually breaks the goal).** `(user/init!)` runs only via the Calva connect sequence in `.vscode/settings.json` (`afterPrimaryReplConnectedCode`). `AGENTS.md` §Connect says "Re-run `user/init!` only after Blender restart or a blown `sys.path`" — presuming the sequence ran. A non-Calva client connects to `.nrepl-port`, never gets `src/` on `sys.path`, and no doc says to run `(load-file "user.lpy") (user/init!)` manually. `.nrepl-port` itself (the harness-agnostic rendezvous) sits in the Stack table but never appears in connect instructions.

**F2 — Adapter promoted to goal.** Calva + Backseat listed inside **Common** ingredients in `AGENTS.md` §Common-vs-situational and `yardcraft-setup` (§Common line + frontmatter description "Babashka + Calva/Backseat"). Same category error in the Phase table: checkbox **"Calva + Backseat Driver"** reads as universal progress; meaningless on Emacs.

**F3 — Cursor treated as its own universe, not a VS Code fork.** `yardcraft-setup` §2 heading "(Cursor)" with `cursor` CLI only (no `code`); `AGENTS.md` "(Cursor path)"; SCRATCHPAD "editor/harness (Cursor vs other)", "Editor extensions (Cursor CLI)". VS Code + Copilot — same adapter, PEZ's expert turf — is invisible.

**F4 — Actively wrong sentence.** `yardcraft-setup` §2 tail: "Other harnesses: install equivalent Clojure nREPL client + Backseat Driver (or peer) via that harness's docs." Contradicts the clarification twice: invites alternate clients on VS Code family, and invents a Backseat "peer" that does not exist off-Calva.

**F5 — Wing has no home.** The non-VS-Code path exists only as a Situational fragment: "other harnesses (web-search skill install + nREPL client)". Right instinct, no landing spot — an agent on Emacs gets Calva-shaped instructions it must silently discard, with no stated goal to steer by.

**F6 — Babashka beat is client-scripted.** `yardcraft-setup` §4 and SCRATCHPAD prescribe "Calva: Start a Project REPL and Connect (Jack-in)" universally. The goal (a live `bb` REPL for human + agent) is never stated client-free.

**F7 — Good silence to keep.** README already hedges ("You may be using something else than Cursor", line 90) — it is the film, leave it (PEZ voice). `basilisp-blender` skill already says "Calva/CIDER" and "use generic basilisp when no Yardcraft sequence" — nearly right, needs one manual-init line. Incidental "Calva load-file ≥0.5" mentions in `basilisp`, `yardcraft-site-ui`, fly-tour skills are verified-environment facts, not prescriptions — leave.

## Target model

Two tiers; goal common, client adapted.

```
λ harness_model.
  goal(common) ≡ skills_in_harness ∧ bb_REPL ∧ blender_nREPL(basilisp-blender) ∧ demo
  | goal_holders ≡ human ∧ agent (both connected)
  | VS_Code_family(Cursor ∨ VS_Code+Copilot ∨ fork) → Calva + Backseat_Driver
      ∧ connect_sequence(user/init!) ∧ cursor|code --install-extension ∧ .vscode/settings.json
      ∧ ¬alternate_VS_Code_clojure_clients
  | ¬VS_Code_family → Calva/Backseat_out_of_picture
      ∧ wing(Observe → web-search → adapt) ∧ rendezvous(.nrepl-port)
      ∧ manual((load-file "user.lpy") (user/init!)) after_blender_connect
  | ¬enumerate(editor_combos)
```

Depth budget: VS Code family gets prescriptive verified steps; non-VS-Code gets ~5 goal-based lines, ever.

## Proposed changes (ordered, file-scoped)

### 1. `AGENTS.md`

- **Phase table:** rename row **"Calva + Backseat Driver"** → **"Editor REPL client"** with value note "(VS Code family: Calva + Backseat Driver)". Row "Harness skills installed": "(Cursor: `.cursor/skills/`)" → "(e.g. Cursor `.cursor/skills/`)" — one named example, not a registry.
- **§Common vs situational:** Common list = goal items only (packaged skills, `bb` REPL, Blender, basilisp-blender + nREPL, connect, demo). Add a two-sentence **harness adapter** note: VS Code family (Cursor, VS Code + Copilot) → Calva + Backseat, deep path, no alternate clients; anything else → same goal, wing it (Observe, web-search), Calva/Backseat out of picture.
- **§Connect:** keep the Calva sequence paragraph as VS Code-family depth. Append one sentence closing F1: other nREPL clients connect via `.nrepl-port` and must run `(load-file "user.lpy") (user/init!)` manually (no connect sequence does it for them).
- **§Stack table**, `user.lpy` row: "adds `src/` after Calva connect" → "adds `src/` after editor connect (Calva sequence runs it; other clients run it manually)".
- **§Coding preferences** item 4: tag "Calva + basilisp-blender + Babashka" as the VS Code-family stack, or reword client-neutral. Light touch.

### 2. `recipe/skills/yardcraft-setup/SKILL.md`

- **Frontmatter description:** goal-first — bb + Blender nREPL for human and agent; VS Code family via Calva/Backseat; other harnesses goal-oriented.
- **§Common/Situational lines:** mirror the AGENTS split (F2).
- **§2** → **"Editor REPL client — VS Code family (Cursor, VS Code + Copilot, forks)"**: both CLIs (`cursor --install-extension …` / `code --install-extension …`), confirm with `--list-extensions`. **Delete** the "equivalent client + Backseat peer" sentence (F4). Add a **"Not VS Code family"** sub-branch, ≤6 lines: restate the goal, Observe the harness, web-search its nREPL client story, note `.nrepl-port` + manual init live in steps 7–8, do not enumerate editors.
- **§3 LSP unblock:** label "VS Code family only" so non-VS-Code agents skip cleanly (`.vscode/settings.json` is inherently family-scoped).
- **§4 Babashka:** lead with the goal (live `bb` REPL, human + agent). VS Code family: keep jack-in steps verbatim. Else: `bb nrepl-server` (or the harness's way) + client connect — wing (F6).
- **§8** → **"Connect editor to Blender nREPL"**: VS Code family = current Calva content unchanged. Else: connect any nREPL client to `.nrepl-port`, then `(load-file "user.lpy") (user/init!)`, verify `src/` on `sys.path` before requiring `yardcraft.*` (F1).
- Everything else (§1 skills copy, §5 Blender, §6 PEZ zip, §7 nREPL panel, §9 demo, §10 handoff, Do-vs-instructions, Invariants) stays — already client-neutral or human-side.

### 3. `recipe/skills/basilisp-blender/SKILL.md` (light)

- After "Connect: Calva connect sequence …" (§Project directory) add one sentence: non-Calva clients connect to `.nrepl-port` and run `(load-file "user.lpy") (user/init!)` manually (F7 residue of F1).

### 4. `recipe/readme/SCRATCHPAD.md` (locks only)

- **Locked decisions:** add row **"Harness model"** capturing the clarification: goal common (bb + blender nREPL, human + agent); VS Code family = Calva + Backseat adapter, deep support, no alternate VS Code clients; non-VS-Code = wing, Calva/Backseat out of picture.
- Situational ingredients: "(Cursor vs other)" → "(VS Code family vs other)". Common ingredients: drop "(or equivalent)" after Calva + Backseat.
- "Editor extensions (Cursor CLI)" heading/body: note `code` CLI parity.

### 5. No-touch list

`README.md` (PEZ voice; example lock holds). `.vscode/settings.json` (it *is* the family adapter artifact). Incidental Calva mentions in `basilisp`, `yardcraft-site-ui`, `yardcraft-fly-tour-*` (environment facts). `yardcraft-base-design` (no client assumptions found).

**Acceptance sketch:** an agent on an Emacs-family harness reading AGENTS + yardcraft-setup reaches both REPLs without encountering a single instruction it must discard as Cursor/Calva-only; a VS Code + Copilot user gets Cursor-equal depth; the README film is byte-identical.

## Non-goals

- No README rewrite or second example chat.
- No Emacs/CIDER/monroe/inf-clojure how-tos — wing means wing.
- No alternate Clojure clients on VS Code family; no Backseat "peers".
- No renaming of the `basilisp-blender` connect sequence, session keys, or `.vscode/settings.json` contents.
- No new skills; no phase-boundary changes (separately open in SCRATCHPAD).

## Open questions for PEZ

1. ~~Skills install dir for VS Code + Copilot~~ — **Locked:** Observe harness settings/docs; ask human if unsure. No path registry.
2. ~~Phase checkbox~~ — **Locked (A):** “nREPL client the AI can use through its harness (in VS Code this is Calva + Calva Backseat Driver)”.

## Original Plan-producing Prompt

> You are assessing Yardcraft setup orientation and writing an alignment plan. ASSESSMENT + PLAN ONLY — do not implement file changes.
>
> **Repo:** /Users/pez/Projects/yardcraft
>
> **Locked clarification (source of truth):** The common, harness-agnostic goal is that the agent works to give itself and the human a Babashka REPL connection and a Blender (basilisp-blender) nREPL connection; then demo / yard work (Human ⊗ AI ⊗ REPLs). Treat Cursor and VS Code + Copilot as one **VS Code family**; Calva is the only Clojure/nREPL client we support there (no alternate VS Code clients), with Calva Backseat Driver tied to Calva for the agent tooling path. Ship extra-good VS Code-family support (`.vscode/settings.json`, connect sequences, `cursor`/`code` CLI installs). On non-VS-Code harnesses (e.g. Emacs), Calva and Backseat are out of picture; do not enumerate editor combos — the agent achieves the same goal by winging details (Observe, web-search, adapt). The README example chat is one happy-path film (Cursor + Calva), not the supported universe. Respect: one shape ever, OODA, skills start uninstalled under `recipe/skills/`.
>
> **Job:** (1) Read `AGENTS.md`, `recipe/skills/yardcraft-setup/SKILL.md`, `recipe/skills/yardcraft-base-design/SKILL.md` (skim), `README.md` Getting Started/example framing, `recipe/readme/SCRATCHPAD.md` (principles, three layers, Story vs AGENTS), `recipe/readme/subagents/inventory-X-cross-review.md`, `.vscode/settings.json`; spot-check the `basilisp-blender` skill for Calva-only assumptions. (2) Assess where the project over-centers Cursor-as-unique, under-states "VS Code family + Calva", or wrongly lists Calva as a universal Common ingredient instead of a VS-Code-family adapter — including Phase checkbox wording, Common vs Situational lists, setup-skill section order, and which Emacs silence is good vs a gap. (3) Write an actionable plan for a follow-up implementer: what to rewrite, what stays as deep VS Code/Calva adapter depth, how Phase progress should read, and how non-VS-Code Observe/wing instructions should look (short, goal-based).
>
> **Output:** `recipe/readme/subagents/plan-harness-setup-alignment.md` with sections: Clarification (restated) / Current state (findings) / Target model / Proposed changes (ordered, file-scoped) / Non-goals / Open questions for PEZ (only real unknowns) / Original Plan-producing Prompt. Be terse, skeptical, high-signal — fewer crisp findings over a laundry list.
