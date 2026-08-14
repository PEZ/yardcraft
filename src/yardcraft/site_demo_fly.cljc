(ns yardcraft.site-demo-fly
  "Orbit fly camera for the welcome demo."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-viewport :as viewport])
  (:import bpy math))

(def ^:private fly-names
  ["site-fly-path" "site-fly-lookat" "site-fly-camera"])

(defn- remove-fly-objects! []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-fly-")
                (mesh/site-object-names))))

(defn- set-bezier-points! [curve-obj points]
  (let [spline (aget (.-splines (.-data curve-obj)) 0)
        bps (.-bezier-points spline)
        need (count points) have (count bps)]
    (when (< have need) (.add bps (- need have)))
    (set! (.-use-cyclic-u spline) true)
    (doseq [[i [x y z]] (map-indexed vector points)]
      (let [bp (aget bps i)]
        (set! (.-co bp) #py [(double x) (double y) (double z)])
        (set! (.-handle-left-type bp) "AUTO")
        (set! (.-handle-right-type bp) "AUTO")))))

(defn- add-fly-path! [path-pts]
  (.primitive-bezier-curve-add (.-curve (.-ops bpy)) **
                               :location #py [0.0 0.0 0.0])
  (let [path (.-object (.-context bpy))]
    (set! (.-name path) "site-fly-path")
    (set! (.-dimensions (.-data path)) "3D")
    (set-bezier-points! path path-pts)
    path))

(defn- add-fly-lookat! [[x y z]]
  (.empty_add (.-object (.-ops bpy)) **
              :type "SPHERE"
              :location #py [(double x) (double y) (double z)])
  (let [look (.-object (.-context bpy))]
    (set! (.-name look) "site-fly-lookat")
    (set! (.-empty_display-size look) 0.4)
    look))

(defn- add-fly-camera! [path look offset-keys]
  (.camera_add (.-object (.-ops bpy)) ** :location #py [0.0 0.0 0.0])
  (let [cam (.-object (.-context bpy))
        follow (.new (.-constraints cam) "FOLLOW_PATH")
        track (.new (.-constraints cam) "TRACK_TO")]
    (set! (.-name cam) "site-fly-camera")
    (set! (.-show-passepartout (.-data cam)) false)
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

(defn- action-fcurves [action]
  (or (try (seq (.-fcurves action))
           (catch python/Exception _ nil))
      (try (let [layer (aget (.-layers action) 0)
                 strip (aget (.-strips layer) 0)
                 bag (aget (.-channelbags strip) 0)]
             (seq (.-fcurves bag)))
           (catch python/Exception _ nil))))

(defn- linearize-object-fcurves! [obj]
  (when-let [fcs (some-> obj .-animation-data .-action action-fcurves)]
    (doseq [fc fcs
            kp (.-keyframe-points fc)]
      (set! (.-interpolation kp) "LINEAR")))
  obj)

(defn- tidy-fly-view! []
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
    (set! (.-show-camera-guides ov) false)))

(defn- view-fly-camera! []
  (when-let [cam (.get (.-objects (.-data bpy)) "site-fly-camera")]
    (set! (.-camera (.-scene (.-context bpy))) cam))
  (tidy-fly-view!)
  (viewport/show-scene-camera!))

(defn ensure-orbit-fly! []
  (remove-fly-objects!)
  (let [n-pts 12
        r (* 22.0 0.75)
        h 12.0
        end-frame 250
        θ0 (+ (- (/ math/pi 2.0)) (math/radians 15.0))
        path-pts (mapv (fn [i]
                         (let [θ (+ θ0 (* 2.0 math/pi (/ (double i) (double n-pts))))]
                           [(* r (math/cos θ)) (* r (math/sin θ)) h]))
                       (range n-pts))
        offset-keys [[1 0.0] [end-frame 1.0]]
        path (add-fly-path! path-pts)
        look (add-fly-lookat! [0.0 0.0 1.0])
        cam (linearize-object-fcurves! (add-fly-camera! path look offset-keys))
        scene (.-scene (.-context bpy))]
    (set! (.-camera scene) cam)
    (set! (.-frame-start scene) 1)
    (set! (.-frame-end scene) end-frame)
    (set! (.-frame-current scene) 1)
    (view-fly-camera!)
    (let [play (viewport/play-animation!)]
      {:path "site-fly-path"
       :lookat "site-fly-lookat"
       :camera "site-fly-camera"
       :frames [1 end-frame]
       :play play})))
