# Basilisp projects and tooling

Source of truth: https://docs.basilisp.org/en/latest/gettingstarted.html

## Install / REPL

```bash
pip install basilisp
basilisp repl
```

Optional extras: `pygments` (REPL highlighting), `pytest` (testing).

## Project template

```bash
lein new org.basilisp/basilisp myproject
```

Typical layout: `src/` and `tests/`, namespaces as folders + `.lpy` files. Dependency tooling is whatever Python uses (`pip`, `uv`, Poetry, …).

## Bootstrapping (embedding in Python)

- CLI-style entry: `basilisp.main.bootstrap("project.core:main")`
- Library/framework init: `basilisp.main.init()` then import namespaces
- Shebang: `#!/usr/bin/env basilisp-run`
- Optional site bootstrap: `basilisp bootstrap` (`.pth` — first run can be slow while `basilisp.core` compiles)

Inside Blender via basilisp-blender, the extension/nREPL path bootstraps for you — you rarely call `init` manually.

## Editor / nREPL

Basilisp ships nREPL support (`basilisp.contrib.nrepl-server`). Calva and CIDER can connect; Calva has an explicit **basilisp** connect path that reads `.nrepl-port`.

## Testing

`basilisp.test` ports `clojure.test`. Pytest integration is documented under Testing in the official docs — follow that when adding a formal suite.
