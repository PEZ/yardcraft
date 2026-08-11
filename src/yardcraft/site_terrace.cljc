(ns yardcraft.site-terrace
  "L-shaped terrace, stairs, railing, and roofed canopy — main design surface."
  (:require [basilisp.string :as string]
            [yardcraft.site-driveway :as driveway]
            [yardcraft.site-house :as house]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-railing :as railing]
            [yardcraft.site-terrace-supports :as supports])
  (:import math mathutils))

(defn- xy-rect
  "Axis-aligned footprint map from min/max XY."
  [min-x min-y max-x max-y]
  {:min-x min-x :max-x max-x :min-y min-y :max-y max-y
   :sx (- max-x min-x) :sy (- max-y min-y)})

(defn- terrace-east-x
  "House east + optional east bay extension."
  [s]
  (+ (:max-x (house/house-footprint s)) (:terrace/east-extend-m s 0)))

(defn- stairs-east-dir
  "East stair descent direction; default :east (into SE cutout)."
  [s]
  (get-in s [:stairs/east :dir] :east))

(defn- stairs-east-cutout?
  "SE open notch for west→east stairs only."
  [s]
  (and (= (stairs-east-dir s) :east)
       (get-in s [:stairs/east :cutout?] true)))

(defn- east-low-extend-m
  [s]
  (:terrace/east-low-extend-m s 0))

(defn- east-low?
  "Low east pad present (east of stairs / high deck)."
  [s]
  (pos? (east-low-extend-m s)))

(defn- east-low-top-z
  [s]
  (:terrace/east-low-top-z s 0.2))

(defn- east-low-floor-z
  "Slab underside / box floor for low pad."
  [s]
  (- (east-low-top-z s) (:terrace/slab-thickness-m s)))

(defn- stairs-east-south?
  [s]
  (= (stairs-east-dir s) :south))

(defn- south-low-depth-m
  [s]
  (:terrace/south-low-depth-m s 0))

(defn- south-low?
  "Low pad south of south-facing east stairs."
  [s]
  (and (stairs-east-south? s) (pos? (south-low-depth-m s))))

(defn- south-low-top-z
  [s]
  (:terrace/south-low-top-z s -0.05))

(defn- south-low-floor-z
  [s]
  (- (south-low-top-z s) (:terrace/slab-thickness-m s)))

(defn terrace-south-footprint
  "South deck: house full width × terrace depth south of south wall."
  [s]
  (let [fp (house/house-footprint s)
        depth (:terrace/depth-m s)
        min-y (- (:min-y fp) depth)
        max-y (:min-y fp)]
    (xy-rect (:min-x fp) min-y (terrace-east-x s) max-y)))

(defn terrace-east-low-footprint
  "Low east pad: after external east stairs (or high east edge) out to :terrace/east-low-extend-m.
  nil when east-low-extend-m is 0."
  [s]
  (when (east-low? s)
    (let [fp (house/house-footprint s)
          depth (:terrace/depth-m s)
          e (:stairs/east s)
          run (if (and (= (stairs-east-dir s) :east)
                       (not (stairs-east-cutout? s)))
                (:run-m e 0)
                0)
          min-x (+ (:max-x fp) (:terrace/east-extend-m s 0) run)
          max-x (+ (:max-x fp) (east-low-extend-m s))
          min-y (- (:min-y fp) depth)
          max-y (:min-y fp)]
      (when (pos? (- max-x min-x))
        (xy-rect min-x min-y max-x max-y)))))

(defn terrace-west-footprint
  "West wrap: house west → bod east (kisses bod); terrace south depth → bod north."
  [s]
  (let [fp (house/house-footprint s)
        bod (house/bod-footprint s)
        depth (:terrace/depth-m s)
        min-y (- (:min-y fp) depth)
        max-y (:max-y bod)]
    (xy-rect (:max-x bod) min-y (:min-x fp) max-y)))

(defn terrace-west-bridge-footprint
  "Gap bridge: driveway retaining-wall east face → house west; bod north → house north."
  [s]
  (let [fp (house/house-footprint s)
        bod (house/bod-footprint s)
        th (:driveway/east-wall-thickness-m s 0.2)
        x-drv (first (first (driveway/driveway-polygon-xy s)))
        min-x (+ x-drv th)
        max-x (:min-x fp)
        min-y (:max-y bod)
        max-y (:max-y fp)]
    (xy-rect min-x min-y max-x max-y)))

