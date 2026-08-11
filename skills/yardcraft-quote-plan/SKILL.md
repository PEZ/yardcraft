---
name: yardcraft-quote-plan
description: >-
  Create contractor quote-plan SVG drawings from Yardcraft site facts via
  yardcraft.site-plan / write-quote-plan!. Use when the user mentions quote plan,
  quote-plan.svg, dimensioned top-down SVG, contractor plan drawing, site-plan,
  show-angles, or exporting a plan that is not a Blender screenshot.
---

# Yardcraft quote plan (facts → SVG)

Produce a **contractor-oriented, dimensioned top-down SVG** from the facts map — not a Blender screenshot. Renderer: `src/yardcraft/site_plan.cljc` (`yardcraft.site-plan`).

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL session (facts/`effective-site` live here)
3. **`clojure`** — shared Clojure conventions

Related: **`yardcraft-design-suggestions`** when the plan should reflect an active or pinned suggestion.

## When to write

- After facts harden (terrace / driveway / canopy dimensions the contractor needs)
- After `show!` of a design option you want on paper
- After iterating the SVG renderer itself

Empty / insufficient facts: `write-quote-plan!` may produce a thin or empty drawing — do not invent measurements to “fill” the plan.

## Primary API

```clojure
(require '[yardcraft.site-plan :as plan])
(require '[yardcraft.site-suggestions :as sug])

;; Active suggestion (if any) merged onto base:
(plan/write-quote-plan! (sug/effective-site site))

;; Pin a suggestion without relying on active UI state:
(plan/write-quote-plan! (sug/effective-site site :my-idea))

;; Custom path + opts
(plan/write-quote-plan! (sug/effective-site site)
                        "out/quote-plan.svg"
                        {:show-angles? true})
```

| Arg / opt | Role |
|---|---|
| `s` | Facts map (prefer `effective-site` so design overlays appear) |
| `file-path` | Default `out/quote-plan.svg` (parent dirs created) |
| `:show-angles?` | Include angle labels (default `false`) |

Return map includes `:path`, `:areas`, `:bytes` (confirm in live docstring).

## Workflow

```
λ quote_plan_loop.
  facts ∨ show! → write-quote-plan! → human_open_SVG → iterate_renderer_∨_facts
  | ¬Blender_screenshot
```

1. Ensure the facts (or suggestion patch) you want drawn are in session.
2. Call `write-quote-plan!` with `(sug/effective-site site)` or a pinned id.
3. Ask the human to open the SVG and judge dimensions / labels / clutter.
4. If the drawing needs work: iterate `site_plan.cljc` (structural edits) and re-run write — do not screenshot the viewport as a substitute.
5. Promote durable renderer or facts changes only when the human is happy.

## Invariants

- **Facts drive geometry** — the SVG is a projection of `site` / effective-site, not mesh world transforms.
- **Suggestions:** use `effective-site` so quote matches what Show displays.
- **Re-run after edits** — stale SVG after `show!` / `show-base!` / `site_data` changes is expected until rewrite.
- **Do not invent site measurements** to make the plan look complete.
