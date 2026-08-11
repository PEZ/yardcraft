(ns yardcraft.site-furniture
  "Schematic terrace furniture for fit-checking: tables, chairs, loungers, BBQ."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-railing :as railing])
  (:import math))

(defn- rot-xy
  "Rotate local [x y] by deg degrees."
  [[x y] deg]
  (let [r (math/radians (double deg))
        c (math/cos r)
        s (math/sin r)]
    [(+ (* x c) (* y (- s)))
     (+ (* x s) (* y c))]))

(defn- world-xy
  "Local [lx ly] rotated by deg, offset from origin [ox oy]."
  [[ox oy] [lx ly] deg]
  (let [[rx ry] (rot-xy [lx ly] deg)]
    [(+ ox rx) (+ oy ry)]))

(defn- compose-pose
  "World pose from group {:xy :rot-z-deg} ⊗ local {:xy :rot-z-deg}."
  [group local]
  (let [gxy (:xy group)
        gr (double (:rot-z-deg group 0))
        lxy (:xy local [0 0])
        lr (double (:rot-z-deg local 0))]
    {:xy (vec (world-xy gxy lxy gr))
     :rot-z-deg (+ gr lr)}))

(defn- set-rot-euler!
  "Set object Euler XYZ from optional :rot-x-deg :rot-z-deg (degrees)."
  [obj {:keys [rot-x-deg rot-z-deg] :or {rot-x-deg 0 rot-z-deg 0}}]
  (set! (.-rotation-euler obj)
        #py [(math/radians (double rot-x-deg))
             0.0
             (math/radians (double rot-z-deg))])
  obj)

(defn- add-oriented-box!
  "Box at world position from origin+local+rot; bottom at z0."
  [n {:keys [size local xy rot-x-deg rot-z-deg z0]
      :or {rot-x-deg 0 rot-z-deg 0}}]
  (let [[wx wy] (world-xy xy local rot-z-deg)
        center (mesh/box-center size [wx wy] z0)]
    (mesh/add-box! n size center)
    (when-let [obj (mesh/object-by-name n)]
      (set-rot-euler! obj {:rot-x-deg rot-x-deg :rot-z-deg rot-z-deg}))
    n))

(defn- add-oriented-cylinder!
  "Cylinder bottom at z0; XY from origin+local+rot-z."
  [n {:keys [radius height local xy rot-z-deg z0]}]
  (let [[wx wy] (world-xy xy local rot-z-deg)
        cz (+ z0 (/ (double height) 2.0))]
    (mesh/add-cylinder! n radius height [wx wy cz])
    (when-let [obj (mesh/object-by-name n)]
      (set-rot-euler! obj {:rot-z-deg rot-z-deg}))
    n))

(defn- leg-corners
  "Four inset corner offsets for footprint [sx sy]."
  [[sx sy] inset]
  (let [ix (- (/ sx 2.0) inset)
        iy (- (/ sy 2.0) inset)]
    [[(- ix) (- iy)]
     [ix (- iy)]
     [ix iy]
     [(- ix) iy]]))

(defn- add-legs!
  "Four corner legs under footprint from z0."
  [prefix {:keys [footprint legs place z0]}]
  (let [{:keys [size inset height]} legs
        {:keys [xy rot-z-deg]} place
        leg-size [size size height]]
    (mapv (fn [i local]
            (let [n (str prefix i)]
              (add-oriented-box! n {:size leg-size
                                    :local local
                                    :xy xy
                                    :rot-z-deg rot-z-deg
                                    :z0 z0})
              n))
          (range)
          (leg-corners footprint inset))))

