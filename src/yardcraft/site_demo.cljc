(ns yardcraft.site-demo
  "Welcome demo: YARDCRAFT patio letters, furniture, sundial, orbit fly cam.

  `(ensure-demo!)` after REPL connect — README onboarding scene.
  Objects: site-demo-patio-*, site-furniture-*, site-sundial-*, site-fly-*.
  Does not require yardcraft.site-ui at ns level (avoid cycles)."
  (:require [basilisp.string :as string]
            [yardcraft.site :as site]
            [yardcraft.site-furniture :as furniture]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-sun :as sun]
            [yardcraft.site-viewport :as viewport])
  (:import bpy math mathutils))

(def ^:private cell-m 0.5)
(def ^:private slab-thick 0.14)
(def ^:private glyph-rows 7)
(def ^:private glyph-cols 5)
(def ^:private letter-gap-m cell-m)
(def ^:private word "YARDCRAFT")

(def ^:private glyphs
  {"Y" ["10001" "10001" "01010" "00100" "00100" "00100" "00100"]
   "A" ["00100" "01010" "10001" "10001" "11111" "10001" "10001"]
   "R" ["11110" "10001" "10001" "11110" "10100" "10010" "10001"]
   "D" ["11100" "10010" "10001" "10001" "10001" "10010" "11100"]
   "C" ["01110" "10001" "10000" "10000" "10000" "10001" "01110"]
   "F" ["11111" "10000" "10000" "11110" "10000" "10000" "10000"]
   "T" ["11111" "00100" "00100" "00100" "00100" "00100" "00100"]})

(def ^:private fly-names
  ["site-fly-path" "site-fly-lookat" "site-fly-camera"])

(defonce ^:private demo-facts* (atom nil))

(defn- word-width-m []
  (let [n (count word)
        letter-w (* glyph-cols cell-m)]
    (+ (* n letter-w) (* (dec n) letter-gap-m))))

(defn- word-start-x []
  (- (/ (word-width-m) 2.0)))

(defn- letter-start-x [idx]
  (+ (word-start-x) (* idx (+ (* glyph-cols cell-m) letter-gap-m))))

(defn- cell-center-xy [letter-idx col row]
  (let [lx (+ (letter-start-x letter-idx) (* col cell-m) (/ cell-m 2.0))
        word-h (* glyph-rows cell-m)
        bottom-y (- (/ word-h 2.0))
        ly (+ bottom-y (* (- (dec glyph-rows) row) cell-m) (/ cell-m 2.0))]
    [lx ly]))

(defn- letter-center-xy [letter-idx]
  (let [sx (letter-start-x letter-idx)
        sy (- (/ (* glyph-rows cell-m) 2.0))]
    [(+ sx (/ (* glyph-cols cell-m) 2.0))
     (+ sy (/ (* glyph-rows cell-m) 2.0))]))

(defn- demo-furniture-facts [s]
  (let [[lx ly] (cell-center-xy 1 1 3)
        [rx ry] (cell-center-xy 1 3 3)
        [cx cy] (letter-center-xy 2)]
    (merge s
           {:furniture/lounger {:size-m [0.65 1.75]
                                :seat-size-m [0.65 1.75]
                                :seat-height-m 0.38
                                :seat-thickness-m 0.05
                                :back-height-m 0.5
                                :back-thickness-m 0.05
                                :back-tilt-deg 28
                                :side-table {:diameter-m 0.42 :height-m 0.35}
                                :placements [{:xy [lx ly] :with-side-table? true}
                                             {:xy [rx ry]}]}
            :furniture/cafe {:xy [cx cy]
                             :rot-z-deg 0
                             :table {:size-m [0.75 0.75] :height-m 0.72
                                      :top-thickness-m 0.04 :xy [0 0]}
                             :chair {:seat-size-m [0.42 0.42]
                                     :seat-height-m 0.45
                                     :seat-thickness-m 0.04
                                     :back-height-m 0.42
                                     :back-thickness-m 0.04
                                     :clearance-m 0.35}
                             :chairs [{:xy [0.0 -0.85] :rot-z-deg 0.0}
                                      {:xy [0.0 0.85] :rot-z-deg 180.0}]}})))

(defn- base-demo-facts []
  (demo-furniture-facts
   {:site/lat-deg 59.3
    :site/lon-deg 18.0
    :site/timezone "Europe/Stockholm"
    :site/north-offset-deg 0.0
    :sun/date "2026-06-21"
    :sun/time-of-day "13:00"
    :house/floor-z slab-thick
    :terrace/slab-thickness-m 0.0
    :terrace/roof-opacity 1.0
    :sundial/radius-m 0.7
    :world/color [0.45 0.52 0.65]
    :world/strength 0.15}))