(defn terrace-bod-south-footprint
  "Deck south of bod: full bod width, terrace south depth up to bod south (kisses bod)."
  [s]
  (let [fp (house/house-footprint s)
        bod (house/bod-footprint s)
        depth (:terrace/depth-m s)
        min-y (- (:min-y fp) depth)
        max-y (:min-y bod)]
    (xy-rect (:min-x bod) min-y (:max-x bod) max-y)))

(defn stairs-east-footprint
  "East stair plan: :north = full east-bay width descending north;
  :south = width-m (default east-bay) at south edge descending south;
  :east + cutout? = SE cutout west→east; :east without cutout = external full-depth
  flight at high south deck east edge."
  [s]
  (let [fp (house/house-footprint s)
        e (:stairs/east s)]
    (case (stairs-east-dir s)
      :north (xy-rect (:max-x fp) (:min-y fp)
                      (terrace-east-x s) (+ (:min-y fp) (:run-m e)))
      :south (let [south (terrace-south-footprint s)
                   width (:width-m e (:terrace/east-extend-m s 0))
                   max-x (:max-x south)
                   min-x (- max-x width)
                   max-y (:min-y south)
                   min-y (- max-y (:run-m e))]
               (xy-rect min-x min-y max-x max-y))
      (if (stairs-east-cutout? s)
        (let [max-y (- (:min-y fp) (:south-offset-m e))
              min-y (- max-y (:depth-m e))
              max-x (terrace-east-x s)
              min-x (- max-x (:run-m e))]
          (xy-rect min-x min-y max-x max-y))
        (let [south (terrace-south-footprint s)
              min-x (:max-x south)
              max-x (+ min-x (:run-m e))]
          (xy-rect min-x (:min-y south) max-x (:max-y south)))))))

(defn terrace-south-low-footprint
  "Low pad south of south-facing east stairs; nil when south-low off."
  [s]
  (when (south-low? s)
    (let [st (stairs-east-footprint s)
          depth (south-low-depth-m s)
          max-y (:min-y st)
          min-y (- max-y depth)]
      (xy-rect (:min-x st) min-y (:max-x st) max-y))))

(defn terrace-south-pieces
  "South deck rects: with SE open notch (main|apron) or solid main when no cutout."
  [s]
  (let [full (terrace-south-footprint s)]
    (if (stairs-east-cutout? s)
      (let [st (stairs-east-footprint s)
            main (xy-rect (:min-x full) (:min-y full) (:min-x st) (:max-y full))
            apron (xy-rect (:min-x st) (:min-y full) (:max-x full) (:min-y st))
            cutout (xy-rect (:min-x st) (:min-y st) (:max-x full) (:max-y full))]
        {:envelope full :main main :apron apron :cutout cutout})
      {:envelope full :main full :apron nil :cutout nil})))

(defn terrace-roofed-footprint
  "Roofed terrace: flush to house south; west-inset; east = south-door east + offset."
  [s]
  (let [fp (house/house-footprint s)
        door (house/door-south-footprint s)
        ry (:terrace/roofed-depth-m s)
        inset (:terrace/roofed-west-inset-m s)
        east-of (:terrace/roofed-east-of-south-door-m s)
        min-x (+ (:min-x fp) inset)
        max-x (+ (:max-x door) east-of)
        max-y (:min-y fp)
        min-y (- max-y ry)]
    (xy-rect min-x min-y max-x max-y)))

(defn- add-stair-flight!
  "Steps descending along +X (east), +Y (north), or −Y (south)."
  [prefix {:keys [dir a0 a1 start-b run drop steps]} z-top]
  (let [span (- a1 a0)
        run-i (/ run steps)
        drop-i (/ drop steps)
        names (atom [])]
    (doseq [i (range steps)]
      (let [sz (max 0.06 drop-i)
            mid-a (/ (+ a0 a1) 2.0)
            b-off (* (+ i 0.5) run-i)
            b (if (= dir :south)
                (- start-b b-off)
                (+ start-b b-off))
            z-tread (- z-top (* (inc i) drop-i))
            cz (+ z-tread (/ sz 2.0))
            n (str prefix "-" i)
            [sx sy cx cy] (case dir
                            :east [run-i span b mid-a]
                            :north [span run-i mid-a b]
                            :south [span run-i mid-a b])]
        (mesh/add-box! n [sx sy sz] [cx cy cz])
        (swap! names conj n)))
    @names))

