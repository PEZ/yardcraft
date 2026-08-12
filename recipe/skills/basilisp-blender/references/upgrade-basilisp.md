# Upgrade Basilisp past the bundled wheel

basilisp-blender ships an older Basilisp wheel (observed **0.4.0**). Yardcraft needs **≥ 0.5.0** (verified **0.5.1** on Blender ≥ 5.2.0 LTS / Python 3.13) so Calva **load-file** and module aliases work.

## Why

[Basilisp #1302](https://github.com/basilisp-lang/basilisp/issues/1302) breaks Calva load-file / host ns aliases (`basilisp_core` and similar missing). Symptoms in Yardcraft: flaky UI / sun / fly after load-file, or missing aliases when requiring modules.

This is an **install overlay** into the extension’s `.local` site-packages — not a source patch of [ikappaki/basilisp-blender](https://github.com/ikappaki/basilisp-blender). The extension still bundles the old wheel; pip into `.local` overlays it (takes precedence for that Blender Python).

## When

- Fresh basilisp-blender extension install or update
- `(importlib.metadata/version "basilisp")` reports `< 0.5.0`
- Calva load-file / require alias bugs consistent with #1302

## Procedure

Paths depend on Blender version + embedded Python. Example for **Blender ≥ 5.2.0 LTS** / **Python 3.13** on macOS:

```bash
TARGET="/Users/<you>/Library/Application Support/Blender/5.2/extensions/.local/lib/python3.13/site-packages"
PY="/Applications/Blender.app/Contents/Resources/5.2/python/bin/python3.13"

# 1) Upgrade into the extension .local site-packages (not Homebrew / system Python)
"$PY" -m pip install --upgrade "basilisp>=0.5.0" --target "$TARGET"

# 2) Optional: remove stale dist-info for the old bundled version
# rm -rf "$TARGET/basilisp-0.4.0.dist-info"

# 3) Clear stale Basilisp bytecode — REQUIRED (else nREPL panel may fail to register: Var_31/Var_32)
find "$TARGET" -type f -name '*.lpyc' -delete
# also project caches if any:
# find /path/to/yardcraft -type f -name '*.lpyc' -delete

# 4) Fully quit + restart Blender, start nREPL (Properties → Output → Basilisp nREPL), reconnect Calva
```

## Verify (REPL after restart)

```clojure
(import importlib.metadata)
(importlib.metadata/version "basilisp")  ; expect >= 0.5.0
```

## Pitfalls

| Pitfall | Effect |
|---|---|
| pip via Homebrew / wrong Python | Wrong env; Blender still runs the old wheel |
| Skip `*.lpyc` clear | nREPL panel may disappear / fail to register |
| Skip full Blender restart after pip | Stale runtime still loaded |
| Patching the extension repo instead of `.local` | Unnecessary; overlay is enough |

Re-run this overlay after a fresh extension install if the bundled Basilisp is still `< 0.5`.
