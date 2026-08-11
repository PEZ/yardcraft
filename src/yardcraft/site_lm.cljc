(ns yardcraft.site-lm
  "Lantmäteriet Min Karta höjd grid — cached EDN samples and road Z profile."
  (:require [basilisp.edn :as edn])
  (:import math
           os
           [os.path :as path]))

(def grid-relpath "src/yardcraft/data/lm_height_grid.edn")

(defonce grid* (atom nil))

(defn- grid-path
  []
  (path/join (os/getcwd) grid-relpath))

(defn- read-edn-file
  [p]
  (edn/read-string
   (with-open [f (python/open p "r" ** :encoding "utf-8")]
     (.read f))))

(defn load-grid!
  "Load LM height grid EDN from project root; cache in atom.
  Returns nil when the file is missing (optional for empty / early sites)."
  []
  (let [p (grid-path)]
    (if-not (path/isfile p)
      (do
        (reset! grid* nil)
        nil)
      (do
        (reset! grid* (read-edn-file p))
        @grid*))))

(defn grid
  "Cached LM grid map; loads on first use. Nil when file absent."
  []
  (or @grid* (load-grid!)))

(defn road-z-profile
  "Frontage RH00 profile from LM grid (house-NW x), or nil if no grid."
  []
  (:road-z-profile (grid)))

(defn with-lm-road
  "Overlay :road/z-profile from LM grid when present; otherwise leave s unchanged."
  [s]
  (if-let [profile (road-z-profile)]
    (assoc s :road/z-profile profile)
    s))

(defn- sample-distance
  [[px py] {:keys [xy]}]
  (let [[x y] xy]
    (math/hypot (- px x) (- py y))))

(defn- idw-rh00
  "Inverse-square blend over nearest samples; exact when d≈0."
  [p samples]
  (loop [[sample & rest] samples
         acc 0.0
         wsum 0.0]
    (if sample
      (let [d (sample-distance p sample)
            {:keys [rh00]} sample]
        (if (< d 1e-9)
          rh00
          (let [w (/ 1.0 (* d d))]
            (recur rest (+ acc (* w rh00)) (+ wsum w)))))
      (/ acc wsum))))

(defn lm-rh00-at
  "RH00 at house-NW [x y] via IDW over the six nearest LM samples."
  [xy]
  (let [samples (:samples (grid))
        nearest (take 6 (sort-by #(sample-distance xy %) samples))]
    (idw-rh00 xy nearest)))

(defn lm-z-at
  "Model Z at house-NW [x y] from LM samples and site :terrain/z0-rh00."
  [s xy]
  (- (lm-rh00-at xy) (:terrain/z0-rh00 s)))