(defn- add-table!
  [prefix {:keys [size-m height-m top-thickness-m xy rot-z-deg]} z0 legs]
  (let [[sx sy] size-m
        leg-h (- height-m top-thickness-m)
        top-bottom (+ z0 leg-h)
        top-n (str prefix "-top")
        leg-prefix (str prefix "-leg-")
        leg-opts {:footprint [sx sy]
                  :legs (assoc legs :height leg-h)
                  :place {:xy xy :rot-z-deg rot-z-deg}
                  :z0 z0}
        leg-names (add-legs! leg-prefix leg-opts)]
    (add-oriented-box! top-n {:size [sx sy top-thickness-m]
                              :local [0 0]
                              :xy xy
                              :rot-z-deg rot-z-deg
                              :z0 top-bottom})
    (into [top-n] leg-names)))

(defn- add-oriented-box-center!
  "Box at world center; local [lx ly] → world XY at lz center Z."
  [n {:keys [size local xy rot-x-deg rot-z-deg lz]
      :or {rot-x-deg 0 rot-z-deg 0}}]
  (let [[wx wy] (world-xy xy local rot-z-deg)
        center [wx wy (double lz)]]
    (mesh/add-box! n size center)
    (when-let [obj (mesh/object-by-name n)]
      (set-rot-euler! obj {:rot-x-deg rot-x-deg :rot-z-deg rot-z-deg}))
    n))

(defn- add-tilted-back!
  "Lounger backrest tilted from vertical toward local +Y."
  [back-n {:keys [w t h d xy rot-z-deg seat-top tilt-deg]}]
  (let [θ (math/radians (double tilt-deg))
        bottom-y (+ (/ d 2.0) (/ t 2.0))
        cy (+ bottom-y (* (/ h 2.0) (math/sin θ)))
        cz (+ seat-top (* (/ h 2.0) (math/cos θ)))]
    (add-oriented-box-center! back-n {:size [w t h]
                                      :local [0 cy]
                                      :xy xy
                                      :rot-x-deg (- (double tilt-deg))
                                      :rot-z-deg rot-z-deg
                                      :lz cz})))

(defn- add-seat-back!
  "Seat + backrest at local +Y + legs. geom: :seat-size-m or :size-m, seat/back dims."
  [prefix {:keys [place geom z0 legs]}]
  (let [{:keys [xy rot-z-deg]} place
        {:keys [seat-size-m size-m seat-height-m seat-thickness-m
                back-height-m back-thickness-m back-tilt-deg]} geom
        [w d] (or seat-size-m size-m)
        leg-h (- seat-height-m seat-thickness-m)
        seat-bottom (+ z0 leg-h)
        back-bottom (+ seat-bottom seat-thickness-m)
        back-y (+ (/ d 2.0) (/ back-thickness-m 2.0))
        seat-n (str prefix "-seat")
        back-n (str prefix "-back")
        leg-prefix (str prefix "-leg-")
        leg-opts {:footprint [w d]
                  :legs (assoc legs :height leg-h)
                  :place {:xy xy :rot-z-deg rot-z-deg}
                  :z0 z0}
        leg-names (add-legs! leg-prefix leg-opts)
        box (fn [n size local z]
              (add-oriented-box! n {:size size
                                    :local local
                                    :xy xy
                                    :rot-z-deg rot-z-deg
                                    :z0 z}))]
    (box seat-n [w d seat-thickness-m] [0 0] seat-bottom)
    (if back-tilt-deg
      (add-tilted-back! back-n {:w w :t back-thickness-m :h back-height-m :d d
                                :xy xy :rot-z-deg rot-z-deg :seat-top back-bottom
                                :tilt-deg back-tilt-deg})
      (box back-n [w back-thickness-m back-height-m] [0 back-y] back-bottom))
    (into [seat-n back-n] leg-names)))

