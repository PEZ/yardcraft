---
name: yardcraft-design-suggestions
description: >-
  Design suggestion overlays for comparing patio/parking/furniture layout options
  in the Yardcraft Blender site model. Use when the user mentions design suggestion,
  suggestion overlay, show suggestion, register suggestion, session suggestion,
  N-panel Show/Base, promote suggestion, site-suggestions, or compare layout options.
  Primary path: REPL register → UI → human Show/Base iterate → EDN commit when approved.
---

# Yardcraft design suggestions (REPL → UI → approve → file)

Explore alternate patio/parking/furniture designs. **Author in the REPL/session first**; write durable EDN under `src/yardcraft/suggestions/` only after the human approves via the Yardcraft N-panel. Keep session `site` as pure base; promote into `site_data` only when adopting a winner as survey base (separate from EDN durability).

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL-in-Blender, `bpy`, Yardcraft session bootstrap (**Safe visual self-check** / `render_check` lives here)
3. **`clojure`** — shared Clojure conventions (structural edits, REPL-first)

After connect: confirm `user/init!` added `src/` to `sys.path` before requiring `yardcraft.*`. Tooling: `src/yardcraft/site_suggestions.cljc` (`yardcraft.site-suggestions`, alias `sug`); facts in `yardcraft.site-data`; orchestration in `yardcraft.site`. Panel: load **`yardcraft-site-ui`** when touching N-panel source.

## Division of labor

| Role | Owns |
|---|---|
| Agent | Session register → `ui/register!` → **`show!` + self-verify** → **`show-base!` + self-verify** → then ask human N-panel Show/Base; iterate until happy; write EDN only after approval |
| Human | Selects suggestion in Yardcraft N-panel dropdown, clicks **Show** / **Base**; judges viewport; confirms file commit and any promote into `site_data` |

```
λ suggestion_authoring.
  REPL_register_map → ui/register!
  → show! → render_check(inspect_image) → fix_if_wrong
  → show-base! → render_check(inspect_image) → fix_if_wrong
  → human(N-panel Show/Base) → iterate_until_approve
  | then_commit(EDN ∧ builder_support_if_needed)
  | promote_plan → site_data only_when_adopting_as_base
  | ¬write_EDN ¬edit_site_data_early
  | ¬ask_human_before_self_verify
```

**Self-verify is mandatory.** Do not hand the human a suggestion you have not applied and visually checked. Use **`basilisp-blender` → Safe visual self-check** (temp PNG via scene REPL, read the image, restore camera/render/active state). Check **Show** *and* **Base** (same camera/frame) so restore is proven too. Execution success ≠ visual correctness.

## Primary authoring process

1. **Create in the REPL** — suggestion map (same shape as EDN) → `(sug/register-suggestion! …)` (session registry; no disk write).
2. **Add to the UI** — `(ui/register!)` or `(ui/reload!)` so EnumProperty items rebuild (baked at PropertyGroup class build).
3. **Self-verify** — `(sug/show! …)` then render_check; `(sug/show-base! …)` then render_check. Fix obvious mismatches before involving the human.
4. **Iterate with the human** — they **select** in the dropdown (staged only), then click **Show** / **Base**. Selecting alone does not apply.
5. **Commit to files only when approved** — write `src/yardcraft/suggestions/<snake_id>.edn`; keep any builder + default facts keys the patch needs. Optionally `(sug/promote-plan :id)` **only** if adopting into survey base (`site_data`) — that is separate from durable suggestion EDN.

Builder support for new patch keys may be required before Show works; experiment in REPL/session until happy.

### RCF — full loop

```clojure
(require 'yardcraft.site-suggestions :reload)
(require '[yardcraft.site-suggestions :as sug])
(require '[yardcraft.site-ui :as ui])

(sug/register-suggestion!
 {:suggestion/id :my-idea
  :suggestion/title "My idea"
  :suggestion/note "Design option — session only."
  :suggestion/domains #{:terrace}
  :suggestion/patch {:terrace/depth-m 5.2}})
(ui/register!)

(sug/show! site :my-idea)
;; render_check + inspect image — fix if wrong
(sug/show-base! site)
;; render_check again — Base OK?
;; Then ask human: N-panel → Show / Base

;; After human approval — durable EDN:
;; write src/yardcraft/suggestions/my_idea.edn
```

- **Resolve:** `load-suggestion` — session registry first, then `src/yardcraft/suggestions/<id>.edn`.
- **List:** `list-suggestions` merges session + files; session wins on same id; `:source :session` or `:file`.
- **N-panel:** session items get a ` (session)` label suffix.

## Show → Compare → Base → Promote

