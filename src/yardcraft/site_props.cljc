(ns yardcraft.site-props
  "Prop placement — glTF cars, trees, mailbox seated on driveway/terrain.

  Domain helpers take facts map `s` plus terrain `extras` from orchestration.
  Hierarchy parenting is left to ensure-site! (organize once)."
  (:require [yardcraft.site-mesh :as mesh]
            [yardcraft.site-lot :as lot]
            [yardcraft.site-driveway :as driveway]))

(defn terrain-surface-z
  "Blended terrain Z at house-NW [x y] (plateau + contours + extras)."
  [s extras xy]
  (lot/features-z s (into (lot/terrain-features s) extras) xy))

(defn- driveway-clearance
  [s seat-driveway?]
  (if seat-driveway?
    (:driveway/car-clearance-m s 0.04)
    0.0))

(defn- resolve-place-z
  "Z for glTF placement: explicit location Z wins, else terrain/driveway seating."
  [s extras {:keys [location seat-driveway? seat-terrain?]}]
  (let [lz (nth location 2 nil)
        [lx ly] location
        clearance (driveway-clearance s seat-driveway?)]
    (cond
      (some? lz) lz
      seat-terrain? (terrain-surface-z s extras [lx ly])
      seat-driveway? (+ (driveway/driveway-surface-z s [lx ly]) clearance)
      :else 0.0)))

(defn- gltf-place-opts
  [opts lx ly z]
  (-> opts
      (dissoc :name :file :seat-driveway :seat-terrain)
      (assoc :location [lx ly z]
             :rot-x-deg 0.0)))

