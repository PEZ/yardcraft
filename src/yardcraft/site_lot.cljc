(ns yardcraft.site-lot
  "Lot polygon, frontage road strip, terrain mesh, and contour polylines."
  (:require [yardcraft.site-lm :as lm]
            [yardcraft.site-mesh :as mesh]
            [basilisp.string :as string])
  (:import math mathutils os
           [os.path :as path]))

(defn lot-polygon-xy
  "Closed-ready open ring of lot corners in house-NW frame."
  [s]
  (:lot/polygon-xy s))

(defn road-edge-xy
  "Street-touching lot edge as [[x0 y0] [x1 y1]] (first polygon segment)."
  [s]
  (let [poly (lot-polygon-xy s)]
    [(nth poly 0) (nth poly 1)]))

(defn offset-polyline
  [pts dist]
  (let [n (count pts)
        seg-normals
        (mapv (fn [i]
                (let [[x0 y0] (nth pts i)
                      [x1 y1] (nth pts (inc i))
                      dx (- x1 x0)
                      dy (- y1 y0)
                      len (math/hypot dx dy)]
                  [(/ (- dy) len) (/ dx len)]))
              (range (dec n)))
        vert-normals
        (mapv (fn [i]
                (cond
                  (zero? i) (nth seg-normals 0)
                  (= i (dec n)) (nth seg-normals (dec (count seg-normals)))
                  :else (let [[ax ay] (nth seg-normals (dec i))
                              [bx by] (nth seg-normals i)
                              sx (+ ax bx)
                              sy (+ ay by)
                              sl (math/hypot sx sy)]
                          (if (< sl 1e-9) [ax ay] [(/ sx sl) (/ sy sl)]))))
              (range n))]
    (mapv (fn [[x y] [nx ny]]
            [(+ x (* dist nx)) (+ y (* dist ny))])
          pts vert-normals)))

(defn road-edges-xy
  "Inner/outer polylines for the frontage strip (both west→east).
   Inner = :road/inner-edge-xy (hand-traced over the 1999 map light table);
   outer = inner offset :road/width-m away from the lot (mitered normals).
   The data keeps an inner vertex at each :road/z-profile knot so the
   piecewise Z renders exactly."
  [s]
  (let [inner (mapv vec (:road/inner-edge-xy s))
        outer (offset-polyline inner (:road/width-m s))]
    {:inner inner :outer outer}))

(defn road-ring-xy
  "Closed ring: inner west→east ++ reverse outer (for legacy consumers)."
  [s]
  (let [{:keys [inner outer]} (road-edges-xy s)]
    (vec (concat inner (reverse outer)))))

(defn road-strip-mesh
  "Quad-strip surface from parallel inner/outer polylines; z-fn maps [x y] → Z.
   Returns {:verts [[x y z] ...] :faces [[i j k l] ...]}."
  [inner outer z-fn]
  (let [n (count inner)
        verts (vec (concat
                    (map (fn [[x y]] [x y (double (z-fn [x y]))]) inner)
                    (map (fn [[x y]] [x y (double (z-fn [x y]))]) outer)))
        faces (mapv (fn [i]
                      [i (inc i) (+ n (inc i)) (+ n i)])
                    (range (dec n)))]
    {:verts verts :faces faces}))

(defn rh00->z
  "Model Z from RH00 using :terrain/z0-rh00."
  [s rh00]
  (- (double rh00) (double (:terrain/z0-rh00 s))))

(defn- ray-crosses-edge?
  "Ray-cast crossing test for point [x y] against edge (xi,yi)-(xj,yj)."
  [[x y] [xi yi] [xj yj]]
  (let [denom (let [d (- yj yi)] (if (< (math/fabs d) 1e-12) 1e-12 d))
        straddles? (not= (> yi y) (> yj y))]
    (and straddles?
         (< x (+ xi (/ (* (- xj xi) (- y yi)) denom))))))

(defn- toggle-parity
  "Flip inside? when the ray crosses an edge."
  [inside? hit?]
  (if hit? (not inside?) inside?))

(defn point-in-poly?
  [p poly]
  (let [ring (conj (vec poly) (first poly))
        edges (map vector ring (rest ring))]
    (reduce (fn [inside? [a b]]
              (toggle-parity inside? (ray-crosses-edge? p a b)))
            false
            edges)))

