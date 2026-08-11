# basilisp-blender API (digest)

Upstream: https://github.com/ikappaki/basilisp-blender/blob/main/API.md

## `basilisp-blender.bpy-utils/nrepl-server-start`

```clojure
(nrepl-server-start
  {:keys [host port nrepl-port-dir interval-sec]
   :or {port 0 interval-sec 0.2}
   :as opts})
```

Starts nREPL in async mode; pending client work runs on a `bpy` timer.

| Opt | Meaning |
|---|---|
| `:host` | Bind address (default `127.0.0.1` if empty/omitted) |
| `:port` | Port; `0` = ephemeral |
| `:nrepl-port-dir` | Directory for `.nrepl-port` (default cwd) |
| `:interval-sec` | Timer period for draining eval queue |

Returns a map:

| Key | Meaning |
|---|---|
| `:error` | Error message if start failed |
| `:host` / `:port` | Bound address |
| `:nrepl-port-file` | Path to port file |
| `:shutdown!` | Zero-arg fn — stop server + timer |

## `basilisp-blender.utils/class-make*`

Macro: create a Python class from Basilisp.

```clojure
(class-make* class-name class-and-interfaces fields & fns)
```

- Each field needs `:default` and/or `:tag` metadata
- Inside methods, fields are callable as `(-field)`
- `self` bound in method bodies
- Python kwargs on methods: `^{:kwargs :collect}` (see Basilisp `:kwargs` docs)

Use when Blender or Python APIs require real subclasses/callbacks; otherwise prefer plain fns + `bpy` data.
