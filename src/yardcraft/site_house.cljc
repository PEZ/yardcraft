(ns yardcraft.site-house
  "House, bod, bay, and exterior doors — seldom-touched massing."
  (:require [yardcraft.site-mesh :as mesh])
  (:import math))

(defn house-center-xy
  "House XY center in house-NW frame: NW at 0,0; +X east; +Y toward road; south negative Y.
  Returns [0.0 0.0] when :house/size-m is missing (empty-site path)."
  [{:house/keys [size-m]}]
  (if-not size-m
    [0.0 0.0]
    (let [[hx hy] size-m]
      [(/ hx 2.0) (- (/ hy 2.0))])))

(defn- house-nw-center+angle
  [s]
  (let [[cx cy] (house-center-xy s)
        a (math/radians (:site/north-offset-deg s))]
    {:cx cx :cy cy :cos-a (math/cos a) :sin-a (math/sin a)}))

(defn world-xy->house-nw
  "World XY → house-NW frame (site-root oriented)."
  [s [wx wy]]
  (let [{:keys [cx cy cos-a sin-a]} (house-nw-center+angle s)]
    [(+ (- (* cos-a wx) (* sin-a wy)) cx)
     (+ (* sin-a wx) (* cos-a wy) cy)]))

(defn house-nw->world-xy
  "House-NW frame XY → world XY (inverse of world-xy->house-nw)."
  [s [lx ly]]
  (let [{:keys [cx cy cos-a sin-a]} (house-nw-center+angle s)
        dx (- lx cx)
        dy (- ly cy)]
    [(+ (* cos-a dx) (* sin-a dy))
     (+ (- (* sin-a dx)) (* cos-a dy))]))

(defn- size-center-footprint
  "Axis-aligned footprint map from size [sx sy] and center [cx cy]."
  [[sx sy] [cx cy]]
  {:min-x (- cx (/ sx 2.0))
   :max-x (+ cx (/ sx 2.0))
   :min-y (- cy (/ sy 2.0))
   :max-y (+ cy (/ sy 2.0))
   :cx cx
   :cy cy
   :sx sx
   :sy sy})

(defn- door-facade-footprint
  "Door footprint on N/S façade; :outward -1 = south, +1 = north."
  [fp {:keys [west-offset-m]} {:keys [width thickness wall-y outward]}]
  (let [min-x (+ (:min-x fp) west-offset-m)
        max-x (+ min-x width)
        [min-y max-y] (if (neg? outward)
                        [(- wall-y thickness) wall-y]
                        [wall-y (+ wall-y thickness)])]
    {:min-x min-x :max-x max-x :min-y min-y :max-y max-y
     :sx width :sy thickness}))

(defn house-footprint
  "Wall footprint map: :min-x :max-x :min-y :max-y :cx :cy :sx :sy."
  [s]
  (size-center-footprint (:house/size-m s) (house-center-xy s)))

(defn bod-center-xy
  "Bod west of house by gap; south edge overhangs house south by :bod/south-overhang-m."
  [s]
  (let [fp (house-footprint s)
        [bx by] (:bod/size-m s)
        gap (:bod/gap-to-house-m s)
        south-over (:bod/south-overhang-m s)
        max-x (- (:min-x fp) gap)
        min-x (- max-x bx)
        min-y (- (:min-y fp) south-over)
        max-y (+ min-y by)]
    (mesh/rect-center-xy min-x min-y max-x max-y)))

(defn bod-footprint
  "Bod wall footprint map: :min-x :max-x :min-y :max-y :cx :cy :sx :sy."
  [s]
  (size-center-footprint (:bod/size-m s) (bod-center-xy s)))

(defn bod-footprint-xy
  "Bod footprint ring NW NE SE SW (house-NW frame)."
  [s]
  (let [[bx by] (:bod/size-m s)
        [cx cy] (bod-center-xy s)
        hx (/ bx 2.0)
        hy (/ by 2.0)]
    [[(- cx hx) (+ cy hy)]
     [(+ cx hx) (+ cy hy)]
     [(+ cx hx) (- cy hy)]
     [(- cx hx) (- cy hy)]]))

(defn door-west-footprint
  "West laundry door on west façade; north jamb from :door/west :north-jamb-from-south-m."
  [s]
  (let [fp (house-footprint s)
        w (:door/width-m s)
        t (:door/thickness-m s)
        jamb (get-in s [:door/west :north-jamb-from-south-m])
        max-y (+ (:min-y fp) jamb)
        min-y (- max-y w)
        min-x (- (:min-x fp) t)
        max-x (:min-x fp)]
    {:min-x min-x :max-x max-x :min-y min-y :max-y max-y
     :sx t :sy w}))