(defn- railing-deck-context
  [s]
  (let [{:keys [inset]} (railing/railing-opts s)
        west (terrace-west-footprint s)
        bod-s (terrace-bod-south-footprint s)
        {:keys [main apron cutout]} (terrace-south-pieces s)
        st (stairs-east-footprint s)
        y-south (+ (:min-y west) inset)
        x-west (+ (:min-x bod-s) inset)
        y-bod-s (- (:max-y bod-s) inset)]
    {:inset inset :west west :bod-s bod-s :main main :apron apron :cutout cutout
     :st st :y-south y-south :x-west x-west :y-bod-s y-bod-s}))

(defn- railing-deck-runs-cutout
  [{:keys [west bod-s apron st cutout y-south x-west y-bod-s inset]}]
  (let [x-east (- (:max-x apron) inset)
        x-cut (- (:min-x st) inset)
        y-apron-n (- (:min-y st) inset)]
    [["site-railing-south" {:x0 (:min-x bod-s) :y0 y-south :x1 (:max-x apron) :y1 y-south}]
     ["site-railing-west" {:x0 x-west :y0 (:min-y west) :x1 x-west :y1 y-bod-s}]
     ["site-railing-east" {:x0 x-east :y0 (:min-y apron) :x1 x-east :y1 (:max-y apron)}]
     ["site-railing-apron-n" {:x0 (:min-x apron) :y0 y-apron-n :x1 (:max-x apron) :y1 y-apron-n}]
     ["site-railing-cutout-w" {:x0 x-cut :y0 (:max-y st) :x1 x-cut :y1 (:max-y cutout)}]]))

(defn- railing-deck-runs-solid
  [s {:keys [west bod-s main st y-south x-west y-bod-s inset]}]
  (let [x-east (- (:max-x main) inset)
        y-east-n (- (:max-y main) inset)
        south-rail (if (stairs-east-south? s)
                     ["site-railing-south"
                      {:x0 (:min-x bod-s) :y0 y-south
                       :x1 (- (:min-x st) inset) :y1 y-south}]
                     ["site-railing-south"
                      {:x0 (:min-x bod-s) :y0 y-south
                       :x1 (:max-x main) :y1 y-south}])
        runs [south-rail
              ["site-railing-west" {:x0 x-west :y0 (:min-y west) :x1 x-west :y1 y-bod-s}]]]
    (if (east-low? s)
      runs
      (conj runs ["site-railing-east" {:x0 x-east :y0 (:min-y main) :x1 x-east :y1 y-east-n}]))))

(defn- railing-east-low-runs
  "Low-pad perimeter rails: south + east. North open (near-grade lawn access)."
  [s]
  (when-let [low (terrace-east-low-footprint s)]
    (let [{:keys [inset]} (railing/railing-opts s)
          x-east (- (:max-x low) inset)
          y-south (+ (:min-y low) inset)
          y-north (- (:max-y low) inset)]
      [["site-railing-east-low-s" {:x0 (:min-x low) :y0 y-south :x1 x-east :y1 y-south}]
       ["site-railing-east-low-e" {:x0 x-east :y0 y-south :x1 x-east :y1 y-north}]])))

(defn- railing-south-low-runs
  "Low south pad rails: south + east + west. North open to stairs."
  [s]
  (when-let [low (terrace-south-low-footprint s)]
    (let [{:keys [inset]} (railing/railing-opts s)
          x-east (- (:max-x low) inset)
          x-west (+ (:min-x low) inset)
          y-south (+ (:min-y low) inset)
          y-north (- (:max-y low) inset)]
      [["site-railing-south-low-s" {:x0 x-west :y0 y-south :x1 x-east :y1 y-south}]
       ["site-railing-south-low-e" {:x0 x-east :y0 y-south :x1 x-east :y1 y-north}]
       ["site-railing-south-low-w" {:x0 x-west :y0 y-south :x1 x-west :y1 y-north}]])))

(defn railing-deck-runs
  "Exposed deck edges as [prefix end-map], inset applied.
  Cutout west rail only north of east stairs (entry from main stays open).
  No rail where deck kisses bod east/south; bridge west open to driveway."
  [s]
  (let [ctx (railing-deck-context s)]
    (if (stairs-east-cutout? s)
      (railing-deck-runs-cutout ctx)
      (railing-deck-runs-solid s ctx))))

