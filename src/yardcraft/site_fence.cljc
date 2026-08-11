(ns yardcraft.site-fence
  "Schematic wooden fence along the street-facing lot border."
  (:require [basilisp.string :as string]
            [yardcraft.site-lot :as lot]
            [yardcraft.site-driveway :as driveway]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-railing :as railing])
  (:import math))

(defn fence-opts
  "Schematic wood fence dimensions from site facts."
  [s]
  {:height (:fence/height-m s 1.0)
   :post (:fence/post-size-m s 0.08)
   :rail (:fence/rail-size-m s 0.05)
   :baluster (:fence/baluster-size-m s 0.04)
   :spacing (:fence/baluster-spacing-m s 0.12)
   :inset (:fence/inset-m s 0.08)})

(defn- split-run
  "Axis-aligned segments along x at constant y, each ≤ max-len."
  [x0 x1 y max-len]
  (let [span (- x1 x0)
        n (max 1 (int (math/ceil (/ span max-len))))]
    (mapv (fn [i]
            (let [a (+ x0 (* (/ (double i) n) span))
                  b (+ x0 (* (/ (double (inc i)) n) span))]
              {:x0 a :y0 y :x1 b :y1 y}))
          (range n))))

(defn- merge-intervals
  "Merge overlapping/touching [lo hi] intervals (sorted by lo)."
  [ivals]
  (reduce (fn [acc [a b]]
            (let [[pa pb] (peek acc)]
              (if (and pa (<= a (+ pb 1e-6)))
                (conj (pop acc) [pa (max pb b)])
                (conj acc [a b]))))
          []
          (sort-by first ivals)))

(defn- fence-gap-intervals
  "Openings along frontage x: entrance gate + mailbox clearance, merged."
  [s]
  (let [{door-w :door/width-m
         {:keys [west-offset-m]} :door/north
         gate-w :fence/gate-width-m
         mb-gap :fence/mailbox-gap-m
         :or {gate-w 1.5 mb-gap 1.0}} s
        gate-cx (+ west-offset-m (/ door-w 2.0))
        gate-half (/ gate-w 2.0)
        [mbx _] (:mailbox/xy s)
        mb-half (/ mb-gap 2.0)]
    (merge-intervals [[(- gate-cx gate-half) (+ gate-cx gate-half)]
                      [(- mbx mb-half) (+ mbx mb-half)]])))

(defn fence-frontage-runs
  "Open fence runs along the road edge between driveway, gaps, and NE corner.
  Inset south of the lot road edge. Returns [{:id :x0 :x1 :y} ...]."
  [s]
  (let [[[_ y0] [x-ne _]] (lot/road-edge-xy s)
        {:keys [inset]} (fence-opts s)
        y (- y0 inset)
        x-drv-e (ffirst (driveway/driveway-polygon-xy s))
        gaps (fence-gap-intervals s)
        cuts (concat [x-drv-e] (mapcat identity gaps) [x-ne])]
    (->> (partition 2 cuts)
         (keep-indexed (fn [i [a b]]
                         (when (> (- b a) 0.05)
                           {:id (str i) :x0 a :x1 b :y y})))
         vec)))

(defn clear-fence!
  "Remove all site-fence* objects."
  []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-fence")
                (mesh/site-object-names))))

(defn ensure-fence!
  "Schematic wooden picket fence along street frontage.
  Gaps at driveway, entrance+mailbox; rails follow terrain pitch per segment.
  Posts sit on terrain-z-fn. Objects: site-fence-*."
  [s terrain-z-fn]
  (clear-fence!)
  (let [opts (fence-opts s)
        max-seg (:fence/segment-m s 3.0)
        names
        (vec
         (mapcat
          (fn [{:keys [id x0 x1 y]}]
            (mapcat
             (fn [i end]
               (let [z-a (double (terrain-z-fn [(:x0 end) y]))
                     z-b (double (terrain-z-fn [(:x1 end) y]))
                     prefix (str "site-fence-" id "-" i)]
                 (railing/add-sloped-railing-run!
                  prefix (assoc end :z0 z-a :z1 z-b) opts)))
             (range)
             (split-run x0 x1 y max-seg)))
          (fence-frontage-runs s)))]
    {:names names
     :count (count names)
     :runs (fence-frontage-runs s)}))