(defn door-south-footprint
  "South hall door; west edge offset from house west."
  [s]
  (let [fp (house-footprint s)]
    (door-facade-footprint fp (:door/south s)
                           {:width (:door/width-m s)
                            :thickness (:door/thickness-m s)
                            :wall-y (:min-y fp)
                            :outward -1})))

(defn door-north-footprint
  "North entrance door within the veranda; west edge offset from house west."
  [s]
  (let [fp (house-footprint s)]
    (door-facade-footprint fp (:door/north s)
                           {:width (:door/width-m s)
                            :thickness (:door/thickness-m s)
                            :wall-y (:max-y fp)
                            :outward 1})))

(defn ensure-house-ground!
  "White pad under house footprint, z 0 → pad-height. Object: site-house-ground."
  [s]
  (let [[hx hy] (:house/size-m s)
        hz (:house/pad-height-m s)
        size [hx hy hz]
        center (mesh/box-center size (house-center-xy s) 0.0)]
    {:name (.-name (mesh/add-box! "site-house-ground" size center))
     :size size}))

(defn ensure-house!
  "Schematic house massing. Object: site-house."
  [s]
  (let [[hx hy] (:house/size-m s)
        hz (:house/schematic-height-m s)
        floor-z (:house/floor-z s)
        size [hx hy hz]
        center (mesh/box-center size (house-center-xy s) floor-z)]
    {:name (.-name (mesh/add-box! "site-house" size center))
     :size size
     :fsh-rh00 (:house/fsh-rh00 s)}))

(defn ensure-house-roof!
  "Flat roof with overhangs (long=N/S, short=E/W). Object: site-house-roof."
  [s]
  (let [[hx hy] (:house/size-m s)
        hz (:house/schematic-height-m s)
        floor-z (:house/floor-z s)
        {:keys [long short]} (:house/roof-overhang-m s)
        size [(+ hx (* 2.0 short)) (+ hy (* 2.0 long)) 0.12]
        z0 (+ floor-z hz)
        center (mesh/box-center size (house-center-xy s) z0)]
    {:name (.-name (mesh/add-box! "site-house-roof" size center))
     :size size}))

(defn bod-floor-z
  "Bod floor = terrace deck (house floor + terrace slab)."
  [s]
  (+ (:house/floor-z s) (:terrace/slab-thickness-m s)))

(defn- ensure-box-at-xy!
  "Create box n with size at xy center, bottom at z0."
  [n size xy z0]
  {:name (.-name (mesh/add-box! n size (mesh/box-center size xy z0)))
   :size size})

(defn ensure-bod!
  "Bod massing west of house. Object: site-bod."
  [s]
  (let [[bx by] (:bod/size-m s)
        size [bx by (:bod/height-m s)]]
    (ensure-box-at-xy! "site-bod" size (bod-center-xy s) (bod-floor-z s))))

(defn ensure-bod-roof!
  "Thin flat roof on bod. Object: site-bod-roof."
  [s]
  (let [[bx by] (:bod/size-m s)
        bz (:bod/height-m s)
        floor-z (bod-floor-z s)
        size [(+ bx 0.3) (+ by 0.3) 0.08]]
    (ensure-box-at-xy! "site-bod-roof" size (bod-center-xy s) (+ floor-z bz))))

(defn ensure-doors!
  "Schematic exterior doors west (laundry), south (hall), north (entré)."
  [s]
  (let [hz (:door/height-m s)
        floor-z (:house/floor-z s)
        door-box! (fn [n fp]
                    (let [size [(:sx fp) (:sy fp) hz]
                          xy (mesh/rect-center-xy (:min-x fp) (:min-y fp)
                                                  (:max-x fp) (:max-y fp))]
                      {:name (.-name (mesh/add-box! n size (mesh/box-center size xy floor-z)))
                       :size size
                       :footprint fp}))]
    {:west (door-box! "site-door-west" (door-west-footprint s))
     :south (door-box! "site-door-south" (door-south-footprint s))
     :north (door-box! "site-door-north" (door-north-footprint s))}))

