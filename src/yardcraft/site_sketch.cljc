(ns yardcraft.site-sketch
  "Elevation sketch overlay + draft contour curves (light-table tracing workflow).

  Place a fitted overlay (`ensure-sketch!` + a :sketch/specs key), seed/edit `draft-contour-*`
  Bezier curves, then `capture-draft-contour!` into :terrain/contours.
  Requires site-root already in the scene (use yardcraft.site/prepare-contour-draw! or ensure-site!)."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-house :as house]
            [yardcraft.site-lot :as lot]
            [yardcraft.site-data :as data])
  (:import bpy math mathutils [os.path :as path]))

(defn- round-cm [n]
  (/ (python/round (* n 100.0)) 100.0))

(defn- spline-control-points
  "Control points for a curve spline (BEZIER or POLY/NURBS)."
  [spl]
  (case (.-type spl)
    "BEZIER" (.-bezier-points spl)
    (.-points spl)))

(defn- curve-world-xyz
  "World-space points from CURVE object (BEZIER or POLY splines)."
  [obj]
  (let [mw (.-matrix-world obj)]
    (vec
     (mapcat
      (fn [spl]
        (map (fn [pt]
               (let [co (.-co pt)
                     v (mathutils/Vector #py [(double (aget co 0))
                                              (double (aget co 1))
                                              (double (aget co 2))])
                     wv (.__matmul__ mw v)]
                 [(.-x wv) (.-y wv) (.-z wv)]))
             (spline-control-points spl)))
      (.-splines (.-data obj))))))

(defn draft-contour-name [rh00] (str "draft-contour-" (int rh00)))

