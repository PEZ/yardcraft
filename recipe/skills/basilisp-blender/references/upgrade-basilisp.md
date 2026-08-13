# Extension zip / Basilisp version

Yardcraft installs a **pre-upstream basilisp-blender zip** that already bundles **Basilisp 0.5.1** (not the stock release with an older wheel).

- Release: [PEZ v0.5.0-basilisp-0.5.1](https://github.com/PEZ/basilisp-blender/releases/tag/v0.5.0-basilisp-0.5.1)
- Asset: [basilisp_blender_extension-0.5.0.zip](https://github.com/PEZ/basilisp-blender/releases/download/v0.5.0-basilisp-0.5.1/basilisp_blender_extension-0.5.0.zip)

## Install (agents / do-mode — CLI first)

**Quit Blender completely before running `extension install-file`.**

1. Download the PEZ asset (Babashka `bb` / `babashka.http-client`, or any HTTP tool).
2. Install and enable via Blender's extension CLI (`-r user_default` = user extensions repo; `-e` = enable after install):

```bash
blender --command extension install-file /path/to/basilisp_blender_extension-0.5.0.zip -r user_default -e
```

Upstream docs: [ikappaki/basilisp-blender](https://github.com/ikappaki/basilisp-blender).

### Finding `blender`

- Prefer `command -v blender` / `which blender` when Blender is on `PATH` (Linux packages, some macOS installs).
- **macOS app bundle fallback** when `blender` is not on `PATH`:

```bash
/Applications/Blender.app/Contents/MacOS/Blender --command extension install-file /path/to/basilisp_blender_extension-0.5.0.zip -r user_default -e
```

Adjust the `.app` path if Blender lives elsewhere.

### Fallback: Install From Disk

If the CLI is missing, points at the wrong Blender build, or `install-file` fails: **Edit → Preferences → Get Extensions → Install From Disk…**, select the zip, then enable **Basilisp Blender Extension**. See [nrepl-and-setup.md](nrepl-and-setup.md) for panel and connect steps.

## Why

The PEZ zip bundles **Basilisp ≥ 0.5.1**, fixing [Basilisp #1302](https://github.com/basilisp-lang/basilisp/issues/1302) (Calva load-file and module aliases break when Basilisp is below 0.5).

## Temporary

Upstream fix tracked in [ikappaki/basilisp-blender#14](https://github.com/ikappaki/basilisp-blender/pull/14). After that merges and releases, switch to upstream zips and this note can shrink or go away.

## Verify (optional, REPL after install)

```clojure
(import importlib.metadata)
(importlib.metadata/version "basilisp")  ; expect >= 0.5.1
```
