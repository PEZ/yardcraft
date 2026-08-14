(ns yardcraft.site-viewport
  "VIEW_3D shading modes and lot/house framing (top + elevations)."
  (:require [yardcraft.site-mesh :as mesh]
            [yardcraft.site-house :as house]
            [yardcraft.site-lot :as lot])
  (:import bpy mathutils))

(defn- view3d-spaces
  "VIEW_3D spaces in the current screen."
  []
  (vec (for [area (.-areas (.-screen (.-context bpy)))
             :when (= (.-type area) "VIEW_3D")
             space (.-spaces area)
             :when (= (.-type space) "VIEW_3D")]
         space)))

(defn- view3d-areas
  []
  (filterv #(= (.-type %) "VIEW_3D")
           (.-areas (.-screen (.-context bpy)))))

(defn- update-view3d-shading!
  "Apply f to each VIEW_3D shading settings. Returns {:updated n}."
  [f]
  (let [spaces (view3d-spaces)]
    (run! #(f (.-shading %)) spaces)
    {:updated (count spaces)}))

(defn show-material-colors!
  "Solid viewport shading using material colors."
  []
  (update-view3d-shading!
   (fn [sh]
     (set! (.-type sh) "SOLID")
     (set! (.-color-type sh) "MATERIAL"))))

(defn show-material-lights!
  "Material Preview using scene lights (e.g. site-sun), not world."
  []
  (update-view3d-shading!
   (fn [sh]
     (set! (.-type sh) "MATERIAL")
     (set! (.-use-scene-lights sh) true)
     (set! (.-use-scene-world sh) false))))

(defn show-rendered!
  "Rendered viewport shading (EEVEE/Cycles); soft world sky fill + site-sun."
  []
  (update-view3d-shading!
   (fn [sh]
     (set! (.-type sh) "RENDERED"))))

(defn hide-relationship-lines!
  "Turn off VIEW_3D parent/constraint relationship overlays."
  []
  (let [spaces (view3d-spaces)]
    (run! #(set! (.-show-relationship-lines (.-overlay %)) false) spaces)
    {:updated (count spaces)}))

(defn play-animation!
  "Start timeline playback if not already playing (screen.animation_play is a toggle)."
  []
  (let [screen (.-screen (.-context bpy))
        was-playing? (.-is_animation_playing screen)]
    (when-not was-playing?
      (.animation_play (.-screen (.-ops bpy))))
    {:playing? (.-is_animation_playing (.-screen (.-context bpy)))
     :started? (not was-playing?)}))

(defn show-n-panel!
  "Open VIEW_3D sidebar (N) and select panel category when supported (e.g. \"Yardcraft\")."
  [category]
  (let [areas (view3d-areas)
        updated
        (reduce
         (fn [n area]
           (doseq [space (filter #(= (.-type %) "VIEW_3D") (.-spaces area))]
             (set! (.-show_region_ui space) true))
           (when-let [region (first (filter #(= (.-type %) "UI") (.-regions area)))]
             (when (and category (hasattr region "active_panel_category"))
               (try
                 (setattr region "active_panel_category" category)
                 (catch python/Exception _)))
             (.tag_redraw area))
           (inc n))
         0
         areas)]
    {:updated updated :category category}))

(defn show-scene-camera!
  "Switch all VIEW_3D spaces to the scene camera (View → Cameras → Active Camera / no numpad)."
  []
  (let [spaces (view3d-spaces)]
    (doseq [space spaces]
      (set! (.-view_perspective (.-region_3d space)) "CAMERA"))
    {:updated (count spaces)
     :camera (some-> (.-camera (.-scene (.-context bpy))) .-name)}))

(defn- lot-world-aabb
  "Axis-aligned world bounds of :lot/polygon-xy under current site-root.
  Returns nil when polygon is missing/empty."
  [s]
  (when-let [poly (seq (lot/lot-polygon-xy s))]
    (let [[cx cy] (house/house-center-xy s)
          root (mesh/object-by-name "site-root")
          rw (if root (.-matrix_world root) (.Identity (.-Matrix mathutils) 4))
          mpi (.Translation (.-Matrix mathutils) #py [(double (- cx)) (double (- cy)) 0.0])
          world-xy (mapv (fn [[x y]]
                           (let [lv (mathutils/Vector #py [(double x) (double y) 0.0])
                                 wv (.__matmul__ rw (.__matmul__ mpi lv))]
                             [(aget wv 0) (aget wv 1)]))
                         poly)
          xs (map first world-xy)
          ys (map second world-xy)
          min-x (apply min xs)
          max-x (apply max xs)
          min-y (apply min ys)
          max-y (apply max ys)]
      {:mid-x (/ (+ min-x max-x) 2.0)
       :mid-y (/ (+ min-y max-y) 2.0)
       :span (max (- max-x min-x) (- max-y min-y))
       :min-x min-x :max-x max-x :min-y min-y :max-y max-y})))

(defn- vec3
  [[x y z]]
  (mathutils/Vector #py [(double x) (double y) (double z)]))

(defn- view-quat-looking
  "Quaternion for ortho view looking along forward with given up."
  [forward up]
  (let [f (.normalized (vec3 forward))
        u (.normalized (vec3 up))
        z (.__mul__ f -1.0)
        x (.normalized (.cross u z))
        y (.cross z x)
        m (mathutils/Matrix #py [#py [(aget x 0) (aget y 0) (aget z 0)]
                                 #py [(aget x 1) (aget y 1) (aget z 1)]
                                 #py [(aget x 2) (aget y 2) (aget z 2)]])]
    (.to_quaternion m)))

(defn- site-root-axes-world
  "House-local +X/+Y as world vectors (from site-root)."
  []
  (let [root (mesh/object-by-name "site-root")
        mw (if root (.-matrix_world root) (.Identity (.-Matrix mathutils) 4))
        x (.__matmul__ mw (mathutils/Vector #py [1.0 0.0 0.0]))
        y (.__matmul__ mw (mathutils/Vector #py [0.0 1.0 0.0]))]
    {:x [(aget x 0) (aget x 1) (aget x 2)]
     :y [(aget y 0) (aget y 1) (aget y 2)]}))

(defn- frame-view3d!
  [area [mid-x mid-y mid-z] dist quat]
  (doseq [space (filter #(= (.-type %) "VIEW_3D") (.-spaces area))]
    (let [r3d (.-region_3d space)]
      (set! (.-view_perspective r3d) "ORTHO")
      (set! (.-view_location r3d) #py [(double mid-x) (double mid-y) (double mid-z)])
      (set! (.-view_distance r3d) (double dist))
      (set! (.-view_rotation r3d) quat))))

(defn- frame-all-view3d!
  [loc dist quat]
  (let [areas (view3d-areas)]
    (run! #(frame-view3d! % loc dist quat) areas)
    {:updated (count areas)}))

(defn- house-view-focus
  "House center in world (XY at origin after site-root) and sizes."
  [s]
  (let [[width depth] (:house/size-m s)
        height (:house/schematic-height-m s)
        mid-z (+ (:house/floor-z s) (/ height 2.0))]
    {:mid-x 0.0 :mid-y 0.0 :mid-z mid-z
     :width width :depth depth :height height}))

(defn frame-lot-top!
  "Top orthographic view framing the lot with ~6% margin (world north up)."
  [s]
  (if-let [{:keys [mid-x mid-y span]} (lot-world-aabb s)]
    (let [dist (* span 1.06)
          quat (mathutils/Quaternion #py [1.0 0.0 0.0 0.0])
          r (frame-all-view3d! [mid-x mid-y 0.0] dist quat)]
      (merge r {:mid-x mid-x :mid-y mid-y :span span :view-distance dist}))
    {:status :no-lot}))

(defn frame-world-rect-top!
  "Top orthographic view framing an axis-aligned world XY rect with ~6% margin (north up)."
  [{:keys [min-x max-x min-y max-y]}]
  (let [mid-x (/ (+ min-x max-x) 2.0)
        mid-y (/ (+ min-y max-y) 2.0)
        span (max (- max-x min-x) (- max-y min-y) 1.0)
        dist (* span 1.06)
        quat (mathutils/Quaternion #py [1.0 0.0 0.0 0.0])
        r (frame-all-view3d! [mid-x mid-y 0.0] dist quat)]
    (merge r {:mid-x mid-x :mid-y mid-y :span span :view-distance dist})))

(defn frame-lot-top-house!
  "Top ortho framing the lot; house west left / road up (site-root axes)."
  [s]
  (if-let [{:keys [mid-x mid-y span]} (lot-world-aabb s)]
    (let [dist (* span 1.06)
          {:keys [y]} (site-root-axes-world)
          quat (view-quat-looking [0.0 0.0 -1.0] y)
          r (frame-all-view3d! [mid-x mid-y 0.0] dist quat)]
      (merge r {:mid-x mid-x :mid-y mid-y :span span :view-distance dist}))
    {:status :no-lot}))

(defn frame-house-south!
  "Ortho looking straight at the house south facade."
  [s]
  (if (and (:house/size-m s) (:house/schematic-height-m s) (:house/floor-z s))
    (let [{:keys [mid-x mid-y mid-z width height]} (house-view-focus s)
          dist (* (max width height) 1.15)
          {:keys [y]} (site-root-axes-world)
          quat (view-quat-looking y [0.0 0.0 1.0])
          r (frame-all-view3d! [mid-x mid-y mid-z] dist quat)]
      (merge r {:mid-x mid-x :mid-y mid-y :mid-z mid-z :view-distance dist}))
    {:status :no-house}))

(defn frame-house-east!
  "Ortho looking straight at the house east facade."
  [s]
  (if (and (:house/size-m s) (:house/schematic-height-m s) (:house/floor-z s))
    (let [{:keys [mid-x mid-y mid-z depth height]} (house-view-focus s)
          dist (* (max depth height) 1.15)
          {:keys [x]} (site-root-axes-world)
          quat (view-quat-looking (mapv - x) [0.0 0.0 1.0])
          r (frame-all-view3d! [mid-x mid-y mid-z] dist quat)]
      (merge r {:mid-x mid-x :mid-y mid-y :mid-z mid-z :view-distance dist}))
    {:status :no-house}))
