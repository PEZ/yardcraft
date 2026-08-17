# Differences from Clojure (agent digest)

Source of truth: https://docs.basilisp.org/en/latest/differencesfromclojure.html  
Repo/docs: https://github.com/basilisp-lang/basilisp

Agent digest of agent-relevant deltas — not a full copy. Prefer the official page when unsure.

## Host

- Runs on **Python 3.10+**, not the JVM
- Host interop is Python (import / methods / props / kwargs), not Java
- Basilisp and Python can import each other once the runtime/hooks are initialized

## Types and equality

- `nil` ↔ `None`
- No distinct `short`/`int`/`long` or `float`/`double` widths
- `=` follows Python optimistic numeric equality (`int` vs `float` may be equal); `==` aliases `=`
- Characters are single-char strings (no distinct char type)
- `#py` reader prefix yields native Python data structures
- Complex literals with `J`; `M` suffix → `decimal.Decimal`; ratios → `fractions.Fraction`
- Sorted sets, sorted maps, and array maps are **not implemented** (tracked upstream)

## Concurrency

- Atoms: yes
- Refs / STM: **no**
- Agents: **no** (tracked upstream)
- Dynamic Vars / `binding`: yes
- Non-dynamic Vars may be direct-linked: `alter-var-root` updates may not be visible to call sites unless the Var is `^:redef` or dynamic

## Special forms / host extras

- No `locking` / `monitor-enter` / `monitor-exit`
- Python-oriented `await` and `yield` special forms exist
- `def` ignores `^:const`

## Namespaces

- `:refer-basilisp` and `:refer-clojure` both work
- No prefix lists on import/require selectors
- Missing `clojure.*` lib auto-maps to `basilisp.*` **on `require`** (the `clojure.*` name becomes an alias). A bare `clojure.string/includes?` without that require fails at analyze.
- Each ns corresponds to a Python module (rarely needs attention)

## Core / libs

- Ports live under `basilisp.*` (`basilisp.string`, `basilisp.set`, `basilisp.test`, …)
- Regex engine is Python `re`
- Clojure libs support is planned, not assumed
- `int` / `float` on strings parse as numbers (unlike Clojure char/string quirks)

## Host interop

- Builtins live under `python/` (e.g. `python/abs`) with no import
- `new` is a compatibility macro (not required for Python construction)
- No `Classname/new` (Clojure 1.12 form) — `new` is a valid Python method name
- Type hints / `:tag` may land on Python AST but are not required; compiler does not use them for optimization today

## Compilation mental model

Closer to Clojure’s JIT feel than ClojureScript’s whole-program compile: form-at-a-time → Python, macros available immediately. No `gen-class`; `gen-interface` exists; dynamic classes via Python `type` are fine. No locals clearing.
