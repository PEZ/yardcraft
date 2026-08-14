# Extension zip / Basilisp version

Yardcraft installs a **pre-upstream basilisp-blender zip** that already bundles **Basilisp 0.5.1** (not the stock release with an older wheel).

- Release: [PEZ v0.5.0-basilisp-0.5.1](https://github.com/PEZ/basilisp-blender/releases/tag/v0.5.0-basilisp-0.5.1)
- Asset: [basilisp_blender_extension-0.5.0.zip](https://github.com/PEZ/basilisp-blender/releases/download/v0.5.0-basilisp-0.5.1/basilisp_blender_extension-0.5.0.zip)
- Package id: `basilisp_blender_extension`

## Observe first (do not skip)

**Before** download, quit prompts, or `install-file`:

1. **Is it installed / which version?** Prefer CLI (Blender quit or not — `extension list` is fine either way):

```bash
blender --command extension list
# macOS app bundle fallback:
/Applications/Blender.app/Contents/MacOS/Blender --command extension list
```

Look for `basilisp_blender_extension`. Or inspect the user extensions dir, e.g. macOS  
`~/Library/Application Support/Blender/<version>/extensions/user_default/basilisp_blender_extension/`.

If the Yardcraft PEZ package is already present at the expected version → **stop**. Mark progress ✓. Do **not** reinstall.

2. **Is Blender running?** Only matters when you **will** install/upgrade. Check the OS process list. Ask the human to quit **only if Blender is running**. Never ask to quit when it is not.

## Install / upgrade (only when Observe says needed)

**If Blender is running:** ask them to quit completely (plain chat), wait for confirmation, then proceed.

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
