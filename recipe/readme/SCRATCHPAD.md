# README / onboarding scratchpad

Working notes for crafting visitor-facing onboarding. **Not** for GitHub visitors.

## Collaboration workflow

- PEZ owns story voice and beats in `README.md` / `EXAMPLE-COOKING.md`.
- Agent: **understand** PEZ-written parts; only apply **spelling and grammar** there.
- If something looks wrong, intentional, or unclear → **ask**, don’t rewrite.
- Agent-authored drafts may be rewritten more freely until PEZ takes the pen on that passage.
- Design the experience in the example docs first; then update `AGENTS.md` / skills so a real agent can deliver it.

## Crafting rules (author → agent behavior in examples)

- Human clones / “Use this template” **before** Hello — agent needs the repo (`AGENTS.md`, recipe).
- Don’t ask the user for facts the agent knows or can check (harness, `PATH`, repo contents).
- Visitor-facing text = **need-to-know**: outcomes and next moves, not tooling meta (no LSP settings essays, no Setup-progress lectures).
- LSP / `clojure` on `PATH`: at clone-time check; enable Calva LSP if CLI present; otherwise leave `"never"`; offer richer editor nav **after base design** — Java/Clojure not part of Yardcraft setup. Keep this out of the example chat unless a short user-facing offer fits later.
- basilisp-blender: agent downloads the extension zip; install via Blender UI (Install From Disk) or `blender --command extension install-file … -e` if `blender` is on `PATH`.
- Setup progress in `AGENTS.md` useful for reload / new-chat resume (not the main clone handoff anymore).

## Tone & level of detail (from current story)

- Warm, direct, lightly playful (“Sweet.”, “Let’s get cranking!”).
- Agent speaks as the pair programmer, not a changelog.
- Early win: **demo scene** (YARDCRAFT letters, furniture, sundial, fly-cam, Yardcraft panel) before real-lot work — delight before survey grind.
- Calibrate Blender skill (1–5) and do-vs-instructions early.
- Human-only Blender UI steps spelled out with clicks; screenshot where it helps (`recipe/readme/images/`).
- Point to `EXAMPLE-COOKING.md` as optional process preview; allow jumping straight in.

## Locked decisions

| Topic | Decision |
|---|---|
| Doc split | Short Getting Started in README; longer pretend cook in `EXAMPLE-COOKING.md` |
| Example site | **Example lot**, sample images under `recipe/example-source-images/` — pretend only, no “swap for yours” beat |
| Harness in example | Cursor + Calva + Backseat Driver |
| Blender download | Link [blender.org/download](https://www.blender.org/download/) (story currently says “latest”) |
| Template flow | Use this template → clone → open → Hello |

## Todos

- [ ] Align `AGENTS.md` Setup with the example story (demo scene first, Calva connect UX, Blender “latest” vs ≥ 5.2 LTS, basilisp-blender download/install, LSP rules, progress block, don’t-ask-what-you-know).
- [ ] Align / refresh skills so demo scene + N-panel + fly-cam buttons are deliverable as written.
- [ ] **Pending:** Align `EXAMPLE-COOKING.md` intro with post-demo state (demo still up / about to replace — not “empty site”). Wait a bit before editing.
- [ ] GIF/screenshot for demo viewport placeholder.
- [ ] Pre-cooked demo scene + UI (YARDCRAFT, furniture, sundial, fly-cam, time slider / Set time / Fly Cam) so Hello→connected is fast. *(blocked briefly by site↔site-ui cycle; compile fixed; scene cleared — resume demo cook)*
- Blender nREPL UI terms (from screenshot): **Output Properties** tab (printer icon) → **Basilisp nREPL server** panel → project path + **START SERVER**.
- Fixed: `site.cljc` must **not** `:require` `site-ui` (cycle → `clear-site!` unresolved). RCF requires UI locally.
- Tip: if `sys.modules` has a `nil` tombstone for `yardcraft`, pop it; prefer absolute `src` on `sys.path` via `user/init!`.
- [ ] Voice pass: PEZ continues story; agent only spelling/grammar + asks.
- [ ] Wire recipe machinery after the story feels right.

## Open questions (ask PEZ)

_(none right now)_
