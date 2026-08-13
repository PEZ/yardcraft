# README / onboarding scratchpad

Working notes for crafting visitor-facing onboarding. **Not** for GitHub visitors.

## Collaboration workflow

- PEZ owns story voice and beats in `README.md` / `EXAMPLE-COOKING.md`.
- Agent: **understand** PEZ-written parts; only apply **spelling and grammar** there.
- If something looks wrong, unintentional, or unclear → **ask**, don’t rewrite.
- Agent-authored drafts may be rewritten more freely until PEZ takes the pen on that passage.
- Design the experience in the example docs first; then update `AGENTS.md` / skills so a real agent can deliver it.

## Crafting rules (author → agent behavior in examples)

- Human clones / “Use this template” **before** Hello — agent needs the repo (`AGENTS.md`, recipe).
- Don’t ask the user for facts the agent knows or can check (harness, `PATH`, repo contents).
- Visitor-facing text = **need-to-know**: outcomes and next moves, not tooling meta (no LSP settings essays, no Setup-progress lectures).
- LSP / `clojure` on `PATH`: at clone-time check; enable Calva LSP if CLI present; otherwise leave `"never"`; offer richer editor nav **after base design** — Java/Clojure not part of Yardcraft setup. Keep this out of the example chat unless a short user-facing offer fits later.
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
- Bundles **Basilisp ≥ 0.5.1** — fixes [basilisp#1302](https://github.com/basilisp-lang/basilisp/issues/1302) (Calva load-file / module aliases) **without** old pip-into-`.local` overlay
- Pre-upstream: [PR #14](https://github.com/ikappaki/basilisp-blender/pull/14) to ikappaki — once merged+released upstream, prefer upstream again and drop special-version callout

### Editor extensions (Cursor CLI)

- **Not Joyride:** Prefer shell/`cursor` for check+install. Do **not** pull Joyride into the visitor story or agent setup narrative — users may get overwhelmed wondering where it fits.
- Verified on this machine: `cursor --install-extension mechatroner.rainbow-csv` succeeded (v3.24.1); confirm with `cursor --list-extensions`.
- Useful commands:
  - `cursor --list-extensions`
  - `cursor --install-extension <publisher.name>`
  - (also `--uninstall-extension`, `--update-extensions` exist)
- Target extensions for Yardcraft agent setup: **Calva** (`betterthantomorrow.calva`) and **Calva Backseat Driver** (`betterthantomorrow.calva-backseat-driver`).

## Tone & level of detail (from current story)

- Warm, direct, lightly playful (“Sweet.”, “Let’s get cranking!”).
- Agent speaks as the pair programmer, not a changelog.
- Early win: **demo scene** (YARDCRAFT letters, furniture, sundial, fly-cam, Yardcraft panel) before real-lot work — delight before survey grind.
- Calibrate Blender skill (1–5) and do-vs-instructions early.
- Human-only Blender UI steps spelled out with clicks; screenshot where it helps (`recipe/readme/images/`).
- Point to `EXAMPLE-COOKING.md` as optional process preview (memoir of maps + paper light-table + Epupp/Babashka); still allow jumping straight in.
- Intentional PEZ phrasings in the cooking doc (“more better”, “flat juice with flat juice”) — do not “fix”.

## Locked decisions

| Topic | Decision |
|---|---|
| Doc split | Short Getting Started + product tour in README; **personal base-design memoir** in `EXAMPLE-COOKING.md` (not a second example chat / not a pretend cook). Deviates from README example-chat shape by design. |
| Example site / images | Real PEZ terrace/driveway story; images under `recipe/example-source-images/` (`.jpg`: lot-road, house-shed-terrace, elevation-lines) — illustrative of one cook, not “swap for yours” beat |
| Harness in example | Cursor + Calva + Backseat Driver |
| Blender download | Link [blender.org/download](https://www.blender.org/download/) (story currently says “latest”) |
| Template flow | Use this template → clone → open → Hello |
| basilisp-blender zip | PEZ `v0.5.0-basilisp-0.5.1` until upstream ships Basilisp 0.5.1 (PR #14) |
| basilisp-blender install | Do mode → close Blender → agent CLI `extension install-file … -e` → reopen + nREPL panel; UI Install From Disk = fallback / instructions-only |

## Todos

- [ ] Write instructions in `AGENTS.md` and/or a skill: check whether Calva + Calva Backseat Driver are installed; install via `cursor --install-extension` if missing (commands above). Keep Joyride out of that path.
- [ ] Align `AGENTS.md` Setup with the example story (demo scene first, Calva connect UX, Blender “latest” vs ≥ 5.2 LTS, PEZ basilisp-blender zip download/install, LSP rules, progress block, don’t-ask-what-you-know).
- [x] Retire `recipe/skills/basilisp-blender/references/upgrade-basilisp.md` overlay procedure from the skill (point at bundled zip / PR instead).
- [x] Update `AGENTS.md` Setup / Session bootstrap: special PEZ zip for now; no ≥0.5 overlay step; note temporary until upstream PR merges.
- [ ] Align / refresh skills so demo scene + N-panel + fly-cam buttons are deliverable as written.
- ~~[ ] Align `EXAMPLE-COOKING.md` intro with post-demo state~~ — **obsolete:** cooking doc is a standalone memoir ending at base terrain, not continuing the example chat.
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

_(none right now)_
