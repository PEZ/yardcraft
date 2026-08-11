(ns yardcraft.site-terrace-supports
  "Ground posts under outer terrace edges."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh])
  (:import math))

(defn support-opts
  "Ground-support post dimensions from site facts."
  [s]
  {:length (:terrace/support-length-m s 7.0)
   :size (:terrace/support-size-m s 0.15)
   :spacing (:terrace/support-spacing-m s 2.5)
   :inset (:terrace/support-inset-m s 0.08)})

(defn- round-xy
  "Round XY to cm for corner dedupe across edges."
  [[x y]]
  [(/ (python/round (* x 100.0)) 100.0)
   (/ (python/round (* y 100.0)) 100.0)])

(defn- points-along
  "Endpoints plus intermediates spaced ~spacing apart."
  [{:keys [x0 y0 x1 y1]} spacing]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        len (math/hypot dx dy)
        n-seg (max 1 (int (python/round (/ (max len 0.01) spacing))))]
    (mapv (fn [i]
            (let [t (/ (double i) n-seg)]
              (round-xy [(+ x0 (* t dx)) (+ y0 (* t dy))])))
          (range (inc n-seg)))))

(defn- add-support-post!
  "Post with top at z-top, extending length downward."
  [n {:keys [x y z-top length size]}]
  (mesh/add-box! n [size size length] [x y (- z-top (/ length 2.0))])
  n)

(defn clear-supports!
  "Remove all site-terrace-post* objects."
  []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-terrace-post")
                (mesh/site-object-names))))

(defn xy-points-from-edges
  "Distinct XY points along edge runs at spacing."
  [edge-runs spacing]
  (->> edge-runs
       (mapcat (fn [end] (points-along end spacing)))
       distinct
       vec))

(defn install-supports!
  "Ground posts at xy-points; top at z-top."
  [z-top {:keys [length size]} xy-points]
  (let [names (mapv (fn [i [x y]]
                      (add-support-post! (str "site-terrace-post-" i)
                                         {:x x :y y :z-top z-top
                                          :length length :size size}))
                    (range) xy-points)]
    {:names names :count (count names) :z-top z-top :z-bottom (- z-top length)}))