### RCF contract (existing / file suggestions)

```clojure
(require 'yardcraft.site-suggestions :reload)
(require '[yardcraft.site-suggestions :as sug])
(sug/list-suggestions)
(sug/show! site :my-idea)
(sug/show! site :other-idea)   ; A→B switch inside show!
(sug/show-base! site)
(sug/promote-plan :my-idea)
(sug/set-base! site :my-idea) ; adopt as session site (not file)
;; Reset session site to file facts, then rebuild:
(sug/reload-file-base!) ; reassembles from site-*-facts; undoes set-base!
(ensure-site! site)
```

- **Basilisp reload:** prefer trailing `:reload-all` when reloading namespaces that need Var restore (e.g. `site-data`). `(require '[ns :as alias] :reload)` does not bind `:as` — use the two-step require above. `:reload-all` can RecursionError if there is a cycle with `yardcraft.site`.
- If still stale after structural edits: `(load-file "src/yardcraft/site_suggestions.cljc")`.

The View3D **Yardcraft** N-panel shares `show!` / `show-base!`; select stages, **Show** applies, **Base** restores. Promotion and `set-base!` remain RCF-only.

### Authoring EDN (approval commit)

After session iteration is approved, files live under `src/yardcraft/suggestions/` (snake_case filename ↔ kebab id). Required keys:

| Key | Role |
|---|---|
| `:suggestion/id` | Keyword id (e.g. `:my-idea`) |
| `:suggestion/title` | Short human label |
| `:suggestion/note` | Design option — not a survey fact |
| `:suggestion/domains` | Set of rebuild domains (`#{:furniture}`, `#{:terrace :furniture}`, …) |
| `:suggestion/patch` | Facts overlay (deep-merged onto base) |

**Patch-first.** Prefer EDN patches. `.cljc` with `:suggestion/apply` is future — only when a structural transform needs code.

v1 domains: `:furniture`, `:terrace` (roof coupled with terrace), `:demo` (welcome-demo overlays via `ensure-demo-overlays!`), `:full` (escape hatch). Unknown domain → fail loud. Lot/terrain/house → full rebuild escape hatch only.

**Durable suggestion EDN** = design option on disk. **`promote-plan` → `site_data`** = adopting that option as survey/base facts — different gate; do not conflate.

### Promote ritual (into survey base)

1. Design already approved via N-panel Show/Base (and EDN committed if it should persist as an option).
2. `(sug/promote-plan :id)` — read-only: `{:changes … :replacements …}`. `:changes` = short per-key summary; `:replacements` = facts-var symbol → **full** deep-merged facts map.
3. Human confirms adopting as base.
4. Only then: copy `:replacements` into matching `site-*-facts` defs in `src/yardcraft/site_data.cljc`.
5. Reload site namespaces; `(sug/show-base! site)` against the new base; re-register UI if panel source changed.

Promote never silently rewrites `site_data`. No suggestion code path writes that file.

## Invariants

- **Self-verify before asking the human** — `show!` + render_check, then `show-base!` + render_check (`basilisp-blender` Safe visual self-check).
- **Session registry + active suggestion persist on `bpy` attrs** (`_yardcraft_session_suggestions`, `_yardcraft_active_suggestion`) — survive Basilisp ns reload. Not Vars / `session-suggestions*` / `alter-var-root`.
- **Session registry is memory-only** — `register-suggestion!` / `unregister-suggestion!` / `clear-session-suggestions!` never write EDN; after any of those, `(ui/register!)` (or `reload!`) so the enum rebuilds.
- **`show!` does not persist.** Session `site` Var stays file base; effective is builders-only.
- **`set-base!` persists effective as session base** (`persist-site!` on the referred `site` Var; `site_data` file untouched).
- **File reset:** `(require '[yardcraft.site-data :as data] :reload-all)` restores session `site`; `reload-file-base!` additionally clears active suggestion, merges `site-*-facts`, and `persist-site!`s — then `ensure-site!`.
- **Deep-merge for nested patches.** Map-only recursive merge; vectors/scalars replace. Shallow `merge` drops sibling furniture keys.
- **`sync-site-hierarchy!` after partials** — owned by `show!` / `show-base!` (agent need not call sync separately for suggestion switches).
- **Survey-key denylist** on apply/promote (`:lot/polygon-xy`, `:terrain/*`, `:house/size-m`, road traces, …) unless explicit opt-in.
- **A→B teardown inside `show!`.** Union domains → teardown → rebuild B from effective, A−B from base — no ghost meshes.
- **Promote is read-only plan + structural edit.** `promote-plan` returns `:changes` + `:replacements` only; agent (or human) pastes into `site_data` after confirmation.