(defn ensure-bay-window!
  "Bay on south façade: trapezoid plan. Object: site-bay-window."
  [s]
  (let [fp (house-footprint s)
        w (:bay/width-m s)
        cw (:bay/center-width-m s)
        d (:bay/depth-m s)
        h (:bay/height-m s)
        floor-z (:house/floor-z s)
        max-x (- (:max-x fp) (:bay/east-offset-m s))
        min-x (- max-x w)
        attach-y (:min-y fp)
        outer-y (- attach-y d)
        cx (/ (+ min-x max-x) 2.0)
        outer-half (/ cw 2.0)
        bottom [[min-x attach-y]
                [max-x attach-y]
                [(+ cx outer-half) outer-y]
                [(- cx outer-half) outer-y]]
        obj (mesh/add-trap-prism! "site-bay-window" bottom h)]
    (set! (.-location obj) #py [0.0 0.0 (double floor-z)])
    {:name (.-name obj)
     :attach-width-m w
     :outer-width-m cw
     :depth-m d
     :height-m h}))

(defn- veranda-layout
  "Derived geometry for entrance veranda from site facts."
  [s]
  (let [{:veranda/keys [size-m center-west-offset-m balcony-height-m
                        parapet-height-m parapet-thickness-m post-size-m stair]} s
        fp (house-footprint s)
        floor-z (:house/floor-z s)
        slab (:terrace/slab-thickness-m s)
        [w d] size-m
        cx (+ (:min-x fp) center-west-offset-m)
        x0 (- cx (/ w 2.0))
        x1 (+ cx (/ w 2.0))
        y0 (:max-y fp)
        y1 (+ y0 d)
        pt parapet-thickness-m
        balcony-floor (+ floor-z balcony-height-m)
        {stair-w :width-m steps :steps tread :tread-m} stair]
    {:w w :d d :cx cx :x0 x0 :x1 x1 :y0 y0 :y1 y1
     :pt pt :ph parapet-height-m :post post-size-m
     :floor-z floor-z :slab slab :balcony-floor balcony-floor
     :post-h (+ (- balcony-floor slab) 0.1)
     :side-len (- d pt)
     :side-cy (+ y0 (/ (- d pt) 2.0))
     :rail-cy (- y1 (/ pt 2.0))
     :stair-w stair-w :steps steps :tread tread
     :rise (/ floor-z steps)
     :seg (/ (- w stair-w) 2.0)}))

(defn- veranda-box!
  "Add a named box; returns the object name."
  [n size xy z0]
  (mesh/add-box! n size (mesh/box-center size xy z0))
  n)

(defn- veranda-structure-names
  "Deck, balcony, posts, and parapet object names for layout."
  [{:keys [w d cx x0 x1 y0 y1 pt ph post floor-z slab balcony-floor
           post-h side-len side-cy rail-cy seg]}]
  [(veranda-box! "site-veranda-deck" [w d slab]
                 [cx (+ y0 (/ d 2.0))] (- floor-z slab))
   (veranda-box! "site-veranda-balcony" [w d slab]
                 [cx (+ y0 (/ d 2.0))] (- balcony-floor slab))
   (veranda-box! "site-veranda-post-w" [post post post-h]
                 [(+ x0 (/ post 2.0)) (- y1 (/ post 2.0))] -0.1)
   (veranda-box! "site-veranda-post-e" [post post post-h]
                 [(- x1 (/ post 2.0)) (- y1 (/ post 2.0))] -0.1)
   (veranda-box! "site-veranda-parapet-balcony-n" [w pt ph]
                 [cx rail-cy] balcony-floor)
   (veranda-box! "site-veranda-parapet-balcony-w" [pt side-len ph]
                 [(+ x0 (/ pt 2.0)) side-cy] balcony-floor)
   (veranda-box! "site-veranda-parapet-balcony-e" [pt side-len ph]
                 [(- x1 (/ pt 2.0)) side-cy] balcony-floor)
   (veranda-box! "site-veranda-parapet-deck-w" [pt side-len ph]
                 [(+ x0 (/ pt 2.0)) side-cy] floor-z)
   (veranda-box! "site-veranda-parapet-deck-e" [pt side-len ph]
                 [(- x1 (/ pt 2.0)) side-cy] floor-z)
   (veranda-box! "site-veranda-parapet-deck-nw" [seg pt ph]
                 [(+ x0 (/ seg 2.0)) rail-cy] floor-z)
   (veranda-box! "site-veranda-parapet-deck-ne" [seg pt ph]
                 [(- x1 (/ seg 2.0)) rail-cy] floor-z)])

(defn- veranda-step-names
  "Stair step object names descending from deck to platform."
  [{:keys [cx y1 floor-z stair-w steps tread rise]}]
  (mapv (fn [k]
          (let [top (- floor-z (* (inc k) rise))]
            (veranda-box! (str "site-veranda-step-" (inc k))
                          [stair-w tread (+ top 0.05)]
                          [cx (+ y1 (* k tread) (/ tread 2.0))]
                          -0.05)))
        (range (dec steps))))

(defn ensure-veranda!
  "Entrance veranda + balcony above, road-facing north wall (H-01 blueprint).
   Deck at house floor; balcony floor :veranda/balcony-height-m above it;
   railings schematized as flat parapet panels; steps down to the platform,
   centered on the entrance. Objects: site-veranda-*."
  [s]
  (let [layout (veranda-layout s)
        {:keys [floor-z balcony-floor x0 x1 y0 y1]} layout
        names (into (veranda-structure-names layout)
                    (veranda-step-names layout))]
    {:names names
     :deck-top floor-z
     :balcony-floor balcony-floor
     :x-span [x0 x1]
     :y-span [y0 y1]}))