(defn- stair-rail-east-north
  [{:keys [st e inset z-deck z-drop]}]
  [{:prefix "site-railing-stair-east-e"
    :steps (:steps e)
    :end {:x0 (- (:max-x st) inset)
          :y0 (:min-y st)
          :z0 z-deck
          :x1 (- (:max-x st) inset)
          :y1 (:max-y st)
          :z1 z-drop}}])

(defn- stair-rail-east-south
  [{:keys [st e inset z-deck z-drop]}]
  [{:prefix "site-railing-stair-east-w"
    :steps (:steps e)
    :end {:x0 (+ (:min-x st) inset)
          :y0 (:max-y st)
          :z0 z-deck
          :x1 (+ (:min-x st) inset)
          :y1 (:min-y st)
          :z1 z-drop}}
   {:prefix "site-railing-stair-east-e"
    :steps (:steps e)
    :end {:x0 (- (:max-x st) inset)
          :y0 (:max-y st)
          :z0 z-deck
          :x1 (- (:max-x st) inset)
          :y1 (:min-y st)
          :z1 z-drop}}])

(defn- stair-rail-east-ew
  "Default :east flight: south + north side rails."
  [{:keys [st e inset z-deck z-drop]}]
  [{:prefix "site-railing-stair-east-s"
    :steps (:steps e)
    :end {:x0 (:min-x st)
          :y0 (+ (:min-y st) inset)
          :z0 z-deck
          :x1 (:max-x st)
          :y1 (+ (:min-y st) inset)
          :z1 z-drop}}
   {:prefix "site-railing-stair-east-n"
    :steps (:steps e)
    :end {:x0 (:min-x st)
          :y0 (- (:max-y st) inset)
          :z0 z-deck
          :x1 (:max-x st)
          :y1 (- (:max-y st) inset)
          :z1 z-drop}}])

(defn- stair-railing-east-sides
  "East stair side rails. :north omits the house-wall (west) side.
  :south gets west+east sides descending south."
  [s z-deck opts]
  (let [st (stairs-east-footprint s)
        e (:stairs/east s)
        inset (:inset opts)
        z-drop (- z-deck (:drop-m e))
        ctx {:st st :e e :inset inset :z-deck z-deck :z-drop z-drop}]
    (case (stairs-east-dir s)
      :north (stair-rail-east-north ctx)
      :south (stair-rail-east-south ctx)
      (stair-rail-east-ew ctx))))

(defn ensure-terrace-railing!
  "Wooden deck + east stair railings (`site-railing-*`). Gaps at stair entries.
  West stairs have no rail."
  [s]
  (railing/clear-railings!)
  (let [opts (railing/railing-opts s)
        z-deck (railing/railing-z-deck s)
        deck-result (railing/install-railings! z-deck opts
                                               (railing-deck-runs s)
                                               (stair-railing-east-sides s z-deck opts))
        low-named (concat
                   (map (fn [[p e]] [p e (east-low-top-z s)])
                        (or (railing-east-low-runs s) []))
                   (map (fn [[p e]] [p e (south-low-top-z s)])
                        (or (railing-south-low-runs s) [])))]
    (if (seq low-named)
      (let [low-names (mapcat (fn [[prefix end z]]
                                (railing/add-railing-run! prefix end z opts))
                              low-named)]
        (-> deck-result
            (update :names into low-names)
            (update :count + (count low-names))))
      deck-result)))

(defn- support-edge-context
  [s]
  (let [{:keys [inset]} (supports/support-opts s)
        setback (:terrace/support-south-setback-m s 0.0)
        west (terrace-west-footprint s)
        bridge (terrace-west-bridge-footprint s)
        bod-s (terrace-bod-south-footprint s)
        {:keys [main apron cutout]} (terrace-south-pieces s)
        st (stairs-east-footprint s)
        x-west (+ (:min-x bod-s) inset)
        y-south (+ (:min-y west) inset setback)
        y-bod-s (- (:max-y bod-s) inset)
        x-bridge-w (+ (:min-x bridge) inset)
        y-bridge-n (- (:max-y bridge) inset)]
    {:inset inset :west west :bridge bridge :bod-s bod-s :main main :apron apron
     :cutout cutout :st st :x-west x-west :y-south y-south :y-bod-s y-bod-s
     :x-bridge-w x-bridge-w :y-bridge-n y-bridge-n}))

