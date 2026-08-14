---
name: yardcraft-base-design
description: >-
  Layer-2 Yardcraft orchestration for gathering real site facts into a lived-in
  base model after the welcome demo. Use when leaving ensure-demo!, ingesting
  maps/sketches, light-table tracing, national map APIs, promoting confirmed
  measurements into site_data, or starting base design / memoir-style site work.
---

# Yardcraft base design (layer 2 — base sauce)

Orchestrates turning a connected Yardcraft session into a **real-site base**: facts in `yardcraft.site-data`, meshes via `ensure-site!`, human viewport judgment. Composable skills hold procedures — this skill **loads them when needed** and keeps the OODA loop honest.

Load when the **human** engages real-site work (Hello boundary: [hello-conversation.md](../yardcraft-setup/references/hello-conversation.md) **Hello job open until**). README / [`MY-BASE-DESIGN-PROCESS.md`](../../../MY-BASE-DESIGN-PROCESS.md) are **examples / memoir**, not checklists. Paths differ by country, sketches vs APIs, and what Observe finds. Run **Human ⊗ AI ⊗ REPLs** (`bb` + `basilisp-blender`, plus Epupp when map UIs open).

## When to use

- Human is ready (or already mid-flight) on real lot / house / terrace facts
- Maps, sketches, light table, elevation, lot polygons, promoting confirmed measurements

Do **not** auto-load solely because demo finished.

## Prerequisites (composable — do not duplicate their procedures)

Load as the situation needs them:

| Skill | Role |
|---|---|
| **`basilisp`** / **`basilisp-blender`** / **`clojure`** | Dialect, scene REPL, structural edits |
| **`babashka`** | Host HTTP/fs/asset plumbing (`bb` session) |
| **`epupp`** | Browser map UI exploration — **install upstream if missing** when map work opens |
| **`sweden-lantmateriet-min-karta`** | Swedish Min Karta / LM höjd (CRS SWEREF99 + RH00) when that stack applies |
| **`yardcraft-light-table`** | Fitted sketch overlay → trace → capture contours / edges |
| **`yardcraft-site-ui`** | N-panel when touching UI source or re-register after base rebuilds |

Country skills for other nations: discover via Epupp / APIs, then **author** under `recipe/skills/references/<country>-…/` declaring **CRS + vertical datum** up front. **Norway** ([Norgeskart](https://norgeskart.no/)) has no parity skill yet — do not imply Sweden-level packaging; discover and/or write the country skill.

Optional memoir (process flavor, not a checklist): [`MY-BASE-DESIGN-PROCESS.md`](../../../MY-BASE-DESIGN-PROCESS.md).

## Layer-2 OODA

```
λ yardcraft_base_design.
  Observe(facts ∧ scene ∧ maps ∧ sketches ∧ AGENTS_progress ∧ human)
  → Orient(layer_2 ∧ country_stack ∧ light_table_vs_API)
  → Decide(next_confirmed_gap)
  → Act(REPL_visible → ask_human(viewport) → promote_when_happy)
  → update(AGENTS.md phase ∧ progress)
  | ¬invent(measurements)
  | demo → leave_when_ready → clear/ensure-site! once_facts_exist
  | draft-* may_survive clear-site!
  | suggestions_Show/Base ∧ quote_plan ≡ need(real_base) ¬empty_demo
  | load(composable_skills) ¬swallow
```

## Progress

When **entering** or **leaving** layer 2, update `AGENTS.md` Phase / progress so the next Observe sees the gate. Leaving layer 2 means the site is “good enough” base for redesign (suggestions / fly / quote) — human call, not a fixed checklist length.

## Demo → base handoff

1. Leave the welcome demo when the human is ready for real material.
2. Once enough facts exist for massing: `(yardcraft.site/clear-site!)` then `(ensure-site! site)` (or incremental `ensure-*!` + `sync-site-hierarchy!`).
3. `clear-site!` removes scene objects **except** `draft-*` light-table sketches — drafts may survive across rebuilds; use light-table show/hide helpers as needed.
4. Empty / insufficient template facts → thin scene is expected; do not invent lot geometry to “look finished.”

## Invariants

### Do not invent measurements

Ask the human, leave placeholders, or capture from maps/sketches/APIs **then** confirm in the viewport. Promote into `src/yardcraft/site_data.cljc` only when the human is happy. Confirmed facts only.

### Real-site sun / Set time

Demo lounger delight is **demo-safe**. On a real base, Set time / solar aim needs **`:site/lat-deg`** and **`:site/lon-deg`** (and related sun facts) filled — observe geo before promising shadow/lounger behavior.

### Suggestions / quote-plan stay out of empty base

Do **not** run suggestion **Show/Base** (or treat quote-plan as a win) on the empty demo or empty template. Those paths need a **real base**. Layer 3 skills (`yardcraft-design-suggestions`, `yardcraft-quote-plan`, fly-tour-*) load after base is worth presenting.

## Suggested ingredient loops (situational)

Order is Observe-driven — skip what you already have.

1. **National maps** — Sweden: load **`sweden-lantmateriet-min-karta`** + Epupp on Min Karta. Elsewhere: Epupp discover → sample → author country skill when the stack stabilizes.
2. **Host APIs / assets** — Babashka REPL for HTTP/fs; prefer `babashka.http-client` / `babashka.fs` over one-off shell.
3. **Hand light table** — photos or window-aligned sketches under `source-images/` (examples live in `recipe/example-source-images/`); fit via `:sketch/specs`; load **`yardcraft-light-table`** for stage → human trace → capture → promote.
4. **Ground truth** — tape measures, angles, elevation overrides beat raw DEM where the human confirms.
5. **Rebuild** — REPL `ensure-*!` / `ensure-site!` → ask human viewport → promote facts/builders when happy.

## Division of labor

| Role | Owns |
|---|---|
| Agent | Observe gaps; drive Epupp/`bb`/Blender REPLs; stage light table; propose fact updates; mark `AGENTS.md` progress |
| Human | Supplies maps/sketches/measurements; traces in Blender; judges viewport fit; confirms promote |

## Hand off to layer 3

When base facts support redesign exploration, update phase progress and load design/explore skills as needed (`yardcraft-design-suggestions`, `yardcraft-fly-tour-*`, `yardcraft-quote-plan`) — still without folding those procedures into this skill.
