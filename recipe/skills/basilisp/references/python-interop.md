# Basilisp Python Interop

Source of truth: https://docs.basilisp.org/en/latest/pyinterop.html

## Name munging

Lisp `kebab-case` → Python `snake_case`. You may write kebab-case when calling Python; the compiler munges.

## Imports

Prefer `:import` on `ns`:

```clojure
(ns my.ns
  (:import bpy
           math
           [os.path :as path]))
```

Top-level members: `module/member`. Submodules keep dotted module names unless aliased.

`:refer` / `:refer :all` exist; prefer aliases — referred Python names lose to Basilisp/`basilisp.core` bindings on conflict (e.g. referred `time` vs `basilisp.core/time`).

## Classes vs instances

Module-level: `src.boo/BooClass`, `src.boo/module-method`.

Class members use a dotted class in the ns position: `src.boo.BooClass/class-var`, `(src.boo.BooClass/some-class-method)`.

Think “Python attribute path, last `.` written as `/`”.

## Methods and properties

```clojure
(. object method arg1)
(.method object arg1)          ; compile-time known method
(.- object property)
(.-property object)
(set! (.-property object) v)   ; mutable host props
```

Qualified methods (Clojure 1.12-style) work for instance/static/class without Java’s leading-`.` restrictions: `(python.str/split "a b c")`.

**Nested attrs:** symbols like `sys/path.insert` fail at analyze (`symbol names may not contain the '.' operator`). Use `(.insert (.-path sys) 0 "…")` instead. See [verified-quirks.md](verified-quirks.md).

`new` is optional sugar — call the class: `(src.boo/BooClass)`.

## Keyword arguments (`**`)

```clojure
(python/open "test.txt" ** :mode "w")
(.primitive-torus-add bpy.ops/mesh **
                      :major-radius 2
                      :location [0 0 0])
```

`**` is compiler syntax (not a value). Keys may be keywords or strings; both are munged to Python identifiers.

### Basilisp fns that accept Python kwargs

Rare (callbacks). Metadata on `fn`/`defn`:

- `^{:kwargs :apply}` — kwargs become keyword rest args
- `^{:kwargs :collect}` — kwargs become a final map arg

Multi-arity does not support `:kwargs`.

## Python collections and literals

- `#py [1 2 3]` → Python `list`
- `#py {…}` → Python `dict` (as documented for `#py` data readers)
- `aget` / `aset` for item access; `aslice` for Python slicing

## Iterators

Re-iterable Python iterables seq successfully. **Single-use** iterators (generators) must be wrapped with `iterator-seq` before multiple passes (`count` then `first` would otherwise share one exhausted iterator; Basilisp errors rather than silent wrong answers).

## Builtins and tagging

- Builtins: `python/abs`, `python/str`, …
- `^python/str` tags pass through to Python annotations; Basilisp does not use them for compilation decisions today

## Decorators

`:decorators` metadata on `defn`/`fn` applies Python-style wrappers (right-to-left). Prefer ordinary higher-order composition unless matching a Python API.
