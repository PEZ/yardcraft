(ns yardcraft.site-fly
  "Narrative hybrid camera fly: Follow Path + Track To with eased keys.

  Path `:tour/*` and gaze `:look-at/*` specs resolve against site surfaces/objects.
  Sparse authored keys; Blender's Bezier easing does the smoothing.
  Objects: site-fly-path, site-fly-lookat, site-fly-camera. Excluded from site-root parenting.

  Playback: `(view-fly-camera!)`, Yardcraft panel Fly cam; Space / scrub timeline.

  Rebuild: `(require '[yardcraft.site-fly :as fly] :reload)` then `(fly/ensure-fly-tour! site)` (load-file is fine on Basilisp ≥0.5)."
  (:require [yardcraft.site-driveway :as driveway]
            [yardcraft.site-house :as house]
            [yardcraft.site-lm :as lm]
            [yardcraft.site-lot :as lot]
            [yardcraft.site-terrace :as terrace]
            [yardcraft.site-viewport :as viewport])
  (:import bpy math mathutils
           [operator :as op]))

(def ^:private fly-names
  ["site-fly-path" "site-fly-lookat" "site-fly-camera"])

(def ^:private tour-path-spec
  "Camera path waypoints in house-NW.
  Standing: [:tour/road|:tour/driveway|:tour/deck x y] or [:tour/stair :east|:west x y step-i].
  Aerial: [:tour/fly x y z] — absolute model Z.
  Empty until a site-specific tour is authored — populate then call ensure-fly-tour!."
  [])

(def ^:private driver-eye-m 1.15)

(def ^:private walk-eye-m 1.80)

(defn- deck-top-z
  [s]
  (+ (:house/floor-z s) (:terrace/slab-thickness-m s)))

(defn- fp-center-xy [fp]
  [(/ (+ (:min-x fp) (:max-x fp)) 2.0)
   (/ (+ (:min-y fp) (:max-y fp)) 2.0)])

(defn- look-road [s x y]
  [x y (+ (lot/road-surface-z s [x y]) 1.2)])

(defn- look-driveway [s x y]
  [x y (+ (driveway/driveway-surface-z s [x y]) 1.2)])

(defn- look-door-north [s]
  (let [fp (house/door-north-footprint s)
        [x y] (fp-center-xy fp)]
    [x y (+ (:house/floor-z s) (/ (:door/height-m s) 2.0))]))

(defn- look-terrace-south [s x]
  (let [fp (terrace/terrace-south-footprint s)]
    [x (:min-y fp) (+ (deck-top-z s) 0.65)]))

(defn- look-bod-behind [s]
  (let [fp (house/bod-footprint s)]
    [(- (:min-x fp) 0.4) (:min-y fp) (+ (house/bod-floor-z s) 0.75)]))

(defn- look-cafe [s]
  (let [{:keys [xy]} (:furniture/cafe s)
        [x y] xy]
    [x y (+ (deck-top-z s) walk-eye-m)]))

(defn- look-terrace-east [s]
  (let [fp (terrace/terrace-south-footprint s)]
    [(+ (:max-x fp) 1.2)
     (/ (+ (:min-y fp) (:max-y fp)) 2.0)
     (:house/floor-z s)]))