(defn- dining-chair-specs
  "Chair placements in group-local frame around table local pose."
  [table chair-geom]
  (let [{:keys [size-m xy rot-z-deg]} table
        {:keys [seat-size-m clearance-m]} chair-geom
        [sx sy] size-m
        [_ d] seat-size-m
        south-y (- (+ (/ sy 2.0) clearance-m (/ d 2.0)))
        north-y (+ (/ sy 2.0) clearance-m (/ d 2.0))
        west-x (- (+ (/ sx 2.0) clearance-m (/ d 2.0)))
        east-x (+ (/ sx 2.0) clearance-m (/ d 2.0))
        xs [(- (/ sx 3.0)) 0.0 (/ sx 3.0)]
        table-pose {:xy (or xy [0 0]) :rot-z-deg (or rot-z-deg 0)}
        place (fn [lx ly face-deg]
                (compose-pose table-pose {:xy [lx ly] :rot-z-deg face-deg}))]
    (vec (concat (map #(place % south-y 180.0) xs)
                 (map #(place % north-y 0.0) xs)
                 [(place west-x 0.0 90.0)]
                 [(place east-x 0.0 -90.0)]))))

(defn- shelf-local-x
  [shelf-side body-sx shelf-sx]
  (let [half-body (/ body-sx 2.0)
        half-shelf (/ shelf-sx 2.0)]
    (case shelf-side
      :east (+ half-body half-shelf)
      :west (- (- half-body) half-shelf)
      (+ half-body half-shelf))))

(defn- add-bbq!
  [prefix {:keys [body-size-m body-height-m lid-height-m shelf-size-m
                  shelf-thickness-m shelf-height-m shelf-side xy rot-z-deg]}
         z0]
  (let [[bsx bsy] body-size-m
        [ssx ssy] shelf-size-m
        body-n (str prefix "-body")
        lid-n (str prefix "-lid")
        shelf-n (str prefix "-shelf")
        lid-bottom (+ z0 body-height-m)
        shelf-x (shelf-local-x shelf-side bsx ssx)
        box (fn [n size local z]
              (add-oriented-box! n {:size size
                                    :local local
                                    :xy xy
                                    :rot-z-deg rot-z-deg
                                    :z0 z}))]
    (box body-n [bsx bsy body-height-m] [0 0] z0)
    (box lid-n [bsx bsy lid-height-m] [0 0] lid-bottom)
    (box shelf-n [ssx ssy shelf-thickness-m] [shelf-x 0] (+ z0 shelf-height-m))
    [body-n lid-n shelf-n]))

(defn clear-furniture!
  "Remove all site-furniture* objects."
  []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-furniture")
                (mesh/site-object-names))))

(defn- ensure-table-chairs!
  [{:keys [table-prefix chair-prefix table chair placements z0 legs]}]
  (let [table-names (add-table! table-prefix table z0 legs)
        chair-names
        (mapcat
         (fn [i placement]
           (add-seat-back! (str chair-prefix i)
                           {:place placement :geom chair :z0 z0 :legs legs}))
         (range)
         placements)]
    (into table-names chair-names)))

(defn- world-placement
  "Compose group pose onto a local placement map."
  [group placement]
  (let [pose (compose-pose group placement)]
    (assoc placement :xy (:xy pose) :rot-z-deg (:rot-z-deg pose))))

