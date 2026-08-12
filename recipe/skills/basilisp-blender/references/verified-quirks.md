# basilisp-blender verified quirks (Yardcraft session)

Environment where probes ran: **Blender ≥ 5.2.0 LTS**, Basilisp nREPL via basilisp-blender, Calva session key `basilisp-blender`, **Python 3.13** (`sys.version`).

Companion dialect facts (file-def reset on `:reload`, dotted method symbols, …): **basilisp** skill → `references/verified-quirks.md`.

## Process-persistent state on `bpy`

Basilisp ns `:reload` reinitializes **file-defined** Var roots (see basilisp skill). Registries that must outlive reload belong on the **`bpy` module** (or another process-global), not in `def` atoms alone.

```clojure
(import bpy)
(setattr bpy "_yardcraft_…" v)
(getattr bpy "_yardcraft_…" default)
(hasattr bpy "_yardcraft_…")
```

Confirmed: `_yardcraft_quirk` / probe attrs survived `(require 'yardcraft.site-suggestions :reload)`.

Yardcraft already uses this pattern in `yardcraft.site-suggestions`:

- `_yardcraft_session_suggestions`
- `_yardcraft_active_suggestion`

## `scene.yardcraft` identity across UI rebuild

`scene.yardcraft` PropertyGroup **Python identity changes** across `(ui/register!)` and unregister/register cycles (`python/id` differed in probe).

After UI rebuild / show paths: **re-fetch** `(.-yardcraft (.-scene (.-context bpy)))` (or `(.-yardcraft (.-scene context))` in ops). Do not stash the props object across register.

## EnumProperty items bake at build time

Enum items for Yardcraft’s `suggestion_id` are fixed when the PropertyGroup is built.

| Step | Observed |
|---|---|
| After `sug/register-suggestion!` alone | Enum item count unchanged; new id absent |
| After `(ui/register!)` | Count grew; new id present |

Therefore: after changing the suggestion registry that feeds enum items, call `(ui/register!)` (or equivalent PropertyGroup rebuild).

Keyword suggestion ids munge to underscore enum identifiers: `:quirk-enum-probe` → `"quirk_enum_probe"`.

## Explicitly NOT claimed (probed, unreproduced)

- `:reload-all` on `yardcraft.site-suggestions` does **not** always RecursionError — returned `:ok` in the probe session. Treat prior RecursionError reports as situational, not invariant.

## Yardcraft UI notes (code-backed, not separate REPL probes)

From `yardcraft.site-ui` / `site_ui.cljc`:

- **Show operator:** on failure, `.report` with `ERROR` and return `#py #{"CANCELLED"}`. Catch-all that always returns `FINISHED` hides failures from the human.
- **Draw sync:** `sync-suggestion-enum-from-active!` updates the dropdown only when `(sug/active-id)` is non-nil — it does **not** force Base when active is nil (preserves staged pre-Show enum selection). `seed-props!` at register is a different path and may set `__base__` when none active.
- **Flaky REPL helpers:** prefer promoting helpers into source + `:reload` over large `in-ns` + `defn-` REPL-only private helpers when the session gets unreliable.