(defn- reset-demo-facts! []
  (reset! demo-facts* (base-demo-facts)))

(defn demo-facts []
  (or @demo-facts* (base-demo-facts)))

(defn demo-active? []
  (boolean (some #(string/starts-with? % "site-demo-patio-")
                 (mesh/site-object-names))))

(defn- build-letter-patios! [letter-idx ch]
  (let [rows (get glyphs ch)]
    (vec
     (for [row (range glyph-rows)
           col (range glyph-cols)
           :when (= (subs (nth rows row) col (inc col)) "1")
           :let [n (str "site-demo-patio-" letter-idx "-" row "-" col)
                 [cx cy] (cell-center-xy letter-idx col row)
                 center (mesh/box-center [cell-m cell-m slab-thick] [cx cy] 0.0)]]
       (do (mesh/add-box! n [cell-m cell-m slab-thick] center)
           n)))))

(defn- build-word-patios! []
  (vec (mapcat (fn [i]
                 (build-letter-patios! i (subs word i (inc i))))
               (range (count word)))))

(defn- true-north-xy [s]
  (let [θ (math/radians (:site/north-offset-deg s 0.0))]
    [(- (math/sin θ)) (math/cos θ)]))

(defn- clear-sundial! []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-sundial")
                (mesh/site-object-names))))

(defn- add-sundial-face! [cx cy z radius]
  (.primitive-cylinder-add (.-mesh (.-ops bpy)) **
                           :radius (double radius)
                           :depth 0.05
                           :location #py [(double cx) (double cy) (double z)]
                           :vertices 48)
  (let [face (.-object (.-context bpy))]
    (set! (.-name face) "site-sundial-face")
    face))

