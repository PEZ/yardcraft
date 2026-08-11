(ns yardcraft.site-plan
  "Contractor quote plan: dimensioned SVG from site facts (no bpy).

  Pass the facts map you want rendered — typically
  `(yardcraft.site-suggestions/effective-site site)` so an active suggestion
  is included. Re-run write-quote-plan! after show!/show-base!/data edits."
  (:require [basilisp.string :as string]
            [yardcraft.site-house :as house]
            [yardcraft.site-terrace :as terrace]
            [yardcraft.site-driveway :as driveway]
            [yardcraft.site-mesh :as mesh])
  (:import math os))

(def ^:private default-svg-path "out/quote-plan.svg")

(defn- rgb->hex
  [[r g b]]
  (format "#%02x%02x%02x"
          (int (+ (* 255.0 (double r)) 0.5))
          (int (+ (* 255.0 (double g)) 0.5))
          (int (+ (* 255.0 (double b)) 0.5))))

(defn- fmt-m [x] (format "%.2f" (double x)))
(defn- fmt-deg [x] (format "%.1f°" (double x)))

(defn- xml-esc
  [s]
  (-> (str s)
      (string/replace "&" "&amp;")
      (string/replace "<" "&lt;")
      (string/replace ">" "&gt;")
      (string/replace "\"" "&quot;")))

(defn- fp->ring
  "AABB → CCW ring from SW."
  [{:keys [min-x min-y max-x max-y]}]
  [[min-x min-y] [max-x min-y] [max-x max-y] [min-x max-y]])

(defn- ring-area
  [ring]
  (let [pts (conj (vec ring) (first ring))]
    (* 0.5 (math/fabs
            (double (reduce + (map (fn [[a b]]
                                     (let [[x0 y0] a
                                           [x1 y1] b]
                                       (- (* x0 y1) (* x1 y0))))
                                   (map vector pts (rest pts)))))))))

(defn- terrace-outline-xy
  "Outer terrace silhouette (CCW) from L-shaped piece footprints."
  [s]
  (let [west (terrace/terrace-west-footprint s)
        bridge (terrace/terrace-west-bridge-footprint s)
        bod-s (terrace/terrace-bod-south-footprint s)
        south (terrace/terrace-south-footprint s)]
    [[(:min-x bod-s) (:min-y south)]
     [(:max-x south) (:min-y south)]
     [(:max-x south) (:max-y south)]
     [(:min-x south) (:max-y south)]
     [(:max-x bridge) (:max-y bridge)]
     [(:min-x bridge) (:max-y bridge)]
     [(:min-x bridge) (:min-y bridge)]
     [(:min-x west) (:max-y west)]
     [(:max-x bod-s) (:max-y bod-s)]
     [(:min-x bod-s) (:max-y bod-s)]]))

(defn- terrace-east-low-ring
  [s]
  (when-let [fp (terrace/terrace-east-low-footprint s)]
    (fp->ring fp)))

(defn- stairs-west-footprint
  [s]
  (let [bridge (terrace/terrace-west-bridge-footprint s)
        run (get-in s [:stairs/west :run-m] 0.8)]
    {:min-x (:min-x bridge) :max-x (:max-x bridge)
     :min-y (:max-y bridge) :max-y (+ (:max-y bridge) run)}))

(defn- driveway-wall-footprint
  "Plan strip of the east retaining wall (full driveway east edge × thickness)."
  [s]
  (let [th (:driveway/east-wall-thickness-m s 0.2)
        [ne _ _ se] (driveway/driveway-polygon-xy s)
        xw (first ne)]
    {:min-x xw :max-x (+ xw th)
     :min-y (second se) :max-y (second ne)}))

(defn- bod-ring
  [s]
  (let [[nw ne se sw] (house/bod-footprint-xy s)]
    [sw se ne nw]))

(defn- driveway-ring
  [s]
  (let [[ne nw sw se] (driveway/driveway-polygon-xy s)]
    [sw se ne nw]))

(defn- edge-info
  [[x0 y0] [x1 y1]]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        len (math/hypot dx dy)
        ox (/ dy len)
        oy (- (/ dx len))]
    {:a [x0 y0] :b [x1 y1]
     :mid [(/ (+ x0 x1) 2.0) (/ (+ y0 y1) 2.0)]
     :len len :ox ox :oy oy}))