(defn- support-edge-runs-cutout
  [{:keys [apron cutout st x-west y-south y-bod-s y-bridge-n x-bridge-w bridge inset]}]
  (let [x-east (- (:max-x apron) inset)
        y-apron-n (- (:min-y st) inset)
        x-cut (- (:min-x st) inset)
        y-cut-n (- (:max-y cutout) inset)]
    [{:x0 x-west :y0 y-south :x1 x-east :y1 y-south}
     {:x0 x-west :y0 y-south :x1 x-west :y1 y-bod-s}
     {:x0 x-bridge-w :y0 (:min-y bridge) :x1 x-bridge-w :y1 y-bridge-n}
     {:x0 x-east :y0 y-south :x1 x-east :y1 y-apron-n}
     {:x0 x-cut :y0 y-apron-n :x1 x-east :y1 y-apron-n}
     {:x0 x-cut :y0 (:max-y st) :x1 x-cut :y1 y-cut-n}]))

(defn- support-edge-runs-solid
  [s {:keys [main st x-west y-south y-bod-s y-bridge-n x-bridge-w bridge inset]}]
  (let [x-east (- (:max-x main) inset)
        y-east-n (- (:max-y main) inset)
        x-south-1 (if (stairs-east-south? s)
                    (- (:min-x st) inset)
                    x-east)
        runs [{:x0 x-west :y0 y-south :x1 x-south-1 :y1 y-south}
              {:x0 x-west :y0 y-south :x1 x-west :y1 y-bod-s}
              {:x0 x-bridge-w :y0 (:min-y bridge) :x1 x-bridge-w :y1 y-bridge-n}]]
    (if (east-low? s)
      runs
      (conj runs {:x0 x-east :y0 y-south :x1 x-east :y1 y-east-n}))))

(defn- support-edge-runs-east-low
  [s]
  (when-let [low (terrace-east-low-footprint s)]
    (let [{:keys [inset]} (supports/support-opts s)
          x-east (- (:max-x low) inset)
          y-south (+ (:min-y low) inset)
          y-north (- (:max-y low) inset)]
      [{:x0 (:min-x low) :y0 y-south :x1 x-east :y1 y-south}
       {:x0 x-east :y0 y-south :x1 x-east :y1 y-north}
       {:x0 (:min-x low) :y0 y-north :x1 x-east :y1 y-north}])))

(defn- support-edge-runs-south-low
  [s]
  (when-let [low (terrace-south-low-footprint s)]
    (let [{:keys [inset]} (supports/support-opts s)
          x-east (- (:max-x low) inset)
          x-west (+ (:min-x low) inset)
          y-south (+ (:min-y low) inset)
          y-north (- (:max-y low) inset)]
      [{:x0 x-west :y0 y-south :x1 x-east :y1 y-south}
       {:x0 x-east :y0 y-south :x1 x-east :y1 y-north}
       {:x0 x-west :y0 y-south :x1 x-west :y1 y-north}])))

(defn- install-pad-supports!
  [prefix z-top edges opts]
  (when (seq edges)
    (let [pts (supports/xy-points-from-edges edges (:spacing opts))
          {:keys [length size]} opts
          names (mapv (fn [i [x y]]
                        (let [n (str prefix i)]
                          (mesh/add-box! n [size size length]
                                         [x y (- z-top (/ length 2.0))])
                          n))
                      (range) pts)]
      {:names names :count (count names) :z-top z-top})))

(defn- install-low-supports!
  [s opts]
  (let [east (install-pad-supports!
              "site-terrace-post-low-"
              (east-low-floor-z s)
              (support-edge-runs-east-low s)
              opts)
        south (install-pad-supports!
               "site-terrace-post-south-low-"
               (south-low-floor-z s)
               (support-edge-runs-south-low s)
               opts)]
    (cond
      (and east south)
      (-> east
          (update :names into (:names south))
          (update :count + (:count south)))
      east east
      south south
      :else nil)))

(defn support-edge-runs
  "Outer deck edges for ground posts as end-maps, inset; shared corner coords.
  South run uses :terrace/support-south-setback-m so posts can stay when deck extends south."
  [s]
  (let [ctx (support-edge-context s)]
    (if (stairs-east-cutout? s)
      (support-edge-runs-cutout ctx)
      (support-edge-runs-solid s ctx))))

(defn ensure-terrace-supports!
  "Ground posts under outer terrace edges (`site-terrace-post-*`)."
  [s]
  (supports/clear-supports!)
  (let [opts (supports/support-opts s)
        z-top (:house/floor-z s)
        pts (supports/xy-points-from-edges (support-edge-runs s) (:spacing opts))
        high-result (supports/install-supports! z-top opts pts)
        low-result (install-low-supports! s opts)]
    (if low-result
      (-> high-result
          (update :names into (:names low-result))
          (update :count + (:count low-result)))
      high-result)))

