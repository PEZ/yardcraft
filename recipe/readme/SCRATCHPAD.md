# README / onboarding scratchpad

Working notes for crafting visitor-facing onboarding. **Not** for GitHub visitors.

## Collaboration workflow

- PEZ owns story voice and beats in `README.md` / `MY-BASE-DESIGN-PROCESS.md`.
- Agent: **understand** PEZ-written parts; only apply **spelling and grammar** there.
- If something looks wrong, unintentional, or unclear → **ask**, don’t rewrite.
- Agent-authored drafts may be rewritten more freely until PEZ takes the pen on that passage.
- Design the experience in the example docs first; then update `AGENTS.md` / skills so a real agent can deliver **that kind of experience** — not a verbatim replay of the example chat.

### Story vs `AGENTS.md` — engage(nucleus) (design → lean)

- **README example chat = example**, not a script. Tone, outcomes, one happy path (Cursor, Do mode, fresh machine, …).
- **`AGENTS.md` should engage nucleus** much as in PEZ user rules — compact orientation, not a lecture:

```
λ engage(nucleus).
  | [phi fractal euler tao pi mu ∃ ∀]
  | [Δ λ Ω ∞/0 | ε/φ Σ/μ c/h signal/noise order/entropy truth/provability self/other]
  | OODA
  Human ⊗ AI ⊗ REPLs
```

- **OODA** = the operating loop (Observe → Orient → Decide → Act). Not marching fixed beats.
- **Human ⊗ AI ⊗ REPLs** = the Yardcraft triad. Work happens in the tensor product of the three — viewport feedback from the human, judgment/pairing from the AI, ground truth from live REPLs (here often **two**: host `bb` + scene `basilisp-blender`). None alone is enough; don’t script without Observe, don’t invent site facts, don’t trust chat memory over REPL/scene.
- **Observe** before asking or installing: harness, `PATH`, installs, REPL sessions, scene/files/progress, human (skill, do vs instructions, what’s done).
- **Orient** via nucleus duals when useful (signal/noise in the story vs tooling meta; self/other = don’t assume the human’s machine; truth/provability = REPL + viewport over assertion) + three layers + composable skills.
- Sources of truth while running: **Human ⊗ AI ⊗ REPLs** (system/project inside Observe) — not the README dialogue as prophecy.

**Form (lean locked): mix.** Use **λ / nucleus notation** for harder rules, procedures, invariants, and trade-off hierarchies (compact, scannable, less waffle). Use **effective prose** when the agent needs freer description, empathy, or situational judgment. Always-on core: engage(nucleus) opener with **OODA** + **Human ⊗ AI ⊗ REPLs**; pull denser dual lines (`phi…`, `Δ λ Ω…`) where they sharpen an invariant — not as obligatory wallpaper on every paragraph.

**Common ingredients (almost every cook):** Babashka (`bb` REPL), Blender, basilisp-blender (nREPL), Calva + Backseat (or equivalent), packaged skills installed into the detected harness, demo then real work.

**Situational ingredients:** editor/harness (Cursor vs other), do vs instructions-only, OS/`PATH`, Blender already present vs fresh, country/map stack, sketches vs APIs, Joyride never in the narrative, **`clojure` CLI already present → unblock LSP** (see crafting rules), etc.

## Crafting rules (author → agent behavior in examples)