(defn- add-sundial-gnomon! [{:keys [cx cy z radius nx ny lat]}]
  (let [len (* radius 0.85)
        dx (* nx (math/cos lat))
        dy (* ny (math/cos lat))
        dz (math/sin lat)
        gnomon (mesh/add-box! "site-sundial-gnomon"
                              [0.04 0.04 len]
                              [(+ cx (* dx (/ len 2.0)))
                               (+ cy (* dy (/ len 2.0)))
                               (+ z (* dz (/ len 2.0)))])
        dir (.normalized (mathutils/Vector #py [(double dx) (double dy) (double dz)]))]
    (set! (.-rotation-euler gnomon) (.to-euler (.to-track-quat dir "Z" "Y")))
    gnomon))

(defn- hour-tick! [{:keys [cx cy z h lx ly r0 r1 noon?]}]
  (let [n (str "site-sundial-hour-" h)
        tx (* lx (/ (+ r0 r1) 2.0))
        ty (* ly (/ (+ r0 r1) 2.0))
        tick (mesh/add-box! n
                            [(if noon? 0.07 0.03) (- r1 r0) 0.04]
                            [(+ cx tx) (+ cy ty) (+ z 0.04)])
        tdir (.normalized (mathutils/Vector #py [(double lx) (double ly) 0.0]))]
    (set! (.-rotation-euler tick) (.to-euler (.to-track-quat tdir "Y" "Z")))
    n))

(defn- ensure-demo-sundial! [s]
  (clear-sundial!)
  (let [radius (:sundial/radius-m s 0.7)
        [cx cy] (letter-center-xy 3)
        z (+ slab-thick 0.02)
        [nx ny] (true-north-xy s)
        lat (math/radians (:site/lat-deg s))
        _ (add-sundial-face! cx cy z radius)
        _ (add-sundial-gnomon! {:cx cx :cy cy :z z :radius radius
                                :nx nx :ny ny :lat lat})
        hours (mapv
               (fn [h]
                 (let [H (math/radians (* 15.0 (- h 12.0)))
                       θ (math/atan2 (* (math/sin H) (math/sin lat)) (math/cos H))
                       lx (+ (* nx (math/cos θ)) (* ny (math/sin θ)))
                       ly (+ (* ny (math/cos θ)) (* (- nx) (math/sin θ)))
                       r0 (* radius 0.55) r1 (* radius 0.95)]
                   (hour-tick! {:cx cx :cy cy :z z :h h :lx lx :ly ly
                                :r0 r0 :r1 r1 :noon? (= h 12)})))
               (range 4 22))]
    {:face "site-sundial-face" :gnomon "site-sundial-gnomon" :hours hours}))

(defn- paint-demo! [_s]
  (let [terrace (mesh/ensure-material! "site-mat-terrace" (:terrace mesh/material-colors)
                                       :roughness 0.85)
        furniture (mesh/ensure-material! "site-mat-furniture" (:furniture mesh/material-colors)
                                         :roughness 0.85)
        sundial-m (mesh/ensure-material! "site-mat-sundial" (:sundial mesh/material-colors))
        mark-m (mesh/ensure-material! "site-mat-sundial-mark" (:sundial-mark mesh/material-colors))
        noon-m (mesh/ensure-material! "site-mat-sundial-noon" (:sundial-noon mesh/material-colors))
        names (mesh/site-object-names)]
    {:assigned
     (vec (concat
           (map #(mesh/assign-material! % terrace)
                (filter #(string/starts-with? % "site-demo-patio-") names))
           (map #(mesh/assign-material! % furniture)
                (filter #(string/starts-with? % "site-furniture") names))
           (map #(mesh/assign-material! % sundial-m)
                (filter #(string/starts-with? % "site-sundial-face") names))
           (map #(mesh/assign-material! % sundial-m)
                (filter #(string/starts-with? % "site-sundial-gnomon") names))
           (map #(mesh/assign-material! % mark-m)
                (filter #(and (string/starts-with? % "site-sundial-hour")
                              (not= % "site-sundial-hour-12"))
                        names))
           (map #(mesh/assign-material! % noon-m)
                (filter #(= % "site-sundial-hour-12") names))))}))

(defn- remove-fly-objects! []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-fly-")
                (mesh/site-object-names))))

(defn- set-bezier-points! [curve-obj points]
  (let [spline (aget (.-splines (.-data curve-obj)) 0)
        bps (.-bezier-points spline)
        need (count points) have (count bps)]
    (when (< have need) (.add bps (- need have)))
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
  (.empty-add (.-object (.-ops bpy)) **
              :type "SPHERE"
              :location #py [(double x) (double y) (double z)])
  (let [look (.-object (.-context bpy))]
    (set! (.-name look) "site-fly-lookat")
    (set! (.-empty-display-size look) 0.4)
    look))

(defn- add-fly-camera! [path look offset-keys]
  (.camera-add (.-object (.-ops bpy)) ** :location #py [0.0 0.0 0.0])
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
        r 22.0
        h 12.0
        end-frame 250
        path-pts (mapv (fn [i]
                         (let [θ (* 2.0 math/pi (/ (double i) (double n-pts)))]
                           [(* r (math/cos θ)) (* r (math/sin θ)) h]))
                       (range n-pts))
        offset-keys [[1 0.0] [end-frame 1.0]]
        path (add-fly-path! path-pts)
        look (add-fly-lookat! [0.0 0.0 1.2])
        cam (add-fly-camera! path look offset-keys)
        scene (.-scene (.-context bpy))]
    (set! (.-camera scene) cam)
    (set! (.-frame-start scene) 1)
    (set! (.-frame-end scene) end-frame)
    (set! (.-frame-current scene) 1)
    (view-fly-camera!)
    {:path "site-fly-path"
     :lookat "site-fly-lookat"
     :camera "site-fly-camera"
     :frames [1 end-frame]}))

(defn set-demo-time! [time-str]
  (let [s (demo-facts)
        {:keys [site] :as r} (sun/aim-sun-at-clock s time-str)
        s' site]
    (reset! demo-facts* s')
    (furniture/orient-loungers-to-sun! s')
    (paint-demo! s')
    (dissoc r :site)))

(defn- register-ui! []
  (require 'yardcraft.site-ui)
  ((ns-resolve 'yardcraft.site-ui 'register!)))

(defn ensure-demo! []
  (site/clear-site!)
  (reset-demo-facts!)
  (let [s (demo-facts)
        patios (build-word-patios!)
        _ (furniture/ensure-terrace-furniture! s)
        sundial (ensure-demo-sundial! s)
        world-r (sun/ensure-world! s)
        aimed (sun/aim-sun-at-clock s (:sun/time-of-day s))
        s' (:site aimed)
        _ (reset! demo-facts* s')
        sun-r (sun/ensure-sun! s')
        painted (paint-demo! s')
        fly (ensure-orbit-fly!)]
    (viewport/hide-relationship-lines!)
    (viewport/show-rendered!)
    (let [ui-r (register-ui!)]
      {:patios patios :sundial sundial :sun sun-r :world world-r
       :aim (dissoc aimed :site) :paint painted :fly fly :ui ui-r})))