(defn dist-point-segment
  "Distance from p to segment a-b."
  [[px py] [ax ay] [bx by]]
  (let [dx (- bx ax)
        dy (- by ay)
        len2 (+ (* dx dx) (* dy dy))]
    (if (< len2 1e-12)
      (math/hypot (- px ax) (- py ay))
      (let [t (max 0.0 (min 1.0 (/ (+ (* (- px ax) dx) (* (- py ay) dy)) len2)))]
        (math/hypot (- px (+ ax (* t dx))) (- py (+ ay (* t dy))))))))

(defn dist-to-polyline
  "Min distance from p to open polyline pts (single point OK)."
  [p pts]
  (if (= 1 (count pts))
    (dist-point-segment p (first pts) (first pts))
    (reduce min (map (fn [a b] (dist-point-segment p a b)) pts (rest pts)))))

(defn terrain-features
  "Interpolation features: plateau polygon (:terrain/plateau-xy), natural contour polylines, lot-corner points."
  [s]
  (let [{:terrain/keys [corner-rh00 contours plateau-rh00 plateau-xy]
         :lot/keys [polygon-xy]} s]
    (-> [{:kind :polygon :xy plateau-xy :rh00 plateau-rh00}]
        (into (map (fn [{:keys [rh00 xy]}] {:kind :polyline :xy xy :rh00 rh00})
                   contours))
        (into (map (fn [xy rh00] {:kind :polyline :xy [xy] :rh00 rh00})
                   polygon-xy
                   corner-rh00)))))

(defn feature-distance
  "Distance from p to feature; 0 inside :polygon features."
  [{:keys [kind xy]} p]
  (if (and (= kind :polygon)
           (point-in-poly? p xy))
    0.0
    (dist-to-polyline p (if (= kind :polygon)
                          (conj (vec xy) (first xy))
                          xy))))

(defn features-z
  "Model Z at [x y]: inverse-cube Shepard blend over feature distances.
   Exact on features; (0,0) lies inside the plateau polygon, so the datum is exact."
  [s features p]
  (let [ws (map (fn [{:keys [rh00] :as f}]
                  (let [d (feature-distance f p)
                        w (if (< d 1e-6) 1e12 (/ 1.0 (* d d d)))]
                    [w (rh00->z s rh00)]))
                features)
        sw (reduce + (map first ws))]
    (/ (reduce + (map (fn [[w z]] (* w z)) ws)) sw)))

(defn sample-ring-xy
  "Open ring of XY samples along poly edges (excludes duplicate closing point)."
  [poly step]
  (let [ring (conj (vec poly) (first poly))]
    (vec
     (mapcat
      (fn [a b]
        (let [len (math/hypot (- (first b) (first a))
                              (- (second b) (second a)))
              n (max 1 (int (math/ceil (/ len step))))]
          (map (fn [i]
                 (let [t (/ (double i) n)]
                   [(+ (first a) (* t (- (first b) (first a))))
                    (+ (second a) (* t (- (second b) (second a))))]))
               (range n))))
      ring (rest ring)))))

(defn interior-grid-xy
  "Grid XY points strictly inside poly."
  [poly step]
  (let [xs (mapv first poly)
        ys (mapv second poly)
        min-x (apply min xs)
        max-x (apply max xs)
        min-y (apply min ys)
        max-y (apply max ys)
        nx (inc (int (math/ceil (/ (- max-x min-x) step))))
        ny (inc (int (math/ceil (/ (- max-y min-y) step))))]
    (vec
     (for [iy (range ny)
           ix (range nx)
           :let [x (+ min-x (* ix step))
                 y (+ min-y (* iy step))]
           :when (point-in-poly? [x y] poly)]
       [x y]))))