(defn- ring-edges
  [ring]
  (mapv edge-info ring (concat (rest ring) [(first ring)])))

(defn- skew-angles
  [ring]
  (let [n (count ring)
        ang (fn [a b c]
              (let [[ax ay] a
                    [bx by] b
                    [cx cy] c
                    ux (- ax bx) uy (- ay by)
                    vx (- cx bx) vy (- cy by)
                    du (math/hypot ux uy) dv (math/hypot vx vy)
                    cos (/ (+ (* ux vx) (* uy vy)) (* du dv))]
                (* (/ 180.0 math/pi) (math/acos (max -1.0 (min 1.0 cos))))))]
    (into []
          (keep (fn [i]
                  (let [deg (ang (nth ring (mod (dec i) n))
                                 (nth ring i)
                                 (nth ring (mod (inc i) n)))]
                    (when (> (math/fabs (- deg 90.0)) 0.5)
                      {:at (nth ring i) :deg deg})))
                (range n)))))

(defn- ring-centroid
  [ring]
  [(/ (reduce + (map first ring)) (double (count ring)))
   (/ (reduce + (map second ring)) (double (count ring)))])

(defn- ray-crosses-edge?
  "Even-odd ray cast: does horizontal ray from (x,y) cross edge i→j?"
  [x y [xi yi] [xj yj]]
  (and (not= (> yi y) (> yj y))
       (< x (+ xi (/ (* (- xj xi) (- y yi)) (- yj yi))))))

(defn- point-in-ring?
  "Ray-cast even-odd for a closed ring (last≠first OK)."
  [ring [x y]]
  (let [pts (vec ring)
        edges (map vector pts (concat (rest pts) [(first pts)]))]
    (->> edges
         (filter (fn [[[xi yi] [xj yj]]]
                   (ray-crosses-edge? x y [xi yi] [xj yj])))
         count
         odd?)))

(defn- label-anchor
  "Point inside ring for area labels (centroid, else south-footprint fallback)."
  [ring fallback]
  (let [c (ring-centroid ring)]
    (if (point-in-ring? ring c) c fallback)))

(defn- svg-points
  [ring]
  (->> ring
       (map (fn [[x y]] (str x "," (- y))))
       (string/join " ")))

(defn- svg-polygon
  [ring fill {:keys [stroke sw extra]
              :or {stroke "#222" sw 0.07 extra ""}}]
  (str "<polygon points=\"" (svg-points ring) "\" fill=\"" fill
       "\" stroke=\"" stroke "\" stroke-width=\"" sw "\""
       extra "/>"))

(defn- svg-line
  [[x0 y0] [x1 y1] {:keys [stroke sw]}]
  (str "<line x1=\"" x0 "\" y1=\"" (- y0) "\" x2=\"" x1 "\" y2=\"" (- y1)
       "\" stroke=\"" stroke "\" stroke-width=\"" sw
       "\" stroke-linecap=\"square\"/>"))

(defn- rail-line-svg
  [{:keys [x0 y0 x1 y1]} stroke]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        len (math/hypot dx dy)
        ox (/ dy len)
        oy (- (/ dx len))
        o 0.06]
    (svg-line [(+ x0 (* o ox)) (+ y0 (* o oy))]
              [(+ x1 (* o ox)) (+ y1 (* o oy))]
              {:stroke stroke :sw 0.14})))

(defn- svg-text
  "Upright text at house-NW (x,y); SVG y is flipped, no scale(1,-1)."
  [x y content attrs]
  (str "<text x=\"" x "\" y=\"" (- y) "\" " attrs ">" (xml-esc content) "</text>"))

(defn- inside-label-xy
  "Place length label just inside the ring at the edge midpoint."
  [ring {:keys [mid ox oy]} offset]
  (let [[mx my] mid
        ;; ox,oy are outward normals for CCW rings
        inward [(- mx (* offset ox)) (- my (* offset oy))]
        outward [(+ mx (* offset ox)) (+ my (* offset oy))]]
    (cond
      (point-in-ring? ring inward) inward
      (point-in-ring? ring outward) outward
      :else mid)))

(defn- outside-label-xy
  "Place length label just outside the ring at the edge midpoint."
  [ring {:keys [mid ox oy]} offset]
  (let [[mx my] mid
        outward [(+ mx (* offset ox)) (+ my (* offset oy))]
        inward [(- mx (* offset ox)) (- my (* offset oy))]]
    (cond
      (not (point-in-ring? ring outward)) outward
      (not (point-in-ring? ring inward)) inward
      :else outward)))

