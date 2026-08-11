(ns yardcraft.site-draw
  "Draw-mode visibility and site-root helpers for the contour light-table workflow.

  Domain helpers take facts map `s`. Orchestration (ensure/show) stays in yardcraft.site."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-hierarchy :as hierarchy]
            [yardcraft.site-house :as house])
  (:import bpy mathutils))

(def ^:private draw-structure-prefixes
  ["site-house" "site-bod" "site-driveway" "site-tree" "site-mailbox" "site-door" "site-bay"
   "site-veranda" "site-terrace" "site-stair" "site-sundial" "site-road" "site-north"
   "site-furniture"])

(defn- set-object-hide!
  [obj hide?]
  (set! (.-hide_viewport obj) hide?)
  (set! (.-hide_render obj) hide?))

(defn- set-hide-names!
  [names hide?]
  (let [touched (atom [])]
    (doseq [n names]
      (when-let [obj (mesh/object-by-name n)]
        (set-object-hide! obj hide?)
        (swap! touched conj n)))
    {(if hide? :hidden :shown) @touched}))

(defn- set-hide-matching!
  [prefixes hide?]
  (let [names (atom [])]
    (doseq [obj bpy.data/objects]
      (when (some #(string/starts-with? (.-name obj) %) prefixes)
        (set-object-hide! obj hide?)
        (swap! names conj (.-name obj))))
    {(if hide? :hidden :shown) @names}))

(defn hide-draw-structures!
  "Hide reference massing used while tracing contours."
  [_s]
  (set-hide-matching! draw-structure-prefixes true))

(defn unhide-draw-structures!
  "Unhide reference massing (does not build missing structures)."
  [_s]
  (set-hide-matching! draw-structure-prefixes false))

(defn- contour-object-names
  []
  (filterv #(string/starts-with? % "site-contour-")
           (mesh/all-object-names)))

(defn- set-terrain-and-contours-hide!
  [hide?]
  (merge (set-hide-names! ["site-terrain"] hide?)
         {:contours (set-hide-names! (contour-object-names) hide?)}))

(defn hide-terrain-for-draw!
  "Hide elevated terrain + site-contour-* (draw pad stays)."
  []
  (set-terrain-and-contours-hide! true))

(defn show-terrain-after-draw!
  "Show terrain and contour objects after draw mode."
  []
  (set-terrain-and-contours-hide! false))

(defn adopt-under-site-root!
  "Parent named object under site-root with house-center parent inverse."
  [s obj-name]
  (let [root (mesh/object-by-name "site-root")
        obj (mesh/object-by-name obj-name)]
    (when (and root obj)
      (let [[cx cy] (house/house-center-xy s)
            mpi (.Translation (.-Matrix mathutils)
                              #py [(double (- cx)) (double (- cy)) 0.0])]
        (set! (.-parent obj) root)
        (set! (.-matrix-parent-inverse obj) (.copy mpi))
        {:parented obj-name}))))

(defn ensure-draw-root!
  "Ensure site-root exists and is north-offset oriented (needed after clear-site!)."
  [s]
  (when-not (mesh/object-by-name "site-root")
    (hierarchy/ensure-site-root! s))
  (hierarchy/orient-site-root! s))