(defn- clear-terrace-scene!
  "Remove terrace slabs, stairs, railings, and support posts."
  []
  (mesh/unlink-and-remove! "site-terrace")
  (run! mesh/unlink-and-remove!
        (concat ["site-terrace-south" "site-terrace-bod-south"
                 "site-terrace-west" "site-terrace-west-bridge"
                 "site-terrace-east-low" "site-terrace-south-low"]
                (filter #(string/starts-with? % "site-terrace-south-")
                        (mesh/site-object-names))
                (filter #(or (string/starts-with? % "site-stair-west")
                             (string/starts-with? % "site-stair-east")
                             (string/starts-with? % "site-stair-sw")
                             (string/starts-with? % "site-stair-se"))
                        (mesh/site-object-names))
                (filter #(string/starts-with? % "site-railing")
                        (mesh/site-object-names))
                (filter #(string/starts-with? % "site-terrace-post")
                        (mesh/site-object-names)))))

(defn- build-slab-piece!
  [piece-name piece tz floor-z]
  (let [size [(:sx piece) (:sy piece) tz]
        xy (mesh/rect-center-xy (:min-x piece) (:min-y piece)
                                (:max-x piece) (:max-y piece))]
    {:name (.-name (mesh/add-box! piece-name size
                                  (mesh/box-center size xy floor-z)))
     :size size
     :footprint piece}))

(defn- build-terrace-slabs!
  [s]
  (let [tz (:terrace/slab-thickness-m s)
        floor-z (:house/floor-z s)
        pieces (terrace-south-pieces s)
        west (terrace-west-footprint s)
        bridge (terrace-west-bridge-footprint s)
        bod-s (terrace-bod-south-footprint s)
        south (cond-> {:main (build-slab-piece! "site-terrace-south-main" (:main pieces) tz floor-z)
                       :cutout (:cutout pieces)}
                (:apron pieces) (assoc :apron (build-slab-piece! "site-terrace-south-apron"
                                                                  (:apron pieces) tz floor-z)))
        east-low (when-let [fp (terrace-east-low-footprint s)]
                   (build-slab-piece! "site-terrace-east-low" fp tz (east-low-floor-z s)))
        south-low (when-let [fp (terrace-south-low-footprint s)]
                    (build-slab-piece! "site-terrace-south-low" fp tz (south-low-floor-z s)))]
    (cond-> {:south south
             :bod-south (build-slab-piece! "site-terrace-bod-south" bod-s tz floor-z)
             :west (build-slab-piece! "site-terrace-west" west tz floor-z)
             :west-bridge (build-slab-piece! "site-terrace-west-bridge" bridge tz floor-z)}
      east-low (assoc :east-low east-low)
      south-low (assoc :south-low south-low))))

(defn- build-east-stairs!
  [s z-top]
  (let [st (stairs-east-footprint s)
        e (:stairs/east s)]
    (case (stairs-east-dir s)
      :north
      (add-stair-flight!
       "site-stair-east"
       {:dir :north
        :a0 (:min-x st) :a1 (:max-x st)
        :start-b (:min-y st)
        :run (:run-m e) :drop (:drop-m e) :steps (:steps e)}
       z-top)
      :south
      (add-stair-flight!
       "site-stair-east"
       {:dir :south
        :a0 (:min-x st) :a1 (:max-x st)
        :start-b (:max-y st)
        :run (:run-m e) :drop (:drop-m e) :steps (:steps e)}
       z-top)
      (add-stair-flight!
       "site-stair-east"
       {:dir :east
        :a0 (:min-y st) :a1 (:max-y st)
        :start-b (:min-x st)
        :run (:run-m e) :drop (:drop-m e) :steps (:steps e)}
       z-top))))

(defn- build-terrace-stairs!
  [s]
  (let [z-top (:house/floor-z s)
        bridge (terrace-west-bridge-footprint s)
        w (:stairs/west s)]
    {:west (add-stair-flight!
            "site-stair-west"
            {:dir :north
             :a0 (:min-x bridge) :a1 (:max-x bridge)
             :start-b (:max-y bridge)
             :run (:run-m w) :drop (:drop-m w) :steps (:steps w)}
            z-top)
     :east (build-east-stairs! s z-top)}))