(defn- apply-driveway-pitch!
  "Pitch car to driveway slope and ground wheels at downhill Z + clearance."
  [{:keys [s n lx ly clearance length-m placed]}]
  (when-let [obj (mesh/object-by-name n)]
    (let [pitch-len (double (or length-m (second (:dims placed))))
          {:keys [pitch-x-rad z-downhill]} (driveway/driveway-seating s [lx ly] pitch-len)
          e (.-rotation-euler obj)]
      (set! (.-rotation-euler obj) #py [(double pitch-x-rad) (aget e 1) (aget e 2)])
      (mesh/ground-at-z! obj lx ly (+ z-downhill clearance)))))

(defn- seat-on-driveway!
  [{:keys [seat-driveway?] :as ctx}]
  (when seat-driveway?
    (apply-driveway-pitch! ctx)))

(defn- ground-on-terrain!
  "Re-ground object bottom on blended terrain Z at [lx ly]."
  [{:keys [s extras n lx ly]}]
  (when-let [obj (mesh/object-by-name n)]
    (mesh/ground-at-z! obj lx ly (terrain-surface-z s extras [lx ly]))))

(defn- seat-on-terrain!
  [{:keys [seat-terrain?] :as ctx}]
  (when seat-terrain?
    (ground-on-terrain! ctx)))

(defn- placed-snapshot
  [n placed]
  (let [obj (mesh/object-by-name n)]
    (assoc placed
           :dims (mesh/object-dimensions obj)
           :location (vec (.-location obj)))))

(defn place-site-gltf!
  "Import glTF into house-NW frame. Returns {:skipped ...} when file missing or location nil — never crashes.

  `place` map: :name :file plus mesh/place-gltf! opts
  (:location :height-m :width-m :length-m :scale :rot-z-deg :rot-x-deg).
  :location may be [x y] or [x y z] (house-NW).
  :seat-driveway — Z + pitch follow the driveway (cars); downhill clearance from :driveway/car-clearance-m.
  :seat-terrain — Z from blended terrain (trees); no pitch.
  Explicit Z in :location wins over seating flags.
  clear-site! / ensure-site! remove site-* props. Parenting via ensure-site! hierarchy."
  [s extras {:keys [name file location length-m seat-driveway seat-terrain] :as place}]
  (cond
    (or (nil? file) (nil? location))
    {:name name :skipped :missing-facts :file file :location location}

    :else
    (try
      (let [[lx ly] location
            seat-driveway? (boolean seat-driveway)
            seat-terrain? (boolean seat-terrain)
            clearance (driveway-clearance s seat-driveway?)
            z (resolve-place-z s extras {:location location
                                         :seat-driveway? seat-driveway?
                                         :seat-terrain? seat-terrain?})
            placed (mesh/place-gltf! name file (gltf-place-opts place lx ly z))]
        (if (:skipped placed)
          placed
          (let [ctx {:s s :extras extras :n name :lx lx :ly ly
                     :clearance clearance :length-m length-m :placed placed
                     :seat-driveway? seat-driveway? :seat-terrain? seat-terrain?}]
            (seat-on-driveway! ctx)
            (seat-on-terrain! ctx)
            (placed-snapshot name placed))))
      (catch python/Exception e
        {:name name :skipped :error :file file :error (str e)}))))

(defn- tint-gltf-materials!
  "Set Principled Base Color on materials whose name starts with prefix."
  [obj-name prefix [r g b]]
  (when-let [obj (mesh/object-by-name obj-name)]
    (let [rgba #py [(double r) (double g) (double b) 1.0]]
      (doseq [slot (.-material-slots obj)
              :let [mat (.-material slot)]
              :when (and mat (.startswith (.-name mat) prefix))]
        (set! (.-diffuse-color mat) rgba)
        (when-let [bsdf (.get (.-nodes (.-node-tree mat)) "Principled BSDF")]
          (set! (.-default-value (.get (.-inputs bsdf) "Base Color")) rgba))))
    obj-name))

(defn- place-car!
  [s {:keys [name file xy width-m length-m body-mat-prefix body-rgb skip-name-re
             mesh-rot-x-deg]}]
  (let [placed (place-site-gltf! s [] (cond-> {:name name
                                               :file file
                                               :location xy
                                               :width-m width-m
                                               :length-m length-m
                                               :seat-driveway true}
                                        skip-name-re (assoc :skip-name-re skip-name-re)
                                        mesh-rot-x-deg (assoc :mesh-rot-x-deg mesh-rot-x-deg)))]
    (when (and (not (:skipped placed)) body-mat-prefix body-rgb)
      (tint-gltf-materials! name body-mat-prefix body-rgb))
    placed))

(defn- clear-cars!
  []
  (doseq [n (filterv #(.startswith % "site-car-") (mesh/site-object-names))]
    (mesh/unlink-hierarchy! n)))

(defn place-cars!
  "EXAMPLE only — optional driveway car props for fit-checking.
  Many sites have no car/driveway use case; leave :cars/placements empty (default).
  Reads :cars/placements from facts (seq of maps with :name :file :xy …).
  Skips missing GLBs; never crashes. See assets/cars/ATTRIBUTION.md + yardcraft-assets skill."
  [s]
  (clear-cars!)
  (mapv #(place-car! s %) (or (:cars/placements s) [])))

(defn- place-tree!
  [s extras {:keys [name file xy height-m rot-z-deg]}]
  (place-site-gltf! s extras {:name name
                              :file file
                              :location xy
                              :height-m height-m
                              :rot-z-deg (or rot-z-deg 0.0)
                              :seat-terrain true}))

(defn- clear-trees!
  []
  (doseq [n (filterv #(.startswith % "site-tree-") (mesh/site-object-names))]
    (mesh/unlink-hierarchy! n)))

(defn place-trees!
  "EXAMPLE/optional — fruit trees from :trees/plantings (see assets/trees/ATTRIBUTION.md).
  Empty plantings = no trees. Skips missing GLBs; never crashes.
  Clears existing site-tree-* then seats on blended terrain.
  Prefer yardcraft.site/ensure-trees-scene! when iterating (syncs under site-root)."
  [s extras]
  (clear-trees!)
  (mapv #(place-tree! s extras %) (or (:trees/plantings s) [])))

(defn place-mailbox!
  "EXAMPLE/optional — mailbox at :mailbox/xy (see assets/props/ATTRIBUTION.md).
  Skips when :mailbox/file or :mailbox/xy nil, or GLB missing; never crashes.
  Seated on terrain. Called from ensure-site!; clear-site! removes it."
  [s extras]
  (place-site-gltf! s extras {:name "site-mailbox"
                              :file (:mailbox/file s)
                              :location (:mailbox/xy s)
                              :height-m (:mailbox/height-m s)
                              :rot-z-deg (or (:mailbox/rot-z-deg s) 0.0)
                              :seat-terrain true}))
