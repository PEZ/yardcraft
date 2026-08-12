(ns yardcraft.site-demo
  "Welcome demo: YARDCRAFT patio letters, brick CRAFT deck, lawn furniture, sundial, orbit fly.

  `(ensure-demo!)` after REPL connect — README onboarding scene.
  Objects: site-demo-*, site-furniture-*, site-sundial-*, site-fly-*.
  Does not require yardcraft.site-ui at ns level (avoid cycles)."
  (:require [basilisp.string :as string]
            [yardcraft.site :as site]
            [yardcraft.site-demo-fly :as demo-fly]
            [yardcraft.site-furniture :as furniture]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-railing :as railing]
            [yardcraft.site-sun :as sun]
            [yardcraft.site-viewport :as viewport])
  (:import bpy math mathutils))

(def ^:private cell-m 0.5)
(def ^:private slab-thick 0.14)
(def ^:private glyph-rows 7)
(def ^:private glyph-cols 5)
(def ^:private letter-gap-m 0.5)
(def ^:private letter-w (* glyph-cols cell-m))
(def ^:private letter-h (* glyph-rows cell-m))
(def ^:private brick-h (* 2 cell-m))
(def ^:private yard-craft-gap-m 1.6)
(def ^:private word "YARDCRAFT")
(def ^:private stone-rgb [0.32 0.32 0.34])
(def ^:private tegelrod-rgb [0.72 0.28 0.22])

(def ^:private glyphs
  {"Y" ["10001" "10001" "01010" "00100" "00100" "00100" "00100"]
   "A" ["00100" "01010" "10001" "10001" "11111" "10001" "10001"]
   "R" ["11110" "10001" "10001" "11110" "10100" "10010" "10001"]
   "D" ["11100" "10010" "10001" "10001" "10001" "10010" "11100"]
   "C" ["01110" "10001" "10000" "10000" "10000" "10001" "01110"]
   "F" ["11111" "10000" "10000" "11110" "10000" "10000" "10000"]
   "T" ["11111" "00100" "00100" "00100" "00100" "00100" "00100"]})

(def ^:private altan-furniture-origins
  [[3.8 -10.5] [7.2 -8.85] [-4 -11.5] [10 -11.5] [11.4 -11.5] [-0.5 -12.5]])

(defonce ^:private demo-facts* (atom nil))
(defonce ^:private demo-terrain-bounds* (atom nil))

(defn- yard-block-width-m []
  (+ (* 4 letter-w) (* 3 letter-gap-m)))

(defn- craft-block-width-m []
  (+ (* 5 letter-w) (* 4 letter-gap-m)))

(defn- word-width-m []
  (+ (yard-block-width-m) yard-craft-gap-m (craft-block-width-m)))

(defn- word-start-x []
  (- (/ (word-width-m) 2.0)))

(defn- word-bottom-y []
  (- (/ letter-h 2.0)))

(defn- word-top-y []
  (/ letter-h 2.0))

(defn- letter-start-x [idx]
  (if (< idx 4)
    (+ (word-start-x) (* idx (+ letter-w letter-gap-m)))
    (+ (word-start-x)
       (yard-block-width-m)
       yard-craft-gap-m
       (* (- idx 4) (+ letter-w letter-gap-m)))))

(defn- cell-center-xy [letter-idx col row]
  (let [lx (+ (letter-start-x letter-idx) (* col cell-m) (/ cell-m 2.0))
        bottom-y (word-bottom-y)
        ly (+ bottom-y (* (- (dec glyph-rows) row) cell-m) (/ cell-m 2.0))]
    [lx ly]))

(defn- craft-max-x []
  (+ (letter-start-x 8) letter-w))

(defn- furniture-transform []
  (let [anchor-y -8.85
        target-cafe-y (- (word-bottom-y) 1.2)
        δy (- target-cafe-y anchor-y)
        xs (map first altan-furniture-origins)
        mid-x (/ (reduce + xs) (count xs))
        δx (- mid-x)]
    {:δx δx :δy δy}))

