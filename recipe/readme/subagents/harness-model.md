# Harness / setup model

Present model for how Yardcraft setup orients across editors. Depth lives in `AGENTS.md` and `yardcraft-setup`; this note is the compact SoT for the split.

## Clarification

- **Goal is common and harness-agnostic:** the agent works to give **itself and the human** two REPL connections — host **`bb`** and Blender **`basilisp-blender`** nREPL. Then demo / yard work. Human ⊗ AI ⊗ REPLs.
- **VS Code family** (Cursor, VS Code + Copilot, other forks) is **one adapter**: **Calva** is the supported Clojure/nREPL client there — no alternate VS Code clients prepared. **Backseat Driver** rides with Calva (agent REPL/edit tooling path). Ship extra-good depth here: `.vscode/settings.json`, connect sequences, `cursor`/`code` CLI installs.
- **Not VS Code** (Emacs, …): Calva/Backseat **out of picture**. Same goal; agent **wings details** — Observe, web-search, adapt. No editor-combo encyclopedia.
- README example chat = one happy-path film (Cursor + Calva). Example ≠ the supported universe.
- Skills install dir: Observe harness settings/docs; ask human if unsure. No path registry.
- Phase checkbox: “nREPL client the AI can use through its harness (in VS Code this is Calva + Calva Backseat Driver)”.

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

## Connect invariant (all harnesses)

- VS Code family: Calva **`basilisp-blender`** sequence runs `(user/init!)`.
- Other nREPL clients: connect via **`.nrepl-port`**, then `(load-file "user.lpy") (user/init!)` so `src/` is on `sys.path` before requiring `yardcraft.*`.

## Non-goals

- No README rewrite or second example chat for harness coverage.
- No Emacs/CIDER/monroe/inf-clojure how-tos — wing means wing.
- No alternate Clojure clients on VS Code family; no Backseat “peers”.
- No renaming of the `basilisp-blender` connect sequence, session keys, or `.vscode/settings.json` contents.
