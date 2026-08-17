---
name: basilisp
description: >-
  Basilisp (Clojure-compatible Lisp on Python 3) development: .lpy source,
  basilisp.edn projects, Python interop, dialect differences from Clojure, REPL
  and nREPL workflows. Use when writing or reviewing Basilisp, editing .lpy
  files, bootstrapping Basilisp in Python, or when the user mentions Basilisp,
  basilisp.edn, or Clojure-on-Python.
---

# Basilisp

Basilisp is a [Clojure-compatible(-ish)](https://docs.basilisp.org/en/latest/differencesfromclojure.html) Lisp hosted on Python 3. It compiles to Python and runs on the CPython VM — seamless interop with the Python ecosystem.

Official docs: https://docs.basilisp.org/en/latest/

## When to use

- `.lpy` files, `basilisp.edn`, or `basilisp` CLI / nREPL
- Python libraries called from Lisp (including Blender’s `bpy`)
- Clarifying “is this Clojure or Basilisp?” before coding

**Treat Basilisp as Clojure.** Always load the **`clojure`** skill — same conventions and workflow (REPL-first, structural edits, data orientation). This skill adds **host/dialect** concerns only. The differences below are **exceptions**, not a reason to skip Clojure conventions.

## Differences from Clojure

Official: [Differences from Clojure](https://docs.basilisp.org/en/latest/differencesfromclojure.html) (also in [basilisp-lang/basilisp](https://github.com/basilisp-lang/basilisp) docs). Fuller agent digest: [references/differences-from-clojure.md](references/differences-from-clojure.md).

Agent-relevant highlights:

- Hosted on **Python 3.10+** (not JVM) — Python interop, not Java
- `=` uses Python optimistic numeric equality (`int`/`float` may be equal); `==` aliases `=`
- No refs/STM; no agents — atoms yes; `binding` yes
- Non-dynamic Vars may be direct-linked → `alter-var-root` may not be visible unless `^:redef` or dynamic
- Regex = Python `re`; characters = single-char strings; `#py` for native Python collections
- No `locking` / `monitor-*`; has `await` / `yield`; `def` ignores `^:const`
- ns: `:refer-basilisp` ↔ `:refer-clojure`; no prefix lists on import/require; missing `clojure.*` auto-aliases `basilisp.*` **on `require`**, not on a bare `clojure.string/includes?`
- Core libs under `basilisp.*`; Clojure libs support planned; sorted maps/sets/array maps not implemented
- `python/` builtins namespace; `new` is a compatibility macro; no `Classname/new`
- JIT form-at-a-time compilation (Clojure-like, not CLJS whole-program)

## Identity

```
λ basilisp.
  dialect ≡ Clojure_compatible_ish ∧ hosted_on(Python_3.10+)
  | treat_as ≡ Clojure → MUST_load(skill `clojure`)
  | differences ≡ exceptions (see official Differences from Clojure)
  | source_ext ≡ .lpy
  | project_marker ≡ basilisp.edn
  | interop ≡ first_class (import ∧ .method ∧ .-prop ∧ ** kwargs)
  | REPL_first → verify → then_edit
```

## Dialect detection

| Signal | Meaning |
|---|---|
| `basilisp.edn` | Basilisp project root |
| `.lpy` sources | Basilisp namespaces |
| nREPL + Calva “basilisp” connect | Basilisp runtime |
| `bpy` / Blender | Likely also **basilisp-blender** — load that skill too |

`require` of a missing `clojure.*` lib auto-aliases the matching `basilisp.*` ns. A bare `clojure.string/includes?` (no prior `require`) fails at analyze.

## Invariants

- Basilisp ≡ Clojure for agent workflow → load **`clojure`** skill; apply its conventions
- Prefer idiomatic Basilisp; drop to Python interop at the edges
- Kebab-case in Lisp; compiler munges to `snake_case` for Python identifiers
- Python kwargs to host callables: use `**` then keyword/value pairs
- `#py […]` / `#py {…}` when you need raw Python collections
- No agents; no STM/refs — atoms and ordinary Python concurrency only
- `.lpy` form edits: structural editing (same rule as Clojure) — never bracket-corrupt with plain text patches

## Python interop (essentials)

```clojure
(ns my.ns
  (:import [os.path :as path]
           math))

(path/exists "test.txt")
(math/cos 0.5)

;; methods / props
(.strftime some-datetime "%Y-%m-%d")
(.-year some-datetime)
(set! (.-use-nodes mat) true)

;; keyword args to Python
(python/open "test.txt" ** :mode "w")
```

Builtins live under `python/` (e.g. `python/abs`) with no import.

## Project layout (typical)

```
.
├── basilisp.edn
├── pyproject.toml          ; or requirements / poetry / uv
├── src/myproject/core.lpy
└── tests/myproject/test_core.lpy
```

Minimal editor-oriented roots (e.g. Blender project dirs) may be flatter: `basilisp.edn` + `*.lpy` + `.nrepl-port`.

## Workflow

1. Confirm runtime (REPL / nREPL connected).
2. Probe host APIs in the REPL before committing file edits.
3. Keep side-effecting host calls in small named fns; explore from `(comment …)`.
4. **Blender / scene work:** make the change visible via REPL first → ask the human to check the viewport → promote to files only when they are happy. Until then, keep experiments in the session / RCF.
5. Apply durable file changes structurally; re-load / re-eval to verify.

## Verified quirks

Probed on **Blender ≥ 5.2.0 LTS** / Basilisp nREPL (Calva `basilisp-blender`) / **Python 3.13**.

- **`(require '[ns :as alias] :reload)` does not bind `:as`** — use two-step `(require 'ns :reload)` then `(require '[ns :as alias])`. Existing aliases can look fine after the combined form (false negative); probe with a fresh alias name.
- **Bare `clojure.string/includes?` fails at analyze** (`unable to resolve symbol`) until that ns is required. Auto-alias `clojure.*` → `basilisp.*` is on `require`, not first mention. `(require '[clojure.string :as string])` then `string/includes?` (or `'[basilisp.string :as string]`). After require, the qualified `clojure.string/includes?` also resolves.
- **Dotted method symbols** rejected at analyze: `(sys/path.insert 0 "…")` → `symbol names may not contain the '.' operator`. Use `(.insert (.-path sys) 0 "…")`.
- **File-defined Vars reinitialize on `(require 'ns :reload)`** — `alter-var-root` mutations lost. Interned-only Vars (not in source) can keep mutated roots; assume **file defs reset**.
- **Private Vars:** `other-ns/private-sym` fails at analyze (`cannot resolve private Var`).
- Prefer `(.-…)` / `(.…)` interop for nested Python attrs over dotted slash symbols.

Depth + explicit non-claims: [references/verified-quirks.md](references/verified-quirks.md).

## Progressive disclosure

Load only what the task needs:

| Reference | Load when |
|---|---|
| [references/python-interop.md](references/python-interop.md) | Calling Python modules, classes, kwargs, `#py`, iterators |
| [references/differences-from-clojure.md](references/differences-from-clojure.md) | Fuller digest of official Differences from Clojure |
| [references/projects-and-tooling.md](references/projects-and-tooling.md) | Bootstrapping, CLI, testing, packaging |
| [references/verified-quirks.md](references/verified-quirks.md) | Reload/Var roots, `:as`+`:reload`, private Vars, dotted-method analyze rejects, bare `clojure.string/…` |

## See also

- **basilisp-blender** skill — nREPL inside Blender, `bpy` patterns
- [Getting Started](https://docs.basilisp.org/en/latest/gettingstarted.html)
- [Python Interop](https://docs.basilisp.org/en/latest/pyinterop.html)
- [Differences from Clojure](https://docs.basilisp.org/en/latest/differencesfromclojure.html)
