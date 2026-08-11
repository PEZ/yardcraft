(ns yardcraft.site-railing
  "Schematic wood railing: posts, rails, balusters, stair sides."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh])
  (:import math mathutils))

(defn railing-opts
  "Schematic wood railing dimensions from site facts."
  [s]
  {:height (:railing/height-m s 1.0)
   :post (:railing/post-size-m s 0.08)
   :rail (:railing/rail-size-m s 0.05)
   :baluster (:railing/baluster-size-m s 0.03)
   :spacing (:railing/baluster-spacing-m s 0.15)
   :inset (:railing/inset-m s 0.04)})

(defn railing-z-deck
  "Top of terrace slab (post bases)."
  [s]
  (+ (:house/floor-z s) (:terrace/slab-thickness-m s)))

(defn- add-post!
  [n {:keys [x y z0 height size]}]
  (mesh/add-box! n [size size height] [x y (+ z0 (/ height 2.0))])
  n)

(defn- add-rail!
  "Axis-aligned rail along dominant axis between endpoints."
  [n {:keys [x0 y0 x1 y1 z-c thick height]}]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        len (math/hypot dx dy)
        cx (/ (+ x0 x1) 2.0)
        cy (/ (+ y0 y1) 2.0)
        along-x? (> (math/fabs dx) (math/fabs dy))
        sx (if along-x? len thick)
        sy (if along-x? thick len)]
    (mesh/add-box! n [sx sy height] [cx cy z-c])
    n))

(defn- add-balusters-along!
  [prefix {:keys [x0 y0 x1 y1 z0 height size spacing]}]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        len (math/hypot dx dy)
        n (max 0 (dec (int (math/floor (/ len spacing)))))]
    (mapv (fn [i]
            (let [t (/ (double i) (inc n))
                  nm (str prefix "-b" i)]
              (add-post! nm {:x (+ x0 (* t dx))
                             :y (+ y0 (* t dy))
                             :z0 z0
                             :height height
                             :size size})
              nm))
          (range 1 (inc n)))))

(defn add-railing-run!
  "Posts, top/bottom rails, and balusters from end map at z0."
  [prefix {:keys [x0 y0 x1 y1]} z0 opts]
  (let [{:keys [height post rail baluster spacing]} opts
        top-z (+ z0 height (- (/ rail 2.0)))
        bot-z (+ z0 0.08 (/ rail 2.0))
        bal-h (- height rail 0.08)
        bal-z0 (+ z0 0.08)
        p0 (str prefix "-p0")
        p1 (str prefix "-p1")
        top (str prefix "-top")
        bot (str prefix "-bot")
        end {:x0 x0 :y0 y0 :x1 x1 :y1 y1}]
    (add-post! p0 {:x x0 :y y0 :z0 z0 :height height :size post})
    (add-post! p1 {:x x1 :y y1 :z0 z0 :height height :size post})
    (add-rail! top (assoc end :z-c top-z :thick rail :height rail))
    (add-rail! bot (assoc end :z-c bot-z :thick rail :height rail))
    (into [p0 p1 top bot]
          (add-balusters-along! prefix
                                (assoc end
                                       :z0 bal-z0
                                       :height bal-h
                                       :size baluster
                                       :spacing spacing)))))

(defn- orient-along!
  "Point obj +X along end map (:x0 :y0 :z0 → :x1 :y1 :z1)."
  [obj {:keys [x0 y0 z0 x1 y1 z1]}]
  (let [dir (.normalized (mathutils/Vector #py [(double (- x1 x0))
                                                (double (- y1 y0))
                                                (double (- z1 z0))]))]
    (set! (.-rotation-euler obj) (.to-euler (.to-track-quat dir "X" "Z")))
    obj))

(defn- add-sloped-rail!
  "Box rail from (x0,y0,z0) to (x1,y1,z1), oriented along the segment."
  [n {:keys [x0 y0 z0 x1 y1 z1]} thick]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        dz (- z1 z0)
        len (math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
        cx (/ (+ x0 x1) 2.0)
        cy (/ (+ y0 y1) 2.0)
        cz (/ (+ z0 z1) 2.0)]
    (mesh/add-box! n [len thick thick] [cx cy cz])
    (orient-along! (mesh/object-by-name n)
                   {:x0 x0 :y0 y0 :z0 z0 :x1 x1 :y1 y1 :z1 z1})
    n))

