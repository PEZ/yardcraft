(ns yardcraft.site-driveway
  "Driveway / parking massing — project subject."
  (:require [yardcraft.site-mesh :as mesh])
  (:import math))

(defn- edge-xy-at-y
  "Point on segment [x0 y0]–[x1 y1] at given y."
  [[x0 y0] [x1 y1] y]
  (let [t (/ (- y y0) (- y1 y0))]
    [(+ x0 (* t (- x1 x0))) y]))

(defn- lot-west-edge
  "West lot boundary SW→NW (last→first of :lot/polygon-xy)."
  [s]
  (let [lot (:lot/polygon-xy s)]
    [(last lot) (first lot)]))

(defn driveway-polygon-xy
  "Driveway ring NE NW SW SE. West edge follows lot west border; NE/SE and NW/SW Y from :driveway/polygon-xy."
  [s]
  (let [[ne nw _ se] (:driveway/polygon-xy s)
        [a b] (lot-west-edge s)]
    [ne
     (edge-xy-at-y a b (second nw))
     (edge-xy-at-y a b (second se))
     se]))

(defn driveway-depth-m
  "N–S span of driveway polygon (house-NW)."
  [s]
  (let [ys (map second (driveway-polygon-xy s))]
    (- (apply max ys) (apply min ys))))

(defn driveway-bod-z
  "Driveway height at bod = terrace deck."
  [s]
  (+ (:house/floor-z s) (:terrace/slab-thickness-m s)))

(defn driveway-mesh-ring-xy
  "Driveway polygon with Z-break verts on west/east edges. Order: NE NW W-break SW SE E-break."
  [s]
  (let [[ne nw sw se] (driveway-polygon-xy s)
        yb (:driveway/z-break-y s)
        wb (edge-xy-at-y nw sw yb)
        eb (edge-xy-at-y se ne yb)]
    [ne nw wb sw se eb]))

(defn driveway-edges-xy
  "West/east polylines south→north for driveway strip. Includes :driveway/z-break-y sample."
  [s]
  (let [[ne nw sw se] (driveway-polygon-xy s)
        yb (:driveway/z-break-y s)
        y-south (second se)
        y-north (max (second nw) (second ne))
        n 8
        ys (vec (sort (distinct
                       (conj (mapv (fn [i]
                                     (+ y-south (* (/ (double i) n)
                                                   (- y-north y-south))))
                                   (range (inc n)))
                             yb))))
        west (mapv (fn [y] (edge-xy-at-y sw nw y)) ys)
        east (mapv (fn [y] (edge-xy-at-y se ne y)) ys)]
    {:west west :east east :ys ys}))

(defn driveway-surface-z
  "Top Z on driveway mesh: flat at bod/terrace deck height. Road stitch deferred."
  [s _xy]
  (driveway-bod-z s))

(defn driveway-terrain-features
  "Terrain features seating the ground to the tarmac: west→east cross-lines
   at surface Z − 0.05 (so the slab sits proud) for each sampled y."
  [s]
  (let [{:keys [west east ys]} (driveway-edges-xy s)
        z0 (:terrain/z0-rh00 s)]
    (mapv (fn [w e y]
            (let [x-mid (/ (+ (first w) (first e)) 2.0)
                  z (- (driveway-surface-z s [x-mid y]) 0.05)]
              {:kind :polyline :xy [w e] :rh00 (+ z0 z)}))
          west east ys)))

(defn ensure-driveway-wall!
  "Retaining wall along the east (house-facing) edge: from terrain up to the
   driveway surface, running north from the south end while the drop exceeds
   0.05 m. Terrain sampled just east of the wall via terrain-z-fn.
   Object: site-driveway-wall."
  [s terrain-z-fn]
  (mesh/unlink-and-remove! "site-driveway-wall")
  (let [th (:driveway/east-wall-thickness-m s 0.2)
        embed 0.1
        {:keys [east]} (driveway-edges-xy s)
        xw (ffirst east)
        xe (+ xw th)
        run (->> east
                 (map (fn [[_ y]]
                        {:y y
                         :top (driveway-surface-z s [xw y])
                         :bot (terrain-z-fn [(+ xe 0.05) y])}))
                 (take-while (fn [{:keys [top bot]}] (> (- top bot) 0.05)))
                 vec)]
    (when (>= (count run) 2)
      (let [verts (vec (mapcat (fn [{:keys [y top bot]}]
                                 (let [zb (- bot embed)]
                                   [[xw y zb] [xe y zb] [xe y top] [xw y top]]))
                               run))
            n (count run)
            side-faces (mapcat (fn [i]
                                 (let [a (* 4 i)
                                       b (* 4 (inc i))]
                                   [[a b (+ b 3) (+ a 3)]
                                    [(+ a 1) (+ b 1) (+ b 2) (+ a 2)]
                                    [(+ a 3) (+ b 3) (+ b 2) (+ a 2)]
                                    [a b (+ b 1) (+ a 1)]]))
                               (range (dec n)))
            caps [[0 1 2 3]
                  (let [a (* 4 (dec n))] [a (+ a 1) (+ a 2) (+ a 3)])]
            faces (vec (concat side-faces caps))
            obj (mesh/add-mesh! "site-driveway-wall" verts faces)]
        {:name (.-name obj)
         :stations n
         :y-span [(:y (first run)) (:y (last run))]
         :max-height (apply max (map (fn [{:keys [top bot]}] (- top bot)) run))}))))

(defn driveway-seating
  "Seating for a length-m object at [x y]: :pitch-x-rad (Euler X so local +Y
   follows the downhill half of the driveway surface — a car straddling the
   ramp crest rests on the ramp plane, belly over the crest, instead of
   chord-diving through it) and :z-downhill (surface Z at the lower sampling
   end, where the bbox-min corner should rest)."
  [s [x y] length-m]
  (let [half (* 0.45 (double length-m))
        z-c (driveway-surface-z s [x y])
        z-n (driveway-surface-z s [x (+ y half)])
        z-s (driveway-surface-z s [x (- y half)])]
    {:pitch-x-rad (cond
                    (< z-n z-s) (math/atan2 (- z-n z-c) half)
                    (< z-s z-n) (math/atan2 (- z-c z-s) half)
                    :else 0.0)
     :z-downhill (min z-n z-s)}))

(defn ensure-driveway!
  "Asphalt driveway quad-strip: flat at bod/terrace deck height. Road stitch deferred."
  [s]
  (let [{:keys [west east]} (driveway-edges-xy s)
        n (count west)
        zf #(driveway-surface-z s %)
        verts (vec (concat (map (fn [[x y]] [x y (double (zf [x y]))]) west)
                           (map (fn [[x y]] [x y (double (zf [x y]))]) east)))
        faces (mapv (fn [i] [i (inc i) (+ n (inc i)) (+ n i)])
                    (range (dec n)))
        obj (mesh/add-mesh! "site-driveway" verts faces)]
    {:name (.-name obj)
     :vert-count (count verts)
     :face-count (count faces)
     :depth-m (driveway-depth-m s)
     :z-bod (driveway-bod-z s)
     :z-break-y (:driveway/z-break-y s)
     :z-north-w (zf (last west))
     :z-north-e (zf (last east))}))