(defn ensure-terrace!
  "Terrace slabs (west wrap + west bridge to house north; full-width south of bod;
  optional east-low pad), west/east stairs, railing, supports."
  [s]
  (clear-terrace-scene!)
  (let [slabs (build-terrace-slabs! s)
        stairs (build-terrace-stairs! s)
        support-result (ensure-terrace-supports! s)
        railing-result (ensure-terrace-railing! s)]
    (assoc slabs
           :stairs stairs
           :supports support-result
           :railing railing-result)))

(defn- canopy-pitch-rad
  [s]
  (math/radians (:terrace/roof-pitch-deg s)))

(defn- deck-top-z
  [s]
  (+ (:house/floor-z s) (:terrace/slab-thickness-m s)))

(defn- canopy-z-attach
  "Underside Z at house south wall, from clearance measured south of the wall."
  [s]
  (let [clear (:terrace/canopy-clearance-m s)
        south-of (:terrace/canopy-clearance-south-of-house-m s 0.0)
        clear-at-wall (+ clear (* south-of (math/tan (canopy-pitch-rad s))))]
    (+ (deck-top-z s) clear-at-wall)))

(defn terrace-roof-footprint
  "Roof plan: roofed width; south edge = terrace south − overhang."
  [s]
  (let [rf (terrace-roofed-footprint s)
        south (terrace-south-footprint s)
        overhang (:terrace/roof-overhang-m s 0.5)
        min-y (- (:min-y south) overhang)]
    {:min-x (:min-x rf) :max-x (:max-x rf)
     :min-y min-y :max-y (:max-y rf)
     :sx (:sx rf) :sy (- (:max-y rf) min-y)}))

(defn- roof-z-at-y
  "Pitched underside Z at house-NW y (drops south from house)."
  [s y]
  (let [y-house (:max-y (terrace-roofed-footprint s))
        d (max 0.0 (- y-house y))
        drop (* d (math/tan (canopy-pitch-rad s)))]
    (- (canopy-z-attach s) drop)))

(defn- add-terrace-roof-covering!
  "Pitched slab mesh site-terrace-roof-covering (vertical thickness)."
  [s]
  (let [rp (terrace-roof-footprint s)
        thick (:terrace/roof-thickness-m s 0.08)
        {:keys [min-x max-x min-y max-y]} rp
        z-n (roof-z-at-y s max-y)
        z-s (roof-z-at-y s min-y)
        bottom [[min-x min-y z-s]
                [max-x min-y z-s]
                [max-x max-y z-n]
                [min-x max-y z-n]]
        top (mapv (fn [[x y z]] [x y (+ z thick)]) bottom)
        verts (vec (concat bottom top))
        faces [[0 1 2 3]
               [7 6 5 4]
               [0 1 5 4]
               [1 2 6 5]
               [2 3 7 6]
               [3 0 4 7]]]
    (mesh/add-mesh! "site-terrace-roof-covering" verts faces)
    {:name "site-terrace-roof-covering" :footprint rp :z-attach z-n :z-south z-s}))

(defn- beam-orient-along!
  [obj {:keys [x0 y0 z0 x1 y1 z1]}]
  (let [dir (.normalized (mathutils/Vector #py [(double (- x1 x0))
                                                (double (- y1 y0))
                                                (double (- z1 z0))]))]
    (set! (.-rotation-euler obj) (.to-euler (.to-track-quat dir "X" "Z")))
    obj))

(defn- add-beam-segment!
  "Box beam along segment; square cross-section beam."
  [name {:keys [x0 y0 z0 x1 y1 z1]} beam]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        dz (- z1 z0)
        len (math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
        cx (/ (+ x0 x1) 2.0)
        cy (/ (+ y0 y1) 2.0)
        cz (/ (+ z0 z1) 2.0)]
    (mesh/add-box! name [len beam beam] [cx cy cz])
    (beam-orient-along! (mesh/object-by-name name)
                        {:x0 x0 :y0 y0 :z0 z0 :x1 x1 :y1 y1 :z1 z1})
    name))

(defn- add-axis-beam!
  "Axis-aligned beam along X or Y centered on underside z."
  [name {:keys [x0 y0 x1 y1 z beam]}]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        len (math/hypot dx dy)
        cx (/ (+ x0 x1) 2.0)
        cy (/ (+ y0 y1) 2.0)
        cz (- z (/ beam 2.0))
        along-x? (> (math/fabs dx) (math/fabs dy))
        sx (if along-x? len beam)
        sy (if along-x? beam len)]
    (mesh/add-box! name [sx sy beam] [cx cy cz])
    name))

