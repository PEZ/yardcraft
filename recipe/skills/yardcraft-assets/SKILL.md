---
name: yardcraft-assets
description: >-
  Download and install optional GLB/glTF props for Yardcraft from
  assets/*/ATTRIBUTION.md using Babashka http-client. Use when the user mentions
  assets, GLB download, Quaternius cars, Poly Pizza trees, mailbox prop,
  ATTRIBUTION.md, missing glb, place-cars!, place-trees!, or installing binary
  assets that must not be committed.
---

# Yardcraft assets (ATTRIBUTION → download → skip-if-present)

Optional car / tree / prop binaries are **not** shipped in the template. Each category documents sources and licenses in:

| Path | Contents |
|---|---|
| `assets/cars/ATTRIBUTION.md` | Car packs / models + target filenames |
| `assets/trees/ATTRIBUTION.md` | Tree GLBs + licenses |
| `assets/props/ATTRIBUTION.md` | Mailbox and other props |

Runtime helpers (`place-cars!`, `place-trees!`, `place-mailbox!`, …) **skip** missing files — never crash the site rebuild because a GLB is absent. Those helpers are **examples**; many sites need none of them.

## Prerequisites

Load before using this skill:

1. **`babashka`** — host-side HTTP / fs (`bb` session); prefer over curl/python one-offs
2. **`clojure`** — shared conventions

Blender/`bpy` work stays on **`basilisp-blender`**. Do not download from inside Blender unless the human asks.

## Invariants

- **Never commit binaries** — `.gitignore` should cover `assets/**/*.glb`, `assets/**/*.gltf`, and related packs; only `ATTRIBUTION.md` (and empty dirs) belong in git.
- **Skip if present** — if the target file already exists, do not re-download or overwrite without explicit human ask.
- **Respect licenses** — keep attribution text; CC BY models need credit as stated in ATTRIBUTION.
- **Prefer Babashka** — `babashka.http-client`, `babashka.fs`, `babashka.process` for unpack when needed.

## Workflow

```
λ assets_loop.
  read(ATTRIBUTION.md) → resolve_URL ∧ dest_path → skip_if_exists → http_get → write ∧ unpack
  | ¬commit_binaries
```

1. Read the relevant `assets/<kind>/ATTRIBUTION.md`.
2. For each listed file, decide destination path under that folder (filenames in the doc are authoritative).
3. If the destination already exists → skip.
4. Download with Babashka. Prefer direct file URLs. If the host only exposes a browse page (Poly Pizza, Sketchfab), ask the human for a direct link or download manually into the path — do not scrape login walls.
5. Unpack archives when the attribution implies a bundle; leave a tidy layout matching the paths builders expect (e.g. `assets/cars/suv.glb`, `assets/cars/miata/scene.gltf`).
6. Verify with `babashka.fs` that expected paths exist; then in Blender, rebuild / call the place helper and ask the human to check the viewport.

### Babashka sketch

```clojure
(require '[babashka.http-client :as http]
         '[babashka.fs :as fs])

(defn ensure-asset!
  "Download url to dest unless dest already exists. Returns :skipped | :wrote."
  [url dest]
  (if (fs/exists? dest)
    :skipped
    (do (fs/create-dirs (fs/parent dest))
        (let [{:keys [status body]} (http/get url {:as :bytes})]
          (when-not (<= 200 status 299)
            (throw (ex-info "asset download failed" {:status status :url url})))
          (fs/write-bytes dest body)
          :wrote))))

;; Example — only when you have a direct URL from ATTRIBUTION / human:
;; (ensure-asset! "https://…" "assets/cars/suv.glb")
```

## After install

- Scene: `(require …)` props ns and call the place helper, or full `(ensure-site! site)` when facts reference plantings/cars.
- Ask the human to confirm scale/orientation in the viewport.
- Do not force-add assets the project does not need.