(defn seed-draft-contour!
  "Create (or replace) a 2D BEZIER curve ready to edit by hand. Name e.g. \"draft-contour-49\".
  Leaves the curve selected in Edit Mode."
  [curve-name]
  (mesh/unlink-and-remove! curve-name)
  (.primitive-bezier-curve-add (.-curve (.-ops bpy)) **
                               :enter_editmode false
                               :location #py [0.0 0.0 0.0])
  (let [obj (.-object (.-context bpy))
        spl (aget (.-splines (.-data obj)) 0)]
    (set! (.-name obj) curve-name)
    (set! (.-name (.-data obj)) curve-name)
    (set! (.-dimensions (.-data obj)) "2D")
    (aset (.-lock-location obj) 2 true)
    (doseq [bp (.-bezier-points spl)]
      (set! (.-handle-left-type bp) "ALIGNED")
      (set! (.-handle-right-type bp) "ALIGNED"))
    (.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
    (.select-set obj true)
    (set! (.-active (.-objects (.-view-layer (.-context bpy)))) obj)
    (.mode-set (.-object (.-ops bpy)) ** :mode "EDIT")
    {:name (.-name obj)
     :spline-type (.-type spl)
     :dimensions (.-dimensions (.-data obj))
     :point-count (python/len (.-bezier-points spl))}))

(defn draft-contour-xy
  "House-NW [[x y] ...] from draft-contour-<rh00>, rounded to cm."
  [s rh00]
  (when-let [obj (mesh/object-by-name (draft-contour-name rh00))]
    (vec (map (fn [[wx wy _z]]
                (let [[x y] (house/world-xy->house-nw s [wx wy])]
                  [(round-cm x) (round-cm y)]))
              (curve-world-xyz obj)))))

(defn- douglas-peucker
  "Simplify polyline pts to tolerance eps (perpendicular distance)."
  [pts eps]
  (if (< (count pts) 3)
    pts
    (let [a (first pts)
          b (peek pts)
          [imax dmax] (reduce (fn [[im dm] i]
                                (let [d (lot/dist-point-segment (nth pts i) a b)]
                                  (if (> d dm) [i d] [im dm])))
                              [0 0.0]
                              (range 1 (dec (count pts))))]
      (if (<= dmax eps)
        [a b]
        (vec (concat (butlast (douglas-peucker (subvec pts 0 (inc imax)) eps))
                     (douglas-peucker (subvec pts imax) eps)))))))

(defn draft-bezier-xy
  "House-NW [[x y] ...] from a draft BEZIER curve with handles honored:
  16 samples per segment, Douglas-Peucker simplified at 3 cm, cm-rounded.
  For XY-geometry traces (e.g. draft-road-inner); draft-contour-xy
  reads control points only."
  [s curve-name]
  (when-let [obj (mesh/object-by-name curve-name)]
    (let [mw (.-matrix-world obj)
          spl (aget (.-splines (.-data obj)) 0)
          bps (vec (.-bezier-points spl))
          ib (.-interpolate-bezier (.-geometry mathutils))
          dense (vec
                 (mapcat (fn [i]
                           (let [a (nth bps i)
                                 b (nth bps (inc i))
                                 seg (ib (.-co a) (.-handle-right a)
                                         (.-handle-left b) (.-co b) 16)
                                 seg (if (zero? i) seg (rest seg))]
                             (map (fn [v]
                                    (let [wv (.__matmul__ mw v)]
                                      (house/world-xy->house-nw s [(.-x wv) (.-y wv)])))
                                  seg)))
                         (range (dec (count bps)))))]
      (mapv (fn [[x y]] [(round-cm x) (round-cm y)])
            (douglas-peucker dense 0.03)))))

(defn- upsert-contour
  [contours rh00 xy]
  (vec (sort-by :rh00 >
                (conj (vec (remove #(= (:rh00 %) rh00) contours))
                      {:rh00 rh00 :xy xy}))))

(defn capture-draft-contour!
  "Read draft-contour-<rh00> into live site :terrain/contours. Does not rebuild meshes."
  [s rh00]
  (let [xy (draft-contour-xy s rh00)]
    (when (empty? xy)
      (throw (python/Exception (str "No points on draft curve: " (draft-contour-name rh00)))))
    (let [contours (upsert-contour (:terrain/contours s) (int rh00) xy)
          _ (data/persist-site! (assoc s :terrain/contours contours))]
      {:rh00 (int rh00) :xy xy :point-count (count xy) :contours-count (count contours)})))

(defn sketch-fit
  "Best-fit similarity (rotation + uniform scale + translation) mapping a sketch
  spec's :corner-px (pixel [u v], v down, in :px-size space) onto
  :lot/polygon-xy corners (same order).
  Returns {:theta-rad :scale-m-per-px :center-local :residuals-m}."
  [s {:keys [corner-px px-size]}]
  (let [ps (mapv (fn [[u v]] [u (- v)]) corner-px)
        qs (lot/lot-polygon-xy s)
        n (count ps)
        mean-xy (fn [xs] [(/ (reduce + (map first xs)) n)
                          (/ (reduce + (map second xs)) n)])
        [pcx pcy] (mean-xy ps)
        [qcx qcy] (mean-xy qs)
        cp (mapv (fn [[x y]] [(- x pcx) (- y pcy)]) ps)
        cq (mapv (fn [[x y]] [(- x qcx) (- y qcy)]) qs)
        a (reduce + (map (fn [[px py] [qx qy]] (+ (* px qx) (* py qy))) cp cq))
        b (reduce + (map (fn [[px py] [qx qy]] (- (* px qy) (* py qx))) cp cq))
        pp (reduce + (map (fn [[px py]] (+ (* px px) (* py py))) cp))
        theta (math/atan2 b a)
        k (/ (math/hypot a b) pp)
        cs (math/cos theta)
        sn (math/sin theta)
        tx (- qcx (* k (- (* cs pcx) (* sn pcy))))
        ty (- qcy (* k (+ (* sn pcx) (* cs pcy))))
        xf (fn [[u v]]
             (let [x u
                   y (- v)]
               [(+ (* k (- (* cs x) (* sn y))) tx)
                (+ (* k (+ (* sn x) (* cs y))) ty)]))
        [pw ph] px-size]
    {:theta-rad theta
     :scale-m-per-px k
     :center-local (xf [(/ pw 2.0) (/ ph 2.0)])
     :residuals-m (mapv (fn [p q]
                          (let [[x y] (xf p)]
                            (round-cm (math/hypot (- x (first q)) (- y (second q))))))
                        corner-px qs)}))

(defn- require-site-root!
  []
  (when-not (mesh/object-by-name "site-root")
    (throw (python/Exception "site-root missing — call yardcraft.site/prepare-contour-draw! or ensure-site! first"))))

(defn- sketch-spec!
  "Lookup :sketch/specs entry or throw."
  [s sketch-key]
  (or (get-in s [:sketch/specs sketch-key])
      (throw (python/Exception (str "No :sketch/specs entry: " sketch-key)))))

(defn- sketch-world-pose
  "World location + Z rotation for a fitted sketch empty."
  [s {:keys [theta-rad center-local]}]
  (let [[cx cy] (house/house-center-xy s)
        root (mesh/object-by-name "site-root")
        rw (.-matrix-world root)
        mpi (.Translation (.-Matrix mathutils)
                          #py [(double (- cx)) (double (- cy)) 0.0])
        lv (mathutils/Vector #py [(double (first center-local))
                                  (double (second center-local))
                                  -0.005])
        wv (.__matmul__ rw (.__matmul__ mpi lv))]
    {:location #py [(.-x wv) (.-y wv) (.-z wv)]
     :rot-z (+ theta-rad (.-z (.-rotation-euler root)))}))

(defn- place-sketch-empty!
  "Create IMAGE empty named object-name with img, pose, and display size."
  [object-name img {:keys [location rot-z]} display-size]
  (let [obj (.new (.-objects (.-data bpy)) object-name nil)]
    (set! (.-empty_display_type obj) "IMAGE")
    (set! (.-data obj) img)
    (set! (.-location obj) location)
    (set! (.-rotation-euler obj) #py [0.0 0.0 (double rot-z)])
    (set! (.-empty_display_size obj) (double display-size))
    (.link (.-objects (.-collection (.-scene (.-context bpy)))) obj)
    obj))

(defn- hide-draw-pad!
  []
  (when-let [pad (mesh/object-by-name "site-draw-pad")]
    (set! (.-hide_viewport pad) true)
    (set! (.-hide_render pad) true)))

(defn ensure-sketch!
  "Place a :sketch/specs entry as an opaque reference image empty (spec :object-name),
  best-fit over the lot via sketch-fit, at z=-0.005 (just above the draw pad).
  Hides site-draw-pad — the sketch itself is the tracing canvas.
  Unparented (world frame); survives clear-site! by the draft- prefix.
  Requires site-root in the scene (orient already applied)."
  [s sketch-key]
  (require-site-root!)
  (let [{:keys [object-name image-path px-size] :as spec} (sketch-spec! s sketch-key)
        fit (sketch-fit s spec)
        img (.load (.-images (.-data bpy)) (path/abspath image-path)
                   ** :check-existing true)
        pose (sketch-world-pose s fit)
        display-size (* (:scale-m-per-px fit) (first px-size))
        obj (do (mesh/unlink-and-remove! object-name)
                (place-sketch-empty! object-name img pose display-size))]
    (hide-draw-pad!)
    {:name (.-name obj)
     :sketch-key sketch-key
     :rot-deg (round-cm (math/degrees (:rot-z pose)))
     :residuals-m (:residuals-m fit)}))

(defn seed-draft-contour-from-xy!
  "Create/replace a 2D BEZIER draft curve through house-NW xy points.
  AUTO handles, z locked, thin bright bevel for visibility over the sketch."
  [s curve-name xy]
  (mesh/unlink-and-remove! curve-name)
  (let [cu (.new (.-curves (.-data bpy)) curve-name "CURVE")
        _ (set! (.-dimensions cu) "2D")
        _ (set! (.-bevel-depth cu) 0.08)
        spl (.new (.-splines cu) "BEZIER")
        pts (.-bezier-points spl)
        _ (.add pts (dec (count xy)))
        mat (mesh/ensure-material! "site-mat-draft-contour"
                                   (:draft-contour mesh/material-colors))]
    (doseq [[i p] (map-indexed vector xy)]
      (let [[wx wy] (house/house-nw->world-xy s p)
            bp (aget pts i)]
        (set! (.-co bp) #py [(double wx) (double wy) 0.0])
        (set! (.-handle-left-type bp) "AUTO")
        (set! (.-handle-right-type bp) "AUTO")))
    (.append (.-materials cu) mat)
    (let [obj (.new (.-objects (.-data bpy)) curve-name cu)]
      (.link (.-objects (.-collection (.-scene (.-context bpy)))) obj)
      (aset (.-lock-location obj) 2 true)
      {:name (.-name obj) :points (count xy)})))

(defn seed-draft-contours-from-site!
  "Seed draft-contour-<rh00> bezier curves from every entry in :terrain/contours."
  [s]
  (mapv (fn [{:keys [rh00 xy]}]
          (seed-draft-contour-from-xy! s (draft-contour-name rh00) xy))
        (:terrain/contours s)))

(defn- set-drafts-hidden!
  "Set hide_viewport/hide_render on all draft-* objects to hidden?."
  [hidden?]
  (let [names (filterv #(string/starts-with? % "draft-")
                       (mesh/all-object-names))
        touched (atom [])]
    (doseq [n names]
      (when-let [obj (mesh/object-by-name n)]
        (set! (.-hide_viewport obj) hidden?)
        (set! (.-hide_render obj) hidden?)
        (swap! touched conj n)))
    @touched))

(defn hide-drafts!
  "Hide all draft-* objects (sketch overlay + draft curves)."
  []
  {:hidden (set-drafts-hidden! true)})

(defn show-drafts!
  "Unhide all draft-* objects."
  []
  {:shown (set-drafts-hidden! false)})