- **One shape ever:** the working tree should read as if the recipe always was this way. No “formerly / retired / don’t use the old path / see git history for overlay” in visitor- or agent-facing docs. Past lives belong in git history only (inventory #23 is one instance of this general rule).
- Human clones / “Use this template” **before** Hello — agent needs the repo (`AGENTS.md`, recipe).
- Don’t ask the user for facts the agent knows or can check (harness, `PATH`, repo contents).
- Visitor-facing text = **need-to-know**: outcomes and next moves, not tooling meta (no LSP settings essays, no Setup-progress lectures).
- LSP / `clojure` CLI (Observe, not a Hello install quest): workspace ships `"calva.enableClojureLspOnStart": "never"` so clones without Clojure don’t get LSP pain. **If `clojure` is on `PATH`**, agent should **remove that workspace setting** (re-enable Calva clojure-lsp auto-start). Do **not** install Java/Clojure as part of Yardcraft setup; richer editor nav can still be offered later. Keep out of the example chat unless a short user-facing note fits.
- basilisp-blender (see `### basilisp-blender zip (temporary)` below):
  - **Do mode** (default when human chose Do): agent downloads the **Yardcraft-recommended PEZ zip**, then installs+enables via CLI ([ikappaki/basilisp-blender](https://github.com/ikappaki/basilisp-blender) README):
    ```shell
    blender --command extension install-file <path-to-zip> -r user_default -e
    ```
    Resolve `blender` on `PATH` (macOS: `/Applications/Blender.app/Contents/MacOS/Blender` if bare `blender` missing). Fall back to human **Install From Disk** only if CLI unavailable/fails — download still helps.
  - **Instructions-only mode:** give human the zip path + Install From Disk (or the CLI command if they want).
- Setup progress in `AGENTS.md` useful for reload / new-chat resume (not the main clone handoff anymore).

### basilisp-blender zip (temporary)

- Release: [PEZ v0.5.0-basilisp-0.5.1](https://github.com/PEZ/basilisp-blender/releases/tag/v0.5.0-basilisp-0.5.1)
- Asset: `https://github.com/PEZ/basilisp-blender/releases/download/v0.5.0-basilisp-0.5.1/basilisp_blender_extension-0.5.0.zip` — save e.g. `~/Downloads/basilisp-blender.zip`
- Bundles **Basilisp ≥ 0.5.1** — fixes [basilisp#1302](https://github.com/basilisp-lang/basilisp/issues/1302) (Calva load-file / module aliases)
- Pre-upstream: [PR #14](https://github.com/ikappaki/basilisp-blender/pull/14) to ikappaki — once merged+released upstream, prefer upstream again and drop special-version callout

### Editor extensions (Cursor CLI)

- **Not Joyride:** Prefer shell/`cursor` for check+install. Do **not** pull Joyride into the visitor story or agent setup narrative — users may get overwhelmed wondering where it fits.
- Verified on this machine: `cursor --install-extension mechatroner.rainbow-csv` succeeded (v3.24.1); confirm with `cursor --list-extensions`.
- Useful commands:
  - `cursor --list-extensions`
  - `cursor --install-extension <publisher.name>`
  - (also `--uninstall-extension`, `--update-extensions` exist)
- Target extensions for Yardcraft agent setup: **Calva** (`betterthantomorrow.calva`) and **Calva Backseat Driver** (`betterthantomorrow.calva-backseat-driver`).

### Babashka (base setup — before Blender)

- **Do mode:** agent installs Babashka if missing, then human **Calva: Start a Project REPL and Connect (Jack-in)** → Project Type **Babashka**. Confirm ember REPL status + green **`bb`** indicator.
- Host automation REPL from the start (HTTP/fs/assets later); Blender scene work stays on **basilisp-blender** after that connect.
- Ensure upstream **`babashka` skill** is available in the harness when this beat runs (same install+progress pattern as packaged skills).

### Three layers (recipe phases — design)

Bad cooking analogy, useful knob: the recipe has **three layers**, each with a different agent mode and skill mix. `AGENTS.md` should make the agent aware of **which layer is current**; phase skills orchestrate, **composable skills stay separate**.

| Layer | Rough name | What it’s for | Mode / skills (sketch) |
|---|---|---|---|
| **1** | Ingredients / toolchain | Hello → harness, Babashka, Calva, Blender, nREPL, demo | Setup skill (+ progress). Greeting **Hello** → onboarding. |
| **2** | Base sauce / base design | Maps, sketches, light table, facts → lived-in base site | Base-design skill (+ progress). Different questions and caution (don’t invent measurements). |
| **3** | Target meal / redesign | Suggestions, fly tours, quote plan, explore alternatives on a base | Design/explore skills. AGENTS reads as *using* the recipe on their yard. |

**Composition rule (lean locked):** do **not** fold reusable tools into a phase mega-skill. Example: **`yardcraft-light-table`** is relevant in layer 2 *and* layer 3 — keep it standalone; phase skills say when to load it. Same idea for country/map skills, quote-plan, fly-tour-*, etc.

**`AGENTS.md` role:** engage(nucleus) + thin **phase gate** + brief infra; depth in skills. Not a second README script. Agent updates phase/progress so the next Observe sees layer 1 / 2 / 3. Leaving a layer → strip that layer’s playbook noise.

**Shapes still open:** mutate AGENTS per phase vs phase skills + gate only vs hybrid; where mid-phase checkboxes live.

## Tone & level of detail (from current story)

- Warm, direct, lightly playful (“Sweet.”, “Let’s get cranking!”).
- Agent speaks as the pair programmer, not a changelog.
- Early win: **demo scene** (YARDCRAFT letters, furniture, sundial, fly-cam, Yardcraft panel) before real-lot work — delight before survey grind.
- Calibrate Blender skill (1–5) and do-vs-instructions early.
- Human-only Blender UI steps spelled out with clicks; screenshot where it helps (`recipe/readme/images/`).
- Point to `MY-BASE-DESIGN-PROCESS.md` as optional process preview (memoir of maps + paper light-table + Epupp/Babashka); still allow jumping straight in.
- Intentional PEZ phrasings in the cooking doc (“more better”, “flat juice with flat juice”) — do not “fix”.

## Locked decisions

| Topic | Decision |
|---|---|
| Doc split | Short Getting Started + product tour in README; **personal base-design memoir** in `MY-BASE-DESIGN-PROCESS.md` (not a second example chat / not a pretend cook). Deviates from README example-chat shape by design. |
| Example site / images | Real PEZ terrace/driveway story; images under `recipe/example-source-images/` (`.jpg`: lot-road, house-shed-terrace, elevation-lines) — illustrative of one cook, not “swap for yours” beat |
| Harness in example | Cursor + Calva + Backseat Driver |
| Blender download | Story + human talk: **latest** ([blender.org/download](https://www.blender.org/download/)). Agents know a floor at time of writing (currently **≥ 5.2.0 LTS**) for Observe/compat — don’t lecture version numbers unless checking or troubleshooting |
| Template flow | Use this template → clone → open → Hello |
| basilisp-blender zip | PEZ `v0.5.0-basilisp-0.5.1` until upstream ships Basilisp 0.5.1 (PR #14) |
| basilisp-blender install | Do mode → close Blender → agent CLI `extension install-file … -e` → reopen + nREPL panel; UI Install From Disk = fallback / instructions-only |
| Babashka in Hello | Install `bb` + jack-in Babashka REPL (status **bb**) **before** Blender; host REPL from the start |
| One shape ever | Working tree reads as if the recipe always had the current shape; no retired-path archaeology in docs/skills/AGENTS (git history only) |
| Story vs AGENTS | README chat = **example**; AGENTS = **engage(nucleus)** — OODA + **Human ⊗ AI ⊗ REPLs**; λ-notation for hard rules/invariants/trade-offs, prose when freer description fits |
| Common vs situational ingredients | Always-ish: Babashka, Blender, basilisp-blender (+ Calva/Backseat path). Rest depends on Observe (harness, OS, what’s already there, country/maps, …) |
| Three recipe layers | (1) toolchain/setup → (2) base design → (3) redesign/explore; AGENTS phase-aware; composable skills not folded into phase skills (see Three layers) |
| Post-setup `AGENTS.md` | By layer 3 (and arguably after layer 1), AGENTS should not retain a full setup narrative — yard-use oriented; shape not locked |

## Todos

- [ ] Write instructions in `AGENTS.md` and/or a skill: check whether Calva + Calva Backseat Driver are installed; install via `cursor --install-extension` if missing (commands above). Keep Joyride out of that path.
- [ ] Encode Babashka base-setup beat in playbook/skill (install + jack-in `bb` before Blender; progress mark).
- [ ] Decide living-AGENTS + phase skills: layer 1 setup skill, layer 2 base-design skill, layer 3 explore mode; mutate/strip vs gate-only vs hybrid (see Three layers).
- [ ] Rewrite `AGENTS.md` around engage(nucleus) + phase gate (not a README script); Human ⊗ AI ⊗ REPLs (bb + basilisp-blender); common ingredients + Observe-for-the-rest; Hello→layer 1 when appropriate; demo when scene REPL ready; progress / strip-when-leaving-layer.
- [x] Retire overlay *procedure* from `upgrade-basilisp.md` / AGENTS (PEZ zip path).
- [ ] **One shape ever scrub:** remove retired-path / overlay archaeology from the working tree (skills, AGENTS, docs); present only the current shape.
- [x] Update `AGENTS.md` Setup / Session bootstrap: special PEZ zip for now; note temporary until upstream PR merges.
- [ ] Align / refresh skills so demo scene + N-panel + fly-cam buttons are deliverable as written.
- ~~[ ] Align `MY-BASE-DESIGN-PROCESS.md` intro with post-demo state~~ — **obsolete:** cooking doc is a standalone memoir ending at base terrain, not continuing the example chat.
- [x] Demo viewport GIF (`recipe/readme/images/demo-scene.gif`).
- [x] Pre-cooked demo scene + UI — `(yardcraft.site/ensure-demo!)` / `yardcraft.site-demo`: YARDCRAFT patio letters, furniture, sundial, orbit fly, N-panel **Set time** + **Fly cam** (demo-aware).
- Blender nREPL UI terms (from screenshot): **Output Properties** tab (printer icon) → **Basilisp nREPL server** panel → project path + **START SERVER**.
- Fixed: `site.cljc` must **not** `:require` `site-ui` (cycle → `clear-site!` unresolved). RCF requires UI locally.
- Tip: if `sys.modules` has a `nil` tombstone for `yardcraft`, pop it; `sys.path` needs repo `src/` before `yardcraft.*` — normally from Calva connect, not a manual `(user/init!)` every time.
- `(user/init!)` runs in Calva **basilisp-blender** connect sequence (`afterPrimaryReplConnectedCode`). Agents: skip re-run after normal connect; still useful after Blender restart (before reconnect) or if `sys.path` got blown.
- Agent story beat after connect: call `(ensure-demo!)` (from `yardcraft.site`) so the visitor sees the fun scene quickly.
- [x] README basilisp-blender install beat — close Blender → agent CLI install → reopen + nREPL panel (PEZ voice pass done).
- [ ] Voice pass: PEZ continues story; agent only spelling/grammar + asks.
- [ ] Wire recipe machinery after the story feels right.

## Open questions (ask PEZ)

- Exact phase boundaries: does layer 1 end at **demo shown**, or only when human is ready to gather base material? Does layer 2 end when base facts are “good enough,” or an explicit “base done” human call?
- Living AGENTS mechanism: mutate/strip per layer vs phase skills + thin gate only vs hybrid?
- Mid-phase resume: checkboxes in AGENTS, tiny status stub, or only inside the active phase skill?