(defn- tx-xy [[x y] {:keys [δx δy]}]
  [(+ x δx) (+ y δy)])

(def ^:private furniture-y-pull 0.75)

(def ^:private cafe-origin [7.2 -8.85])

(defn- place-furniture-xy
  "World XY after furniture transform; non-cafe Y pulled toward cafe."
  [origin t]
  (let [cafe (vec (tx-xy cafe-origin t))
        [_ cfy] cafe
        [x y] (tx-xy origin t)]
    (if (= origin cafe-origin)
      cafe
      [x (+ cfy (* furniture-y-pull (- y cfy)))])))

(defn- furniture-world-anchors
  "World XY of furniture anchors after transform + Y-pull (cafe unscaled)."
  []
  (let [t (furniture-transform)]
    (mapv #(place-furniture-xy % t) altan-furniture-origins)))

(defn- terrain-bounds []
  (let [fpts (furniture-world-anchors)
        f-min-x (apply min (map first fpts))
        f-max-x (apply max (map first fpts))
        f-min-y (apply min (map second fpts))
        margin-x (* 0.75 letter-w)
        margin-n letter-w
        margin-s 1.0
        min-x (- (min (word-start-x) f-min-x) margin-x)
        max-x (+ (max (+ (word-start-x) (word-width-m)) f-max-x) margin-x)
        min-y (- f-min-y margin-s)
        max-y (+ (word-top-y) margin-n)]
    {:min-x min-x :max-x max-x :min-y min-y :max-y max-y}))

(def ^:private altan-furniture-raw
  {:furniture/leg-size-m 0.045
   :furniture/leg-inset-m 0.05
   :furniture/dining
   {:xy [3.8 -10.5]
    :rot-z-deg 0
    :table {:xy [0 0]
            :rot-z-deg 0
            :size-m [2.2 1.0]
            :height-m 0.75
            :top-thickness-m 0.04}
    :chair {:seat-size-m [0.45 0.48]
            :seat-height-m 0.45
            :seat-thickness-m 0.04
            :back-height-m 0.42
            :back-thickness-m 0.03
            :clearance-m 0.12}}
   :furniture/cafe
   {:xy [7.2 -8.85]
    :rot-z-deg 0
    :table {:xy [0 0]
            :rot-z-deg 0
            :size-m [0.7 0.7]
            :height-m 0.75
            :top-thickness-m 0.04}
    :chair {:seat-size-m [0.40 0.45]
            :seat-height-m 0.45
            :seat-thickness-m 0.04
            :back-height-m 0.4
            :back-thickness-m 0.03
            :clearance-m 0.1}
    :chairs [{:xy [0.6 0] :rot-z-deg 0} {:xy [-0.6 0] :rot-z-deg 0}]}
   :furniture/lounger
   {:size-m [0.65 1.20]
    :seat-size-m [0.65 1.20]
    :seat-height-m 0.35
    :seat-thickness-m 0.08
    :back-height-m 0.55
    :back-thickness-m 0.06
    :back-tilt-deg 55
    :side-table {:diameter-m 0.45
                 :height-m 0.35
                 :xy [-0.60 0.30]}
    :placements [{:xy [-4 -11.5] :with-side-table? true}
                 {:xy [10 -11.5] :with-side-table? true}
                 {:xy [11.4 -11.5] :with-side-table? true}]}
   :furniture/bbq
   {:body-size-m [1.15 0.55]
    :body-height-m 0.95
    :lid-height-m 0.12
    :shelf-size-m [0.48 0.5]
    :shelf-thickness-m 0.04
    :shelf-height-m 0.88
    :shelf-side :west
    :xy [-0.5 -12.5]
    :rot-z-deg 0}})

(defn- relocate-furniture
  "Translate by t; keep cafe XY; pull other pieces toward cafe in Y."
  [facts t]
  (let [place #(place-furniture-xy % t)]
    (-> facts
        (update-in [:furniture/dining :xy] place)
        (update-in [:furniture/cafe :xy] place)
        (update-in [:furniture/bbq :xy] place)
        (update-in [:furniture/lounger :placements]
                   (fn [ps] (mapv #(update % :xy place) ps))))))

(defn- altan-furniture-facts [s]
  (merge s (relocate-furniture altan-furniture-raw (furniture-transform))))

(defn- base-demo-facts []
  (altan-furniture-facts
   {:site/lat-deg 59.3
    :site/lon-deg 18.0
    :site/timezone "Europe/Stockholm"
    :site/north-offset-deg 0.0
    :sun/date "2026-06-21"
    :sun/time-of-day "13:00"
    :house/floor-z 0.0
    :terrace/slab-thickness-m 0.0
    :terrace/roof-opacity 1.0
    :sundial/radius-m 0.4
    :world/color [0.45 0.52 0.65]
    :world/strength 0.15}))

(defn- reset-demo-facts! []
  (reset! demo-facts* (base-demo-facts)))

(defn demo-facts []
  (or @demo-facts* (base-demo-facts)))

(defn demo-active? []
  (boolean (some #(string/starts-with? % "site-demo-")
                 (mesh/site-object-names))))

(defn- demo-world-bounds []
  (or @demo-terrain-bounds* (terrain-bounds)))

(defn frame-demo!
  "Top ortho framing the demo terrain/word bounds (north up)."
  []
  (viewport/frame-world-rect-top! (demo-world-bounds)))

(defn- terrain-grid-verts [{:keys [min-x max-x min-y max-y]}]
  (let [cols 11 rows 3
        xs (mapv #(+ min-x (* % (/ (- max-x min-x) (double (dec cols)))))
                 (range cols))
        ys (mapv #(+ min-y (* % (/ (- max-y min-y) (double (dec rows)))))
                 (range rows))]
    (for [y ys x xs]
      [x y 0.0])))

(defn- terrain-grid-faces []
  (let [cols 11 rows 3
        idx (fn [c r] (+ (* r cols) c))]
    (for [r (range (dec rows)) c (range (dec cols))]
      [(idx c r) (idx (inc c) r) (idx (inc c) (inc r)) (idx c (inc r))])))

(defn- build-terrain! []
  (let [bounds (terrain-bounds)]
    (reset! demo-terrain-bounds* bounds)
    (mesh/add-mesh! "site-demo-terrain"
                    (vec (terrain-grid-verts bounds))
                    (vec (terrain-grid-faces)))
    "site-demo-terrain"))

(defn- build-letter-patios! [letter-idx ch z0]
  (let [rows (get glyphs ch)]
    (vec
     (for [row (range glyph-rows)
           col (range glyph-cols)
           :when (= (subs (nth rows row) col (inc col)) "1")
           :let [n (str "site-demo-patio-" letter-idx "-" row "-" col)
                 [cx cy] (cell-center-xy letter-idx col row)
                 center (mesh/box-center [cell-m cell-m slab-thick] [cx cy] z0)]]
       (do
         (mesh/add-box! n [cell-m cell-m slab-thick] center)
         n)))))

(defn- build-yard-patios! []
  (vec (mapcat #(build-letter-patios! % (subs word % (inc %)) 0.0)
               (range 4))))

(defn- build-craft-patios! []
  (vec (mapcat #(build-letter-patios! % (subs word % (inc %)) brick-h)
               (range 4 (count word)))))

(defn- build-brick! []
  (let [min-x (letter-start-x 4)
        max-x (craft-max-x)
        w (- max-x min-x)
        cx (/ (+ min-x max-x) 2.0)
        cy (/ (+ (word-bottom-y) (word-top-y)) 2.0)
        center [cx cy (/ brick-h 2.0)]]
    (mesh/add-box! "site-demo-brick" [w letter-h brick-h] center)
    "site-demo-brick"))

(defn- d-stair-y-span []
  (let [rows [2 3 4]
        ys (map #(second (cell-center-xy 3 4 %)) rows)
        half (/ cell-m 2.0)]
    [(- (apply min ys) half)
     (+ (apply max ys) half)]))

(defn- add-stair-flight!
  "Steps descending along dir from z-top. :west/:east use X as run axis."
  [prefix {:keys [dir a0 a1 start-b run drop steps]} z-top]
  (let [span (- a1 a0)
        run-i (/ run steps)
        drop-i (/ drop steps)
        names (atom [])]
    (doseq [i (range steps)]
      (let [sz (max 0.06 drop-i)
            mid-a (/ (+ a0 a1) 2.0)
            b-off (* (+ i 0.5) run-i)
            b (case dir
                (:west :south) (- start-b b-off)
                (+ start-b b-off))
            z-tread (- z-top (* (inc i) drop-i))
            cz (+ z-tread (/ sz 2.0))
            n (str prefix "-" i)
            [sx sy cx cy] (case dir
                            (:east :west) [run-i span b mid-a]
                            (:north :south) [span run-i mid-a b])]
        (mesh/add-box! n [sx sy sz] [cx cy cz])
        (swap! names conj n)))
    @names))

(defn- build-stairs-and-railings! [s]
  (let [[y-min y-max] (d-stair-y-span)
        d-right (+ (letter-start-x 3) letter-w)
        c-left (letter-start-x 4)
        z-top (+ brick-h slab-thick)
        z-bot slab-thick
        opts (railing/railing-opts s)
        inset (:inset opts)
        steps 5
        stair-names (add-stair-flight! "site-demo-stair"
                                       {:dir :west
                                        :a0 y-min :a1 y-max
                                        :start-b c-left
                                        :run yard-craft-gap-m
                                        :drop brick-h
                                        :steps steps}
                                       z-top)
        rail-n (railing/add-stair-side-railing!
                "site-railing-demo-stair-n" steps
                {:x0 d-right :y0 (- y-max inset) :z0 z-bot
                 :x1 c-left :y1 (- y-max inset) :z1 z-top}
                opts)
        rail-s (railing/add-stair-side-railing!
                "site-railing-demo-stair-s" steps
                {:x0 d-right :y0 (+ y-min inset) :z0 z-bot
                 :x1 c-left :y1 (+ y-min inset) :z1 z-top}
                opts)]
    {:stairs stair-names :railings (vec (concat rail-n rail-s))}))

(defn- build-pedestal! []
  (let [radius 0.45
        height (+ brick-h slab-thick)
        cx (+ (craft-max-x) (* 0.75 cell-m) radius)
        cy (- (word-bottom-y) (* 0.25 cell-m))
        cz (/ height 2.0)]
    (mesh/add-cylinder! "site-demo-pedestal" radius height [cx cy cz])
    {:name "site-demo-pedestal" :cx cx :cy cy :height height :radius radius}))

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

(defn- ensure-demo-sundial! [s pedestal]
  (clear-sundial!)
  (let [{:keys [cx cy height]} pedestal
        face-radius (:sundial/radius-m s 0.4)
        z (+ height 0.02)
        [nx ny] (true-north-xy s)
        lat (math/radians (:site/lat-deg s))
        _ (add-sundial-face! cx cy z face-radius)
        _ (add-sundial-gnomon! {:cx cx :cy cy :z z :radius face-radius
                                :nx nx :ny ny :lat lat})
        hours (mapv
               (fn [h]
                 (let [H (math/radians (* 15.0 (- h 12.0)))
                       θ (math/atan2 (* (math/sin H) (math/sin lat)) (math/cos H))
                       lx (+ (* nx (math/cos θ)) (* ny (math/sin θ)))
                       ly (+ (* ny (math/cos θ)) (* (- nx) (math/sin θ)))
                       r0 (* face-radius 0.55) r1 (* face-radius 0.95)]
                   (hour-tick! {:cx cx :cy cy :z z :h h :lx lx :ly ly
                                :r0 r0 :r1 r1 :noon? (= h 12)})))
               (range 4 22))]
    {:face "site-sundial-face" :gnomon "site-sundial-gnomon" :hours hours}))

(defn- patio-letter-idx [name]
  (some-> (re-find #"site-demo-patio-(\d+)-" name) second parse-long))

(defn- paint-demo! [_s]
  (let [terrain-m (mesh/ensure-material! "site-mat-demo-terrain"
                                         (:terrain mesh/material-colors)
                                         :roughness 0.9)
        stone-m (mesh/ensure-material! "site-mat-demo-stone" stone-rgb :roughness 0.9)
        terrace-m (mesh/ensure-material! "site-mat-terrace"
                                         (:terrace mesh/material-colors)
                                         :roughness 0.85)
        tegelrod-m (mesh/ensure-material! "site-mat-demo-tegelrod"
                                           tegelrod-rgb :roughness 0.85)
        furniture-m (mesh/ensure-material! "site-mat-furniture"
                                           (:furniture mesh/material-colors)
                                           :roughness 0.85)
        railing-m (mesh/ensure-material! "site-mat-railing"
                                         (:railing mesh/material-colors)
                                         :roughness 0.85)
        sundial-m (mesh/ensure-material! "site-mat-sundial" (:sundial mesh/material-colors))
        mark-m (mesh/ensure-material! "site-mat-sundial-mark" (:sundial-mark mesh/material-colors))
        noon-m (mesh/ensure-material! "site-mat-sundial-noon" (:sundial-noon mesh/material-colors))
        names (mesh/site-object-names)]
    {:assigned
     (vec (concat
           (keep #(mesh/assign-material! % terrain-m)
                 (filter #(= % "site-demo-terrain") names))
           (keep #(mesh/assign-material! % stone-m)
                 (filter #(= % "site-demo-pedestal") names))
           (keep #(mesh/assign-material! % tegelrod-m)
                 (filter #(or (= % "site-demo-brick")
                              (string/starts-with? % "site-demo-stair-"))
                         names))
           (mapcat
            (fn [n]
              (when-let [idx (patio-letter-idx n)]
                [(mesh/assign-material! n (if (< idx 4) stone-m terrace-m))]))
            (filter #(string/starts-with? % "site-demo-patio-") names))
           (map #(mesh/assign-material! % furniture-m)
                (filter #(string/starts-with? % "site-furniture") names))
           (map #(mesh/assign-material! % railing-m)
                (filter #(string/starts-with? % "site-railing") names))
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

(defn ensure-orbit-fly! []
  (demo-fly/ensure-orbit-fly!))

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
  (railing/clear-railings!)
  (let [s (demo-facts)
        terrain (build-terrain!)
        yard (build-yard-patios!)
        brick (build-brick!)
        craft (build-craft-patios!)
        stairs (build-stairs-and-railings! s)
        pedestal (build-pedestal!)
        sundial (ensure-demo-sundial! s pedestal)
        furn (furniture/ensure-terrace-furniture! s)
        world-r (sun/ensure-world! s)
        aimed (sun/aim-sun-at-clock s (:sun/time-of-day s))
        s' (:site aimed)
        _ (reset! demo-facts* s')
        sun-r (sun/ensure-sun! s')
        painted (paint-demo! s')
        fly (demo-fly/ensure-orbit-fly!)]
    (viewport/hide-relationship-lines!)
    (viewport/show-rendered!)
    (let [ui-r (register-ui!)]
      {:terrain terrain
       :yard yard
       :brick brick
       :craft craft
       :stairs stairs
       :pedestal pedestal
       :sundial sundial
       :furniture furn
       :sun sun-r
       :world world-r
       :aim (dissoc aimed :site)
       :paint painted
       :fly fly
       :ui ui-r})))