(defn- near-pt?
  [[x0 y0] [x1 y1] eps]
  (< (math/hypot (- x0 x1) (- y0 y1)) eps))

(defn- same-edge?
  [e0 e1 eps]
  (or (and (near-pt? (:a e0) (:a e1) eps) (near-pt? (:b e0) (:b e1) eps))
      (and (near-pt? (:a e0) (:b e1) eps) (near-pt? (:b e0) (:a e1) eps))))

(defn- edge-on-rings?
  [e rings eps]
  (boolean (some (fn [ring]
                   (some #(same-edge? e % eps) (ring-edges ring)))
                 rings)))

(defn- labelable-edge?
  [e skip-rings]
  (and (> (:len e) 0.3)
       (or (empty? skip-rings)
           (not (edge-on-rings? e skip-rings 0.08)))))

(defn- stair-label-side?
  [{:keys [ox oy]}]
  (or (> ox 0.5) (< oy -0.5)))

(defn- edge-label-svgs
  ([ring offset] (edge-label-svgs ring offset nil "#111"))
  ([ring offset skip-rings] (edge-label-svgs ring offset skip-rings "#111"))
  ([ring offset skip-rings fill]
   (for [e (ring-edges ring)
         :when (labelable-edge? e skip-rings)
         :let [[lx ly] (inside-label-xy ring e offset)]]
     (svg-text lx ly (fmt-m (:len e))
               (str "font-size=\"0.36\" text-anchor=\"middle\" dominant-baseline=\"middle\" fill=\""
                    fill "\"")))))

(defn- stair-edge-label-svgs
  "Label south/east stair edges outside the stair AABB (readable on hatch-free fill)."
  ([ring offset] (stair-edge-label-svgs ring offset nil))
  ([ring offset skip-rings]
   (for [e (ring-edges ring)
         :when (and (labelable-edge? e skip-rings) (stair-label-side? e))
         :let [[lx ly] (outside-label-xy ring e offset)]]
     (svg-text lx ly (fmt-m (:len e))
               "font-size=\"0.34\" text-anchor=\"middle\" dominant-baseline=\"middle\" fill=\"#111\""))))

(defn- area-label-svg
  [_ring label fill anchor]
  (let [[cx cy] anchor]
    (svg-text cx cy label
              (str "font-size=\"0.42\" text-anchor=\"middle\" dominant-baseline=\"middle\" fill=\""
                   fill "\" font-weight=\"600\""))))

(defn- angle-label-svgs
  [ring]
  (for [{:keys [at deg]} (skew-angles ring)
        :let [[x y] at
              ;; nudge outside using average of adjacent edge outward normals
              e0 (first (filter #(= (:b %) at) (ring-edges ring)))
              e1 (first (filter #(= (:a %) at) (ring-edges ring)))
              ox (/ (+ (get e0 :ox 0.0) (get e1 :ox 0.0)) 2.0)
              oy (/ (+ (get e0 :oy 0.0) (get e1 :oy 0.0)) 2.0)
              [lx ly] (inside-label-xy ring {:mid [x y] :ox ox :oy oy} 0.7)]]
    (svg-text lx ly (fmt-deg deg)
              "font-size=\"0.32\" text-anchor=\"middle\" dominant-baseline=\"middle\" fill=\"#a33000\"")))

(defn- parent-dir
  [file-path]
  (when-let [i (string/last-index-of file-path "/")]
    (when (pos? i)
      (subs file-path 0 i))))

(defn- role-hex
  [k]
  (rgb->hex (get mesh/material-colors k)))

(defn plan-model
  [s]
  (let [terr (terrace-outline-xy s)
        low (terrace-east-low-ring s)
        drive (driveway-ring s)
        canopy (fp->ring (terrace/terrace-roof-footprint s))]
    {:terrace-ring terr
     :terrace-low-ring low
     :driveway-ring drive
     :canopy-ring canopy
     :areas {:terrace-m2 (ring-area terr)
             :terrace-low-m2 (if low (ring-area low) 0.0)
             :driveway-m2 (ring-area drive)
             :canopy-m2 (ring-area canopy)}}))

(defn- plan-colors
  []
  {:house (role-hex :house)
   :bod (role-hex :house)  ;; same as house for quote plan
   :terrace (role-hex :terrace)
   :driveway (role-hex :driveway)
   :wall (role-hex :driveway-wall)
   :stairs (role-hex :stairs)
   :canopy (role-hex :terrace-roof)
   :rail (role-hex :railing)})

(defn- east-low-rail-runs
  [s]
  (when-let [low (terrace/terrace-east-low-footprint s)]
    [["site-railing-east-low-s"
      {:x0 (:min-x low) :y0 (:min-y low)
       :x1 (:max-x low) :y1 (:min-y low)}]
     ["site-railing-east-low-e"
      {:x0 (:max-x low) :y0 (:min-y low)
       :x1 (:max-x low) :y1 (:max-y low)}]]))

(defn- east-stair-rail-runs
  [s]
  (let [se (terrace/stairs-east-footprint s)
        dir (get-in s [:stairs/east :dir] :north)]
    (case dir
      :north
      [["site-railing-stair-east-e"
        {:x0 (:max-x se) :y0 (:min-y se)
         :x1 (:max-x se) :y1 (:max-y se)}]]
      ;; :east (and cutout) — rails on south + north sides of the flight
      [["site-railing-stair-east-s"
        {:x0 (:min-x se) :y0 (:min-y se)
         :x1 (:max-x se) :y1 (:min-y se)}]
       ["site-railing-stair-east-n"
        {:x0 (:min-x se) :y0 (:max-y se)
         :x1 (:max-x se) :y1 (:max-y se)}]])))

(defn- plan-rail-runs
  "Deck rails + east-low rails + east-stair side rails."
  [s]
  (vec (concat (terrace/railing-deck-runs s)
               (or (east-low-rail-runs s) [])
               (east-stair-rail-runs s))))

(defn- plan-geometry
  [s]
  (let [wf (driveway-wall-footprint s)
        xe (:max-x wf)
        thick (max 0.35 (- xe (:min-x wf)))
        ;; Exaggerate west into driveway; inset east so fill stays west of terrace
        wall-vis {:min-x (- xe thick)
                  :max-x (- xe 0.02)
                  :min-y (:min-y wf)
                  :max-y (:max-y wf)}]
    {:house (fp->ring (house/house-footprint s))
     :bod (bod-ring s)
     :drive (driveway-ring s)
     :terr (terrace-outline-xy s)
     :terr-low (terrace-east-low-ring s)
     :canopy (fp->ring (terrace/terrace-roof-footprint s))
     :st-e (fp->ring (terrace/stairs-east-footprint s))
     :st-w (fp->ring (stairs-west-footprint s))
     :wall-vis wall-vis
     :wall-core (fp->ring wf)
     :wall (fp->ring wall-vis)
     :wall-len (- (:max-y wf) (:min-y wf))
     :wf wf
     :rails (plan-rail-runs s)
     :south-c (ring-centroid (fp->ring (terrace/terrace-south-footprint s)))}))

(defn- plan-viewbox
  [rings]
  (let [all-pts (mapcat identity rings)
        pad 2.0
        xs (map first all-pts)
        ys (map (fn [[_ y]] (- y)) all-pts)
        min-x (- (apply min xs) pad)
        max-x (+ (apply max xs) pad)
        min-y (- (apply min ys) pad)
        max-y (+ (apply max ys) pad)]
    {:min-x min-x :min-y min-y
     :width (- max-x min-x) :height (- max-y min-y)}))

(defn- plan-fill-svgs
  [{:keys [drive wall terr terr-low house bod st-e st-w canopy]} colors]
  (concat
   [(svg-polygon drive (:driveway colors) {:stroke "#111" :sw 0.035})
    (svg-polygon wall (:wall colors) {:stroke "none" :sw 0})
    (svg-polygon terr (:terrace colors) {:stroke "#333" :sw 0.04})]
   (when terr-low
     [(svg-polygon terr-low (:terrace colors) {:stroke "#333" :sw 0.04})])
   [(svg-polygon house (:house colors) {:stroke "#222" :sw 0.045})
    (svg-polygon bod (:bod colors) {:stroke "#222" :sw 0.04})
    (svg-polygon st-e (:stairs colors) {:stroke "#222" :sw 0.04})
    (svg-polygon st-w (:stairs colors) {:stroke "#222" :sw 0.04})
    (svg-polygon canopy "none" {:stroke (:canopy colors) :sw 0.055
                                :extra " stroke-dasharray=\"0.3 0.15\""})]))

(defn- plan-label-svgs
  [{:keys [house drive terr terr-low st-e st-w wall-core wall-vis wf wall-len south-c canopy rails]}
   colors areas
   {:keys [show-angles?] :or {show-angles? false}}]
  (let [terr-anchor (label-anchor terr south-c)
        drive-anchor (label-anchor drive (ring-centroid drive))
        canopy-anchor (let [[cx cy] (ring-centroid canopy)]
                        [cx (- cy 0.9)])
        low-anchor (when terr-low (ring-centroid terr-low))
        wall-label-y (/ (+ (max (:min-y wf) 1.0) (:max-y wf)) 2.0)]
    (concat
     (for [[_ run] rails]
       (rail-line-svg run (:rail colors)))
     (edge-label-svgs house 0.5)
     (edge-label-svgs terr 0.5 [house])
     (when terr-low (edge-label-svgs terr-low 0.5 [terr st-e]))
     (edge-label-svgs drive 0.5 [wall-core] "#f0f0f0")
     (stair-edge-label-svgs st-e 0.35 [terr])
     (stair-edge-label-svgs st-w 0.35 [terr])
     (when show-angles? (angle-label-svgs drive))
     [(area-label-svg drive
                       (str (fmt-m (:driveway-m2 areas)) " m²")
                       "#f0f0f0"
                       drive-anchor)
      (area-label-svg terr
                      (str (fmt-m (:terrace-m2 areas)) " m²")
                      "#1a1a1a"
                      terr-anchor)
      (area-label-svg canopy
                      (str (fmt-m (:canopy-m2 areas)) " m²")
                      "#1a1a1a"
                      canopy-anchor)
      (svg-text (- (:min-x wall-vis) 0.15)
                wall-label-y
                (str "wall " (fmt-m wall-len) " m")
                "font-size=\"0.32\" text-anchor=\"end\" dominant-baseline=\"middle\" fill=\"#f0f0f0\"")]
     (when low-anchor
       [(area-label-svg terr-low
                         (str (fmt-m (:terrace-low-m2 areas)) " m²")
                         "#1a1a1a"
                         low-anchor)]))))

(defn render-svg
  ([s] (render-svg s {}))
  ([s opts]
   (let [colors (plan-colors)
         geo (plan-geometry s)
         base-rings [(:house geo) (:bod geo) (:drive geo) (:terr geo)
                     (:canopy geo) (:st-e geo) (:st-w geo) (:wall geo)]
         view-rings (if (:terr-low geo)
                      (conj base-rings (:terr-low geo))
                      base-rings)
         {:keys [min-x min-y width height]} (plan-viewbox view-rings)
         areas (:areas (plan-model s))
         body (concat (plan-fill-svgs geo colors)
                      (plan-label-svgs geo colors areas opts))]
     (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\""
          min-x " " min-y " " width " " height
          "\" width=\"1400\" height=\"900\">\n"
          "<rect x=\"" min-x "\" y=\"" min-y "\" width=\"" width
          "\" height=\"" height "\" fill=\"#f4f2ec\"/>\n"
          (string/join "\n" body)
          "\n</svg>\n"))))

(defn write-quote-plan!
  "Write contractor quote-plan SVG for facts map s.
  opts: {:show-angles? bool} (default false). Returns {:path :areas :bytes}."
  ([s] (write-quote-plan! s default-svg-path {}))
  ([s file-path] (write-quote-plan! s file-path {}))
  ([s file-path opts]
   (let [parent (parent-dir file-path)
         _ (when parent (.makedirs os parent ** :exist_ok true))
         svg (render-svg s opts)
         areas (:areas (plan-model s))]
     (spit file-path svg)
     {:path file-path :areas areas :bytes (count svg)})))

(comment
  (require '[yardcraft.site-data :refer [site]])
  (require '[yardcraft.site-suggestions :as sug])
  (write-quote-plan! (sug/effective-site site :terrace-east-low-pad))
  (write-quote-plan! (sug/effective-site site) "out/quote-plan.svg")
  (write-quote-plan! (sug/effective-site site) "out/quote-plan.svg" {:show-angles? true})
  (:areas (plan-model (sug/effective-site site)))
  :rcf)