(defn add-sloped-railing-run!
  "Posts, sloped top/bottom rails, and balusters. End map includes ground Zs :z0 :z1."
  [prefix {:keys [x0 y0 z0 x1 y1 z1]} opts]
  (let [{:keys [height post rail baluster spacing]} opts
        p0 (str prefix "-p0")
        p1 (str prefix "-p1")
        top (str prefix "-top")
        bot (str prefix "-bot")
        zt0 (+ z0 height (- (/ rail 2.0)))
        zt1 (+ z1 height (- (/ rail 2.0)))
        zb0 (+ z0 0.08 (/ rail 2.0))
        zb1 (+ z1 0.08 (/ rail 2.0))
        dx (- x1 x0)
        dy (- y1 y0)
        len-xy (math/hypot dx dy)
        n-bal (max 0 (dec (int (math/floor (/ len-xy spacing)))))
        bals (mapv (fn [i]
                     (let [t (/ (double i) (inc n-bal))
                           nm (str prefix "-b" i)
                           z (+ z0 (* t (- z1 z0)))]
                       (add-post! nm {:x (+ x0 (* t dx))
                                      :y (+ y0 (* t dy))
                                      :z0 (+ z 0.08)
                                      :height (- height rail 0.08)
                                      :size baluster})
                       nm))
                   (range 1 (inc n-bal)))]
    (add-post! p0 {:x x0 :y y0 :z0 z0 :height height :size post})
    (add-post! p1 {:x x1 :y y1 :z0 z1 :height height :size post})
    (add-sloped-rail! top {:x0 x0 :y0 y0 :z0 zt0 :x1 x1 :y1 y1 :z1 zt1} rail)
    (add-sloped-rail! bot {:x0 x0 :y0 y0 :z0 zb0 :x1 x1 :y1 y1 :z1 zb1} rail)
    (into [p0 p1 top bot] bals)))

(defn add-stair-side-railing!
  "Posts at top/bottom, sloped top rail, balusters along a stair side."
  [prefix steps {:keys [x0 y0 z0 x1 y1 z1]} opts]
  (let [{:keys [height post rail baluster]} opts
        p-top (str prefix "-p-top")
        p-bot (str prefix "-p-bot")
        n-rail (str prefix "-top")
        zt0 (+ z0 height)
        zt1 (+ z1 height)
        dx (- x1 x0)
        dy (- y1 y0)
        dz (- zt1 zt0)
        len (math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
        cx (/ (+ x0 x1) 2.0)
        cy (/ (+ y0 y1) 2.0)
        cz (/ (+ zt0 zt1) 2.0)
        _ (add-post! p-top {:x x0 :y y0 :z0 z0 :height height :size post})
        _ (add-post! p-bot {:x x1 :y y1 :z0 z1 :height height :size post})
        _ (mesh/add-box! n-rail [len rail rail] [cx cy cz])
        _ (orient-along! (mesh/object-by-name n-rail)
                         {:x0 x0 :y0 y0 :z0 zt0 :x1 x1 :y1 y1 :z1 zt1})
        bals (mapv (fn [i]
                     (let [t (/ (+ i 0.5) steps)
                           nm (str prefix "-b" i)]
                       (add-post! nm {:x (+ x0 (* t dx))
                                      :y (+ y0 (* t dy))
                                      :z0 (+ z0 (* t (- z1 z0)))
                                      :height height
                                      :size baluster})
                       nm))
                   (range steps))]
    (into [p-top p-bot n-rail] bals)))

(defn clear-railings!
  "Remove all site-railing* objects."
  []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-railing")
                (mesh/site-object-names))))

(defn install-railings!
  "Build deck runs + stair-side railings. deck-runs: [prefix end-map]*.
  stair-sides: [{:keys [prefix steps end]}*]."
  [z-deck opts deck-runs stair-sides]
  (let [deck-names (mapcat (fn [[prefix end]]
                             (add-railing-run! prefix end z-deck opts))
                           deck-runs)
        stair-names (mapcat (fn [{:keys [prefix steps end]}]
                              (add-stair-side-railing! prefix steps end opts))
                            stair-sides)
        all (vec (concat deck-names stair-names))]
    {:names all :count (count all)}))