(defn- table-set-config
  [{:keys [table-prefix chair-prefix set-map z0 legs]}]
  (let [group (select-keys set-map [:xy :rot-z-deg])
        table-local (:table set-map)
        table-pose (compose-pose group table-local)
        table-world (assoc table-local
                           :xy (:xy table-pose)
                           :rot-z-deg (:rot-z-deg table-pose))
        chair-locals (or (:chairs set-map)
                         (dining-chair-specs table-local (:chair set-map)))
        chair-worlds (mapv #(world-placement group %) chair-locals)]
    {:table-prefix table-prefix
     :chair-prefix chair-prefix
     :table table-world
     :chair (:chair set-map)
     :placements chair-worlds
     :z0 z0
     :legs legs}))

(defn- table-chair-configs
  "Dining (auto chair ring) + cafe (explicit chairs) configs for ensure-table-chairs!."
  [s z0 legs]
  (cond-> []
    (:furniture/dining s)
    (conj (table-set-config {:table-prefix "site-furniture-dining-table"
                             :chair-prefix "site-furniture-dining-chair-"
                             :set-map (:furniture/dining s)
                             :z0 z0
                             :legs legs}))
    (:furniture/cafe s)
    (conj (table-set-config {:table-prefix "site-furniture-cafe-table"
                             :chair-prefix "site-furniture-cafe-chair-"
                             :set-map (:furniture/cafe s)
                             :z0 z0
                             :legs legs}))))

(defn lounger-rot-z-to-face-sun
  "House-NW Z yaw (deg) so lounger face (local −Y) points at sun azimuth."
  [s]
  (let [az (double (:sun/azimuth-deg s 180.0))
        th (double (:site/north-offset-deg s 0.0))
        sx (math/sin (math/radians (- az th)))
        sy (math/cos (math/radians (- az th)))]
    (math/degrees (math/atan2 sx (- sy)))))

(defn- side-table-local-xy
  "Default local +X offset to lounger's right; override via :xy on side-table."
  [geom side-table]
  (let [diam (:diameter-m side-table 0.45)
        [w _] (:size-m geom)]
    (or (:xy side-table)
        [(+ (/ w 2.0) 0.08 (/ diam 2.0)) 0.0])))

(defn- add-lounger-group!
  "Lounger seat/back/legs; optional side-table cylinder on local +X."
  [prefix place geom z0 legs side-table]
  (let [lounger-names (add-seat-back! prefix {:place place :geom geom :z0 z0 :legs legs})]
    (if-not side-table
      lounger-names
      (let [diam (:diameter-m side-table 0.45)
            h (:height-m side-table 0.35)
            local (side-table-local-xy geom side-table)
            table-n (str prefix "-side-table")]
        (add-oriented-cylinder! table-n {:radius (/ diam 2.0) :height h
                                         :local local
                                         :xy (:xy place)
                                         :rot-z-deg (:rot-z-deg place)
                                         :z0 z0})
        (conj (vec lounger-names) table-n)))))

(defn- ensure-loungers!
  [s z0 legs]
  (when-let [{:keys [placements side-table] :as lounger} (:furniture/lounger s)]
    (let [geom (dissoc lounger :placements :side-table)
          rot-z (lounger-rot-z-to-face-sun s)
          sun-placements (mapv #(assoc % :rot-z-deg rot-z) placements)]
      (mapcat
       (fn [i placement]
         (add-lounger-group! (str "site-furniture-lounger-" i)
                             placement geom z0 legs
                             (when (:with-side-table? placement) side-table)))
       (range)
       sun-placements))))

(defn- clear-loungers!
  []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-furniture-lounger")
                (mesh/site-object-names))))

(defn orient-loungers-to-sun!
  "Rebuild loungers facing current :sun/azimuth-deg. Returns {:names … :rot-z-deg …}.
  Caller should sync-site-hierarchy! (set-time-of-day! does)."
  [s]
  (clear-loungers!)
  (let [z0 (railing/railing-z-deck s)
        legs {:size (:furniture/leg-size-m s 0.05)
              :inset (:furniture/leg-inset-m s 0.03)}
        names (vec (or (ensure-loungers! s z0 legs) []))
        rot-z (lounger-rot-z-to-face-sun s)]
    {:names names :rot-z-deg rot-z}))

(defn- ensure-bbq!
  [s z0]
  (when-let [bbq (:furniture/bbq s)]
    (add-bbq! "site-furniture-bbq" bbq z0)))

(defn ensure-terrace-furniture!
  "Schematic terrace furniture (tables, chairs, loungers, BBQ). Objects: site-furniture-*.
  After calling, sync under site-root (prefer yardcraft.site/ensure-terrace-furniture-scene!)."
  [s]
  (clear-furniture!)
  (let [z0 (railing/railing-z-deck s)
        legs {:size (:furniture/leg-size-m s 0.05)
              :inset (:furniture/leg-inset-m s 0.03)}
        names (vec (concat (mapcat ensure-table-chairs! (table-chair-configs s z0 legs))
                           (or (ensure-loungers! s z0 legs) [])
                           (or (ensure-bbq! s z0) [])))]
    {:names names}))
