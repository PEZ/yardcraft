# bpy patterns from Basilisp

## Scene queries (orient first)

```clojure
(require '[yardcraft.scene :as scene])

(scene/census)
(scene/object-info "site-sundial-face")
(scene/render-check!)
(scene/render-check! "/tmp/yardcraft-visual-check-sundial.png"
                     {:look-at "site-sundial-face"})
```

Each census record is `{:name :type :parent :location :rotation :scale :hide-viewport? :hide-render? :children}`. `:location` is world XYZ.

Bare `render-check!` is the current scene camera (orbit/fly frame). `{:look-at name}` or `{:look-at [x y z] :distance 8}` frames that part from the same viewing side, then restores the fly/scene camera. Do not add a camera yourself.

For a one-off walk, prefer `(.-objects (.-data bpy))` over `bpy.data/objects` — lint-clean under clj-kondo (see skill cheatsheet).

## Operators and kwargs

Blender ops are keyword-heavy. Always use `**` (Basilisp Python-kwargs syntax; excluded from clj-kondo unresolved-symbol in this project).

```clojure
(.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
(.select-by-type (.-object (.-ops bpy)) ** :type "MESH")
;; (.delete (.-object (.-ops bpy)))  ; destructive — only with human intent
```

## Materials / nodes

```clojure
(defn create-random-material []
  (let [mat (.new (.-materials (.-data bpy)) ** :name "RandomMaterial")
        _   (set! (.-use-nodes mat) true)
        bsdf (aget (.. mat -node-tree -nodes) "Principled BSDF")]
    (set! (-> bsdf .-inputs (aget "Base Color") .-default-value)
          [(rand) (rand) (rand) 1])
    mat))
```

## Geometry creation pattern

1. Ops add primitive → becomes `(.-object (.-context bpy))`
2. Assign material / rename / transform
3. Wrap in a fn taking a data map (`{:location […] :size …}`)

Upstream example: `examples/torus_pattern.lpy` in the basilisp-blender repo — good reference for `**`, `set!`, and `aget` on node inputs. **Do not** paste its top-level `clear-mesh-objects` into a lived-in scene without asking.

## Load-time discipline

```clojure
(defn make-variant! [opts] …)

(comment
  (make-variant! {:name "patio-sketch-a"}))
```

Avoid unconditional wipe/create at namespace load in project files the human will re-eval often.

## Session state that must survive `:reload`

File-level Vars reset on `(require 'ns :reload)` (basilisp skill). For registries that must outlive reload, store on the `bpy` module via `setattr` / `getattr` / `hasattr`. Details: [verified-quirks.md](verified-quirks.md).

## Naming for design exploration

Prefix objects/collections by concern (`patio-`, `park-`, `ref-`) and variant (`-a`, `-b`, `-massing`). Makes REPL filters and human discussion easier.