(defn build-terrain-grid
  "Lot-filling terrain mesh: boundary ring + interior grid, CDT triangles.
   z-fn maps [x y] → model Z (default: feature-distance blend).
   Returns {:verts [[x y z] ...] :faces [[i j k] ...]}."
  ([s features]
   (build-terrain-grid s features #(features-z s features %)))
  ([s features z-fn]
   (let [poly (lot-polygon-xy s)
         step (double (:terrain/grid-step-m s))
         boundary (sample-ring-xy poly step)
         interior (interior-grid-xy poly step)
         verts2d (vec (concat boundary interior))
         n-bound (count boundary)
         edges (mapv (fn [i] #py [i (mod (inc i) n-bound)]) (range n-bound))
         py-verts (mapv (fn [[x y]] (.Vector mathutils #py [x y])) verts2d)
         cdt ((.-delaunay_2d_cdt (.-geometry mathutils))
              py-verts edges #py [] 1 1e-5 false)
         out-verts (nth cdt 0)
         out-faces (nth cdt 2)
         verts (mapv (fn [v]
                       (let [x (double (.-x v))
                             y (double (.-y v))]
                         [x y (double (z-fn [x y]))]))
                     out-verts)
         faces (mapv vec out-faces)]
     {:verts verts :faces faces})))

(defn clear-contour-objects!
  []
  (->> (mesh/all-object-names)
       (filter #(string/starts-with? % "site-contour-"))
       (run! mesh/unlink-and-remove!)))

(defn ensure-contours!
  "Polyline meshes site-contour-<rh00> at contour Z (+small lift). Overlay only. Returns names."
  [s]
  (clear-contour-objects!)
  (let [names
        (mapv (fn [{:keys [rh00 xy]}]
                (let [n (str "site-contour-" (int rh00))
                      z (+ (rh00->z s rh00) 0.08)]
                  (.-name (mesh/add-polyline-mesh! n xy z))))
              (:terrain/contours s))]
    {:names names}))

(defn smoothstep
  [edge0 edge1 x]
  (let [t (/ (- x edge0) (- edge1 edge0))
        t (max 0.0 (min 1.0 t))]
    (* t t (- 3.0 (* 2.0 t)))))

(defn front-of-house?
  "Strip between house north wall and frontage road."
  [{:house/keys [size-m]} [x y]]
  (let [hx (first size-m)]
    (and (<= -1.0 x (+ hx 1.0))
         (<= 0.0 y 6.5))))

(defn road-stitch-like?
  "Single-point road-edge stitch feature (y≈6)."
  [{:keys [kind xy]}]
  (and (= kind :polyline)
       (= 1 (count xy))
       (let [y (second (first xy))]
         (and (<= 5.5 y) (<= y 7.0)))))

(defn manual-trust?
  "Plateau interior or within 2 m of a constructed extra feature."
  [s constructed p]
  (or (point-in-poly? p (:terrain/plateau-xy s))
      (some #(and (< (feature-distance % p) 2.0) %) constructed)))

(defn ensure-draw-pad!
  "Flat lot slab for contour tracing. Top at z=-0.01. Object: site-draw-pad."
  [s]
  (let [poly (lot-polygon-xy s)
        z -0.02
        th 0.01
        obj (mesh/add-polygon-slab! "site-draw-pad" poly z th)]
    {:name (.-name obj) :top-z (+ z th)}))

(defn- lerp [a b u] (+ a (* u (- b a))))

(defn- interpolate-rh00
  "Lerp RH00 between knots a and b at x (caller ensures xa ≤ x ≤ xb)."
  [x {xa :x ra :rh00} {xb :x rb :rh00}]
  (let [span (- xb xa)
        u (if (< span 1e-12) 0.0 (/ (- x xa) span))]
    (lerp ra rb u)))

(defn- rh00-between-knots
  "Piecewise RH00 for x against sorted profile knots."
  [x pts]
  (loop [i 0]
    (let [a (nth pts i)
          b (nth pts (inc i) nil)
          xa (:x a)
          ra (:rh00 a)]
      (cond
        (nil? b) ra
        (<= x xa) ra
        (<= x (:x b)) (interpolate-rh00 x a b)
        :else (recur (inc i))))))

(defn- rh00-at-x
  "Interpolate :road/z-profile RH00 at house-NW x (clamped outside the knots)."
  [s x]
  (rh00-between-knots x (vec (sort-by :x (:road/z-profile s)))))

(defn road-surface-z
  "Model Z of road top at [x y]: piecewise-linear :road/z-profile in house-NW x."
  [s xy]
  (rh00->z s (rh00-at-x s (first xy))))

(defn front-strip-z
  "Ramp front strip to road: blend features (no stitch) → road surface by y."
  [s features xy]
  (let [[_ y] xy
        base (features-z s (remove road-stitch-like? features) xy)
        road (road-surface-z s xy)
        t (smoothstep 3.5 6.0 y)]
    (+ base (* t (- road base)))))

(defn- apply-z-clamps
  "Lower Z to each :clamp-polygon feature's RH00 when p is inside its ring."
  [s constructed p z]
  (reduce (fn [acc {:keys [kind xy rh00]}]
            (if (and (= kind :clamp-polygon)
                     (point-in-poly? p xy))
              (min acc (rh00->z s rh00))
              acc))
          z
          constructed))

(defn terrain-z-with-lm
  "Selective terrain Z: trust constructed/plateau blend, LM grid elsewhere,
   front-of-house strip ramps to road (avoids stitch hill).
   Driveway clamp via :clamp-polygon constructed features."
  [s features constructed xy]
  (let [z (cond
            (front-of-house? s xy) (front-strip-z s features xy)
            (manual-trust? s constructed xy) (features-z s features xy)
            :else (lm/lm-z-at s xy))]
    (apply-z-clamps s constructed xy z)))

(def terrain-cache-relpath "assets/terrain/site-terrain.glb")

(defn terrain-cache-path
  []
  (path/join (os/getcwd) terrain-cache-relpath))

(defn write-terrain-cache!
  []
  (mesh/export-object-gltf! "site-terrain" (terrain-cache-path)))

(defn load-terrain-cache!
  []
  (mesh/unlink-and-remove! "site-lot")
  (mesh/unlink-and-remove! "site-terrain")
  (let [{:keys [name dims]} (mesh/import-gltf-as! "site-terrain" (terrain-cache-path))]
    {:name name
     :cached? true
     :path terrain-cache-relpath
     :dims dims}))

(defn ensure-lot!
  "Elevated lot terrain (site-terrain). Loads assets/terrain/site-terrain.glb when
  present unless :rebuild? true; otherwise builds LM/feature mesh and writes cache.
  Opts: {:rebuild? bool}."
  ([s] (ensure-lot! s []))
  ([s extra-features] (ensure-lot! s extra-features {}))
  ([s extra-features {:keys [rebuild?] :or {rebuild? false}}]
   (mesh/unlink-and-remove! "site-lot")
   (let [cache (terrain-cache-path)]
     (if (and (not rebuild?) (path/isfile cache))
       (load-terrain-cache!)
       (let [features (into (terrain-features s) extra-features)
             constructed (conj (vec extra-features)
                             {:kind :polygon
                              :xy (:terrain/plateau-xy s)
                              :rh00 (:terrain/plateau-rh00 s)})
             z-fn (fn [[x y]] (terrain-z-with-lm s features constructed [x y]))
             {:keys [verts faces]} (build-terrain-grid s features z-fn)
             obj (mesh/add-mesh! "site-terrain" verts faces)
             wrote (write-terrain-cache!)]
         {:name (.-name obj)
          :vert-count (count verts)
          :face-count (count faces)
          :feature-count (count features)
          :cached? false
          :wrote-cache wrote})))))

(defn road-stitch-features
  "Terrain features leveling the lot's north edge to the road surface
   (road = source of truth) from the NW corner east to :road/stitch-to-x.
   Single-point features at the terrain boundary-ring sample positions,
   so the edge verts take the road Z exactly."
  [s]
  (let [[a b] (road-edge-xy s)
        step (double (:terrain/grid-step-m s))
        len (math/hypot (- (first b) (first a)) (- (second b) (second a)))
        n (max 1 (int (math/ceil (/ len step))))
        z0 (double (:terrain/z0-rh00 s))
        to-x (double (:road/stitch-to-x s))]
    (vec (for [i (range n)
               :let [t (/ (double i) n)
                     x (+ (first a) (* t (- (first b) (first a))))
                     y (+ (second a) (* t (- (second b) (second a))))]
               :when (<= x to-x)]
           {:kind :polyline :xy [[x y]]
            :rh00 (+ z0 (road-surface-z s [x y]))}))))

(defn ensure-road!
  "Schematic frontage strip on the traced inner edge; piecewise Z west→east.
   Quad-strip surface (no thickness). Object: site-road-frontage.
   Z-profile mid knot (≈house-center x) is RH00 49.0 (model Z=0)."
  [s]
  (let [{:keys [inner outer]} (road-edges-xy s)
        {:keys [verts faces]} (road-strip-mesh inner outer #(road-surface-z s %))
        obj (mesh/add-mesh! "site-road-frontage" verts faces)]
    {:name (.-name obj)
     :vert-count (count verts)
     :face-count (count faces)
     :z-at-knots (mapv (fn [{:keys [x]}] [x (road-surface-z s [x 0.0])])
                       (:road/z-profile s))}))
