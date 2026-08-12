# Basilisp verified quirks (Yardcraft session)

Environment where probes ran: **Blender ≥ 5.2.0 LTS**, Basilisp nREPL via basilisp-blender, Calva session key `basilisp-blender`, **Python 3.13** (`sys.version`).

These are REPL-verified facts. Unreproduced folklore (including claims still lingering in `AGENTS.md`) is not authoritative — this skill + probes win.

## Dotted method symbols — analyze reject

`(sys/path.insert 0 "…")` fails at analyze:

```text
symbol names may not contain the '.' operator
```

Working form:

```clojure
(import sys)
(.insert (.-path sys) 0 "…")
;; restore:
(.remove (.-path sys) "…")
```

Same rule for any `module/attr.method` style — use property/method interop forms.

## Nested Python attrs — prefer interop forms

Prefer `(.-path sys)`, `(.insert …)`, chained `.-` / `.` over dotted slash names for nested host attributes. Slash forms work for top-level module members (`math/cos`); nested dots in the symbol name hit the analyzer rule above.

## `:reload` and Var roots

| Kind | After `(require 'ns :reload)` |
|---|---|
| File-defined `def` / `defn` | Roots reinitialize from source (REPL `alter-var-root` mutations lost) |
| Interned-only Vars (not in source) | Mutated roots **can** survive |

Confirmed: mutate `yardcraft.site-suggestions/survey-ns-denylist` via `alter-var-root`, then `:reload` → file value restored (sentinel lost).

Do not assume every Var resets; assume **file defs reset**.

## Private Vars

Resolving `other-ns/private-sym` from another namespace fails at analyze (`cannot resolve private Var`). Same discipline as Clojure visibility.

## Explicitly NOT claimed (probed, unreproduced)

- Trailing `:reload` does **not** drop `:as` aliases. `(require '[basilisp.string :as strx] :reload)` kept `strx` resolvable (`strx/join` worked).