(defn- look-tree [s tree-name]
  (let [t (first (filter #(= tree-name (:name %)) (:trees/plantings s)))
        [x y] (:xy t)]
    [x y (* 0.55 (:height-m t))]))

(defn- look-canopy [s]
  (let [fp (terrace/terrace-roof-footprint s)
        [x y] (fp-center-xy fp)]
    [x y (+ (deck-top-z s) (:terrace/canopy-clearance-m s))]))

(defn- stair-tread-z
  "Walkable top of stair step `step-i` (0 = top tread at floor-z) for `:east` or `:west`."
  [s side step-i]
  (let [{:keys [drop-m steps]} (case side
                                 :east (:stairs/east s)
                                 :west (:stairs/west s))]
    (- (:house/floor-z s) (* (double step-i) (/ (double drop-m) (double steps))))))

(defn- standing-cam-z
  "Eye-height camera Z above a standing surface.
  For `:tour/stair`, pass opts `{:side :east|:west :step-i n}`."
  [s kind xy {:keys [side step-i]}]
  (cond
    (= kind :tour/road) (+ (lot/road-surface-z s xy) driver-eye-m)
    (= kind :tour/driveway) (+ (driveway/driveway-surface-z s xy) driver-eye-m)
    (= kind :tour/deck) (+ (deck-top-z s) walk-eye-m)
    (= kind :tour/stair) (+ (stair-tread-z s side step-i) walk-eye-m)))

(defn- resolve-path-point
  "Expand a path spec waypoint to [x y z] in house-NW."
  [s wp]
  (let [kind (nth wp 0)]
    (cond
      (= kind :tour/fly)
      [(nth wp 1) (nth wp 2) (nth wp 3)]

      (= kind :tour/stair)
      (let [side (nth wp 1)
            x (nth wp 2)
            y (nth wp 3)
            step-i (nth wp 4)]
        [x y (standing-cam-z s kind [x y] {:side side :step-i step-i})])

      :else
      (let [x (nth wp 1)
            y (nth wp 2)]
        [x y (standing-cam-z s kind [x y] nil)]))))

(defn- tour-path-local
  "Resolve `tour-path-spec` against site facts `s` → [[x y z] …]."
  [s]
  (mapv #(resolve-path-point s %) tour-path-spec))

(def ^:private tour-look-spec
  "Gaze script [frame look-target]. Repeated targets = dwells.
  Targets are `:look-at/*` forms resolved from site facts/footprints.
  Empty until a site-specific tour is authored."
  [])

(defn- resolve-look-at
  "Expand a `:look-at/*` target to [x y z] in house-NW."
  [s target]
  (let [kind (nth target 0)]
    (cond
      (= kind :look-at/road) (look-road s (nth target 1) (nth target 2))
      (= kind :look-at/driveway) (look-driveway s (nth target 1) (nth target 2))
      (= kind :look-at/door-north) (look-door-north s)
      (= kind :look-at/terrace-south) (look-terrace-south s (nth target 1))
      (= kind :look-at/bod-behind) (look-bod-behind s)
      (= kind :look-at/cafe) (look-cafe s)
      (= kind :look-at/terrace-east) (look-terrace-east s)
      (= kind :look-at/tree) (look-tree s (nth target 1))
      (= kind :look-at/canopy) (look-canopy s))))

(defn- tour-look-local
  "Resolve `tour-look-spec` against site facts `s` → [[frame [x y z]] …]."
  [s]
  (mapv (fn [[frame target]] [frame (resolve-look-at s target)]) tour-look-spec))

(defn- point-dist
  [[x0 y0 z0] [x1 y1 z1]]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        dz (- z1 z0)]
    (math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))))

(defn- path-chord-cum
  "Cumulative chord lengths along pts; first element 0."
  [pts]
  (loop [xs pts
         acc [0.0]]
    (if (next xs)
      (recur (next xs) (conj acc (+ (peek acc) (point-dist (first xs) (second xs)))))
      acc)))

(defn- offset-at
  [cum i]
  (/ (double (nth cum i)) (double (peek cum))))

(defn- tour-offset-end-frame
  "Last tour frame when offset keys are auto-derived from path alone."
  []
  (if (seq tour-look-spec)
    (first (peek tour-look-spec))
    250))

(defn- tour-offset-keys
  "Frame → Follow Path offset_factor keys.
  Empty when path is empty/degenerate. With ≥2 points and positive chord length:
    (let [cum (path-chord-cum path-local)]
      [[1 (offset-at cum 0)]
       [end (offset-at cum (dec (count path-local)))]])"
  [path-local]
  (if (< (count path-local) 2)
    []
    (let [cum (path-chord-cum path-local)]
      (if (zero? (peek cum))
        []
        (let [end (tour-offset-end-frame)]
          [[1 (offset-at cum 0)]
           [end (offset-at cum (dec (count path-local)))]])))))

(defn remove-fly-proof!
  "Removes site-fly-* tour objects if present."
  []
  (doseq [n fly-names]
    (when-let [o (.get (.-objects (.-data bpy)) n)]
      (.remove (.-objects (.-data bpy)) o ** :do-unlink true)))
  :removed)

(defn- tidy-fly-view!
  "Deselect; hide path/lookat/camera objects in viewport; disable camera frame overlays
  (passepartout + dashed border / guides) so camera view looks like a plain 3D view."
  []
  (.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
  (doseq [n fly-names
          :let [o (.get (.-objects (.-data bpy)) n)]
          :when o]
    (set! (.-hide-viewport o) true))
  (when-let [cam (.get (.-objects (.-data bpy)) "site-fly-camera")]
    (set! (.-show-passepartout (.-data cam)) false))
  (doseq [area (.-areas (.-screen (.-context bpy)))
          :when (= (.-type area) "VIEW_3D")
          space (.-spaces area)
          :when (= (.-type space) "VIEW_3D")
          :let [ov (.-overlay space)]]
    (set! (.-show-camera-passepartout ov) false)
    (set! (.-show-camera-guides ov) false))
  :tidied)

(defn view-fly-camera!
  "Make site-fly-camera the scene camera and enter camera view (no numpad)."
  []
  (when-let [cam (.get (.-objects (.-data bpy)) "site-fly-camera")]
    (set! (.-camera (.-scene (.-context bpy))) cam))
  (tidy-fly-view!)
  (viewport/show-scene-camera!))

(defn- house-nw->world
  [s [x y z]]
  (let [root (.get (.-objects (.-data bpy)) "site-root")
        [cx cy] (house/house-center-xy s)
        v (op/matmul (.-matrix-world root)
                     (.Vector mathutils #py [(double (- x cx)) (double (- y cy)) (double z) 1.0]))]
    [(.-x v) (.-y v) (.-z v)]))

(defn- set-bezier-points!
  [curve-obj points]
  (let [spline (aget (.-splines (.-data curve-obj)) 0)
        bps (.-bezier-points spline)
        need (count points)
        have (count bps)]
    (when (< have need)
      (.add bps (- need have)))
    (doseq [[i [x y z]] (map-indexed vector points)]
      (let [bp (aget bps i)]
        (set! (.-co bp) #py [(double x) (double y) (double z)])
        (set! (.-handle-left-type bp) "AUTO")
        (set! (.-handle-right-type bp) "AUTO")))
    need))

(defn- add-fly-path!
  [path-pts]
  (.primitive-bezier-curve-add (.-curve (.-ops bpy)) **
                               :location #py [0.0 0.0 0.0])
  (let [path (.-object (.-context bpy))]
    (set! (.-name path) "site-fly-path")
    (set! (.-dimensions (.-data path)) "3D")
    (set-bezier-points! path path-pts)
    path))

(defn- add-fly-lookat!
  [[x y z]]
  (.empty-add (.-object (.-ops bpy)) **
              :type "SPHERE"
              :location #py [(double x) (double y) (double z)])
  (let [look (.-object (.-context bpy))]
    (set! (.-name look) "site-fly-lookat")
    (set! (.-empty-display-size look) 0.4)
    look))

(defn- keyframe-lookat!
  [look look-keys]
  (doseq [[frame [x y z]] look-keys]
    (set! (.-location look) #py [(double x) (double y) (double z)])
    (.keyframe-insert look ** :data-path "location" :frame frame)))

(defn- add-fly-camera!
  [path look offset-keys]
  (.camera-add (.-object (.-ops bpy)) ** :location #py [0.0 0.0 0.0])
  (let [cam (.-object (.-context bpy))
        follow (.new (.-constraints cam) "FOLLOW_PATH")
        track (.new (.-constraints cam) "TRACK_TO")]
    (set! (.-name cam) "site-fly-camera")
    (set! (.-show-passepartout (.-data cam)) false)
    (set! (.-location cam) #py [0.0 0.0 0.0])
    (set! (.-rotation-euler cam) #py [0.0 0.0 0.0])
    (set! (.-target follow) path)
    (set! (.-use-fixed-location follow) true)
    (set! (.-use-curve-follow follow) false)
    (set! (.-target track) look)
    (set! (.-track-axis track) "TRACK_NEGATIVE_Z")
    (set! (.-up-axis track) "UP_Y")
    (doseq [[frame offset] offset-keys]
      (set! (.-offset-factor follow) offset)
      (.keyframe-insert follow ** :data-path "offset_factor" :frame frame))
    cam))

(defn- configure-scene-camera!
  [cam end-frame]
  (let [scene (.-scene (.-context bpy))]
    (set! (.-camera scene) cam)
    (set! (.-frame-start scene) 1)
    (set! (.-frame-end scene) end-frame)
    (set! (.-frame-current scene) 1)
    scene))

(defn- action-fcurves
  "FCurves from a legacy or Blender-5 layered Action."
  [action]
  (or (try (seq (.-fcurves action))
           (catch python/Exception _ nil))
      (try (let [layer (aget (.-layers action) 0)
                 strip (aget (.-strips layer) 0)
                 bag (aget (.-channelbags strip) 0)]
             (seq (.-fcurves bag)))
           (catch python/Exception _ nil))))

(defn- ease-fcurve-points!
  [fc]
  (doseq [kp (.-keyframe-points fc)]
    (set! (.-interpolation kp) "BEZIER")
    (set! (.-easing kp) "AUTO")
    (set! (.-handle-left-type kp) "AUTO_CLAMPED")
    (set! (.-handle-right-type kp) "AUTO_CLAMPED")))

(defn- ease-object-fcurves!
  [obj]
  (when-let [fcs (some-> obj .-animation-data .-action action-fcurves)]
    (run! ease-fcurve-points! fcs))
  obj)

(defn ensure-fly-tour!
  "Build narrative road→driveway→terrace→west-exit→orbit-home fly.
  Overlays LM road Z profile (same as ensure-site!) so path eye height tracks the road mesh.
  Takes site facts `s`. Returns summary map, or `{:fly :no-tour :reason …}` when no tour is authored."
  [s]
  (let [existing-cam (.get (.-objects (.-data bpy)) "site-fly-camera")
        s (lm/with-lm-road s)
        path-local (tour-path-local s)
        offset-keys (tour-offset-keys path-local)]
    (if (or (empty? tour-path-spec) (empty? offset-keys))
      (do
        (when existing-cam (view-fly-camera!))
        {:fly :no-tour
         :reason (if (empty? tour-path-spec) :empty-path-spec :degenerate-path)})
      (do
        (remove-fly-proof!)
        (let [path-pts (mapv #(house-nw->world s %) path-local)
              look-keys (mapv (fn [[frame p]] [frame (house-nw->world s p)]) (tour-look-local s))
              end-frame (first (last offset-keys))
              start-look (second (first look-keys))
              path (add-fly-path! path-pts)
              look (add-fly-lookat! start-look)
              _ (keyframe-lookat! look look-keys)
              cam (add-fly-camera! path look offset-keys)]
          (ease-object-fcurves! look)
          (ease-object-fcurves! cam)
          (configure-scene-camera! cam end-frame)
          (view-fly-camera!)
          {:path "site-fly-path"
           :lookat "site-fly-lookat"
           :camera "site-fly-camera"
           :frames [1 end-frame]})))))

(defn ensure-fly-proof!
  "Alias for ensure-fly-tour! (RCF compat)."
  [s]
  (ensure-fly-tour! s))