(defn- add-terrace-roof-frame-beams!
  "Perimeter beams along roof underside."
  [s]
  (let [rp (terrace-roof-footprint s)
        beam (:terrace/roof-beam-size-m s 0.08)
        {:keys [min-x max-x min-y max-y]} rp
        z-n (roof-z-at-y s max-y)
        z-s (roof-z-at-y s min-y)]
    [(add-axis-beam! "site-terrace-roof-frame-n"
                      {:x0 min-x :y0 max-y :x1 max-x :y1 max-y :z z-n :beam beam})
     (add-axis-beam! "site-terrace-roof-frame-s"
                      {:x0 min-x :y0 min-y :x1 max-x :y1 min-y :z z-s :beam beam})
     (add-beam-segment! "site-terrace-roof-frame-w"
                        {:x0 min-x :y0 min-y :z0 z-s
                         :x1 min-x :y1 max-y :z1 z-n}
                        beam)
     (add-beam-segment! "site-terrace-roof-frame-e"
                        {:x0 max-x :y0 min-y :z0 z-s
                         :x1 max-x :y1 max-y :z1 z-n}
                        beam)]))

(defn- add-terrace-roof-poles!
  "Evenly spaced poles at terrace south edge under roofed width."
  [s]
  (let [rp (terrace-roof-footprint s)
        south-y (:min-y (terrace-south-footprint s))
        inset 0.08
        y-pole (+ south-y inset)
        z-deck (deck-top-z s)
        z-under (roof-z-at-y s y-pole)
        pole-h (max 0.05 (- z-under z-deck))
        pole-size (:terrace/roof-pole-size-m s 0.12)
        n (:terrace/roof-pole-count s)
        x0 (+ (:min-x rp) inset)
        x1 (- (:max-x rp) inset)
        xs (if (= n 1)
             [x0]
             (mapv (fn [i] (+ x0 (* (/ (double i) (dec n)) (- x1 x0))))
                   (range n)))]
    (mapv (fn [i x]
            (let [pole-name (str "site-terrace-roof-pole-" i)
                  cz (+ z-deck (/ pole-h 2.0))]
              (mesh/add-box! pole-name [pole-size pole-size pole-h] [x y-pole cz])
              pole-name))
          (range) xs)))

(defn- clear-terrace-roof-scene!
  "Remove canopy covering, frame beams, and poles (incl. legacy roof name)."
  []
  (run! mesh/unlink-and-remove!
        (concat ["site-terrace-roof" "site-terrace-roof-covering"]
                (filter #(string/starts-with? % "site-terrace-roof-frame")
                        (mesh/site-object-names))
                (filter #(string/starts-with? % "site-terrace-roof-pole")
                        (mesh/site-object-names)))))

(defn ensure-terrace-roof!
  "Pitched canopy covering + timber frame beams + south-edge poles."
  [s]
  (clear-terrace-roof-scene!)
  (let [covering (add-terrace-roof-covering! s)
        frame (add-terrace-roof-frame-beams! s)
        poles (add-terrace-roof-poles! s)]
    (merge covering
           {:covering (:name covering)
            :frame frame
            :poles poles
            :pitch-deg (:terrace/roof-pitch-deg s)
            :opacity (:terrace/roof-opacity s 1.0)
            :overhang-m (:terrace/roof-overhang-m s 0.5)})))

(def ^:private roof-covering-name "site-terrace-roof-covering")

(defn- set-roof-covering-hide!
  [obj hide?]
  (set! (.-hide_viewport obj) hide?)
  (set! (.-hide_render obj) hide?))

(defn set-terrace-roof-covering-visible!
  "Show/hide canopy covering (frame stays). Returns {:name :visible?}."
  [visible?]
  (when-let [obj (mesh/object-by-name roof-covering-name)]
    (set-roof-covering-hide! obj (not visible?)))
  {:name roof-covering-name :visible? visible?})

(defn toggle-terrace-roof-covering!
  "Toggle canopy covering visibility. Returns {:name :visible?}."
  []
  (if-let [obj (mesh/object-by-name roof-covering-name)]
    (let [visible? (not (.-hide_viewport obj))]
      (set-roof-covering-hide! obj visible?)
      {:name roof-covering-name :visible? (not visible?)})
    {:name roof-covering-name :visible? true}))
