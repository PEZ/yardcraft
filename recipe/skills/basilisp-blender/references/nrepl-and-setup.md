# nREPL and setup (basilisp-blender)

Upstream README: https://github.com/ikappaki/basilisp-blender

## Extension install (Blender ≥ 5.2.0 LTS)

1. Download `basilisp_blender_extension-<version>.zip` from releases
2. `Edit → Preferences → Get Extensions → Install From Disk…`
3. Enable **Basilisp Blender Extension** under Add-ons

CLI variant:

```bash
blender --command extension install-file basilisp_blender_extension-<version>.zip -r user_default -e
```

Older Blender: manual `pip install basilisp-blender` into Blender’s Python (see upstream “Manual Installation”).

After install/update: overlay Basilisp **≥ 0.5** into the extension `.local` site-packages (bundled wheel may be older). See [upgrade-basilisp.md](upgrade-basilisp.md).

## Control panel

Properties editor → **Output** (printer icon) → nREPL panel:

- Start / stop server
- Bind host + port
- Set **Basilisp Project Directory** (this repo root for Yardcraft)

Serving state writes/updates `.nrepl-port` in that directory.

## Editor connect

- **Calva (generic):** Command Palette → *Calva: Connect to a Running REPL Server, in your project* → `basilisp`
- **Yardcraft:** use the **`basilisp-blender`** connect sequence (session key `basilisp-blender`), then confirm `user/init!` (loads via connect sequence) before requiring `yardcraft.*`
- **CIDER:** `cider-connect-clj` → localhost → project:port

Open `basilisp.edn` so Clojure features activate even though the runtime is Basilisp.

## Programmatic start (Python inside Blender)

```python
from basilisp_blender.nrepl import server_start

shutdown_fn = server_start(host="127.0.0.1", port=8889)
# or:
shutdown_fn = server_start(nrepl_port_filepath="/path/to/project/.nrepl-port")
```

Optional `interval_sec` (default `0.2`) controls how often the main-thread timer drains pending nREPL work.

### Basilisp API wrapper

```clojure
(require '[basilisp-blender.bpy-utils :as bb])

(def server
  (bb/nrepl-server-start {:port 0 :interval-sec 0.2}))

;; (:shutdown! server) when done
```

See [api.md](api.md).

## Eval without nREPL

```python
from basilisp_blender.eval import eval_str, eval_file, eval_editor

eval_str("(+ 1 2)")
eval_file("path/to/code.lpy")
eval_editor("<text-block-name>")
```

## Debugging

```python
import logging
from basilisp_blender import log_level_set

log_level_set(logging.DEBUG, filepath="bblender.log")
```
