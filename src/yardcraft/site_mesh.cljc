(ns yardcraft.site-mesh
  "Shared Blender mesh helpers for site-* objects."
  (:require [basilisp.string :as string])
  (:import bpy math mathutils os
           [os.path :as path]
           [operator :as op]))

(defn object-by-name
  "Returns the Blender object named n, or nil."
  [n]
  (.get bpy.data/objects n))

(defn unlink-and-remove!
  "Removes object n from the scene if it exists. Returns n or nil."
  [n]
  (when-let [obj (object-by-name n)]
    (.remove bpy.data/objects obj ** :do-unlink true)
    n))

(defn all-object-names
  "Names of every object currently in bpy.data/objects."
  []
  (->> bpy.data/objects
       (map #(.-name %))
       vec))

(defn site-object-names
  "Names of Blender objects owned by this site model (`site-` prefix)."
  []
  (->> bpy.data/objects
       (map #(.-name %))
       (filter #(string/starts-with? % "site-"))
       vec))

(defn add-box!
  "Creates (or replaces) a box named n with size [sx sy sz] centered at [lx ly lz]."
  [n [sx sy sz] [lx ly lz]]
  (unlink-and-remove! n)
  (.primitive-cube-add bpy.ops/mesh ** :size 1 :location #py [0.0 0.0 0.0])
  (let [obj bpy.context/object]
    (set! (.-name obj) n)
    (set! (.-scale obj) #py [(double sx) (double sy) (double sz)])
    (.transform-apply bpy.ops/object ** :location false :rotation false :scale true)
    (set! (.-location obj) #py [(double lx) (double ly) (double lz)])
    obj))

(defn add-cylinder!
  "Creates (or replaces) a vertical cylinder named n: radius, height (Z), center [lx ly lz]."
  [n radius height [lx ly lz]]
  (unlink-and-remove! n)
  (.primitive-cylinder-add bpy.ops/mesh ** :radius (double radius)
                           :depth (double height)
                           :location #py [0.0 0.0 0.0]
                           :vertices 24)
  (let [obj bpy.context/object]
    (set! (.-name obj) n)
    (.transform-apply bpy.ops/object ** :location false :rotation false :scale true)
    (set! (.-location obj) #py [(double lx) (double ly) (double lz)])
    obj))

(defn add-trap-prism!
  "Creates (or replaces) a vertical prism named n from bottom XY ring [[x y] ...] and height top-z."
  [n bottom-xy top-z]
  (unlink-and-remove! n)
  (let [verts (vec (concat
                    (map (fn [[x y]] #py [(double x) (double y) 0.0]) bottom-xy)
                    (map (fn [[x y]] #py [(double x) (double y) (double top-z)]) bottom-xy)))
        faces #py [#py [0 1 2 3]
                   #py [4 5 6 7]
                   #py [0 1 5 4]
                   #py [1 2 6 5]
                   #py [2 3 7 6]
                   #py [3 0 4 7]]
        mesh (.new bpy.data/meshes n)
        coll bpy.context/collection
        _ (.from-pydata mesh (python/list verts) #py [] faces)
        _ (.update mesh)
        obj (.new bpy.data/objects n mesh)]
    (.link (.-objects coll) obj)
    obj))

(defn add-polygon-slab!
  "Creates (or replaces) a horizontal slab named n from XY ring [[x y] ...] at bottom z with thickness."
  [n xy-ring z thickness]
  (unlink-and-remove! n)
  (let [n-v (count xy-ring)
        z0 (double z)
        z1 (double (+ z thickness))
        bottom (mapv (fn [[x y]] #py [(double x) (double y) z0]) xy-ring)
        top (mapv (fn [[x y]] #py [(double x) (double y) z1]) xy-ring)
        verts (vec (concat bottom top))
        bottom-face (vec (range n-v))
        top-face (vec (range (dec (* 2 n-v)) (dec n-v) -1))
        sides (mapv (fn [i]
                      (let [j (mod (inc i) n-v)]
                        [i j (+ j n-v) (+ i n-v)]))
                    (range n-v))
        faces (python/list (map #(python/list %) (concat [bottom-face top-face] sides)))
        mesh-data (.new bpy.data/meshes n)
        _ (.from-pydata mesh-data (python/list verts) #py [] faces)
        _ (.update mesh-data)
        obj (.new bpy.data/objects n mesh-data)
        coll (.-collection (.-context bpy))]
    (.link (.-objects coll) obj)
    obj))

(defn add-polyline-mesh!
  "Creates (or replaces) an edge-only mesh named n from XY ring at constant z."
  [n xy-2d z]
  (unlink-and-remove! n)
  (let [verts (mapv (fn [[x y]] #py [(double x) (double y) (double z)]) xy-2d)
        edges (python/list (map (fn [i] #py [i (inc i)]) (range (dec (count verts)))))
        mesh-data (.new bpy.data/meshes n)
        _ (.from-pydata mesh-data (python/list verts) edges #py [])
        _ (.update mesh-data)
        obj (.new bpy.data/objects n mesh-data)
        coll (.-collection (.-context bpy))]
    (.link (.-objects coll) obj)
    obj))

(defn add-mesh!
  "Creates (or replaces) mesh n from verts [[x y z] ...] and faces [[i j k] or [i j k l] ...]."
  [n verts faces]
  (unlink-and-remove! n)
  (let [py-verts (python/list (map (fn [[x y z]] #py [(double x) (double y) (double z)]) verts))
        py-faces (python/list (map #(python/list %) faces))
        mesh-data (.new bpy.data/meshes n)
        _ (.from-pydata mesh-data py-verts #py [] py-faces)
        _ (.update mesh-data)
        obj (.new bpy.data/objects n mesh-data)
        coll (.-collection (.-context bpy))]
    (.link (.-objects coll) obj)
    obj))

(defn add-sloped-polygon-surface!
  "Creates (or replaces) a single-face surface named n from XY ring; z-fn maps [x y] → Z."
  [n xy-ring z-fn]
  (let [verts (mapv (fn [[x y]]
                      [x y (double (z-fn [x y]))])
                    xy-ring)
        faces [(vec (range (count verts)))]]
    (add-mesh! n verts faces)))

(defn box-center
  "World center for a box with size [sx sy sz], XY center [cx cy], bottom at z0."
  [[_sx _sy sz] [cx cy] z0]
  [cx cy (+ z0 (/ sz 2.0))])

(defn rect-center-xy
  "Center [cx cy] of axis-aligned rect from min/max corners."
  [min-x min-y max-x max-y]
  [(/ (+ min-x max-x) 2.0)
   (/ (+ min-y max-y) 2.0)])

(def material-colors
  "Simple viewport paints for judging massing."
  {:garden [0.25 0.55 0.22]
   :house [0.55 0.55 0.55]
   :house-pad [0.95 0.95 0.95]
   :roof [0.35 0.35 0.38]
   :bod [0.45 0.42 0.38]
   :bay [0.62 0.62 0.65]
   :terrace [0.72 0.55 0.28]
   :terrace-roof [0.55 0.45 0.35]
   :driveway [0.08 0.08 0.09]
   :driveway-wall [0.62 0.62 0.58]
   :road [0.20 0.20 0.21]
   :stairs [0.78 0.60 0.32]
   :support [0.42 0.36 0.28]
   :railing [0.55 0.38 0.22]
   :furniture [0.48 0.40 0.30]
   :door [0.25 0.35 0.55]
   :veranda [0.88 0.88 0.86]
   :north [0.85 0.15 0.12]
   :sundial [0.72 0.58 0.28]
   :sundial-mark [0.25 0.2 0.12]
   :sundial-noon [0.75 0.15 0.12]
   :contour [0.85 0.55 0.15]
   :draft-contour [0.95 0.35 0.05]
   :draw-pad [0.32 0.48 0.28]
   :terrain [0.28 0.50 0.24]})

(defn ensure-material!
  "Get or create material with RGB for viewport + BSDF. Optional :alpha for transparency."
  [name [r g b] & {:keys [roughness alpha] :or {roughness 0.7 alpha 1.0}}]
  (let [mat (or (.get bpy.data/materials name)
                (.new bpy.data/materials ** :name name))
        a (double alpha)
        rgba #py [(double r) (double g) (double b) a]]
    (set! (.-use-nodes mat) true)
    (set! (.-diffuse-color mat) rgba)
    (let [bsdf (aget (.. mat -node-tree -nodes) "Principled BSDF")]
      (set! (-> bsdf .-inputs (aget "Base Color") .-default-value)
            #py [(double r) (double g) (double b) 1.0])
      (set! (-> bsdf .-inputs (aget "Alpha") .-default-value) a)
      (set! (-> bsdf .-inputs (aget "Roughness") .-default-value)
            (double roughness)))
    (if (< a 1.0)
      (set! (.-blend_method mat) "BLEND")
      (set! (.-blend_method mat) "OPAQUE"))
    mat))

(defn assign-material!
  "Assign mat to object obj-name (slot 0). Returns obj-name or nil."
  [obj-name mat]
  (when-let [obj (object-by-name obj-name)]
    (let [mats (.-materials (.-data obj))]
      (if (zero? (python/len mats))
        (.append mats mat)
        (aset mats 0 mat))
      obj-name)))

(defn unlink-hierarchy!
  "Removes object n and its children recursively. Returns n or nil."
  [n]
  (when-let [obj (object-by-name n)]
    (doseq [c (vec (.-children obj))]
      (unlink-hierarchy! (.-name c)))
    (.remove bpy.data/objects obj ** :do-unlink true)
    n))

(defn ensure-empty!
  "Hidden empty at world origin named n (create or reuse). Returns the object."
  [n]
  (if-let [obj (object-by-name n)]
    obj
    (do (.empty-add (.-object (.-ops bpy)) ** :type "PLAIN_AXES"
                    :location #py [0.0 0.0 0.0])
        (let [obj (.-object (.-context bpy))]
          (set! (.-name obj) n)
          (set! (.-hide-viewport obj) true)
          (set! (.-hide-render obj) true)
          obj))))

(defn parent-identity!
  "Parent child-name under parent-name with identity matrix-parent-inverse.
  Keeps child local transform (house-NW coords) when parent empty is at origin."
  [child-name parent-name]
  (when-let [c (object-by-name child-name)]
    (when-let [p (object-by-name parent-name)]
      (set! (.-parent c) p)
      (set! (.-matrix-parent-inverse c) (.Identity (.-Matrix mathutils) 4))
      child-name)))

(defn object-dimensions
  "Returns [dx dy dz] from obj.dimensions."
  [obj]
  (let [d (.-dimensions obj)]
    [(aget d 0) (aget d 1) (aget d 2)]))

(defn scale-uniform!
  "Multiply obj scale by factor on all axes. Returns obj."
  [obj factor]
  (let [s (.-scale obj)
        f (double factor)]
    (set! (.-scale obj) #py [(* (aget s 0) f)
                             (* (aget s 1) f)
                             (* (aget s 2) f)])
    obj))

(defn scale-to-xy-length!
  "Uniform-scale obj so max(dx,dy) equals length-m. Returns obj."
  [obj length-m]
  (let [[dx dy] (object-dimensions obj)
        xy (max dx dy)]
    (when (pos? xy)
      (scale-uniform! obj (/ (double length-m) xy)))
    obj))

(defn scale-to-xy-width!
  "Uniform-scale obj so min(dx,dy) equals width-m (car track width). Returns obj."
  [obj width-m]
  (let [[dx dy] (object-dimensions obj)
        w (min dx dy)]
    (when (pos? w)
      (scale-uniform! obj (/ (double width-m) w)))
    obj))

(defn scale-to-height!
  "Uniform-scale obj so vertical extent (dz) equals height-m. Returns obj."
  [obj height-m]
  (let [[_ _ dz] (object-dimensions obj)]
    (when (pos? dz)
      (scale-uniform! obj (/ (double height-m) dz)))
    obj))

(defn- world-bound-z-min
  "World-space minimum Z of obj local bound-box corners."
  [obj]
  (let [mw (.-matrix-world obj)
        bb (.-bound-box obj)]
    (apply min
           (map (fn [i]
                  (let [c (aget bb i)
                        v (mathutils/Vector #py [(double (aget c 0))
                                                 (double (aget c 1))
                                                 (double (aget c 2))])]
                    (.-z (op/matmul mw v))))
                (range 8)))))

(defn ground-at-z!
  "Set location so world bound-box bottom sits at z; keep x/y. Honors rotation/scale.
   Forces a depsgraph update so matrix-world matches current parent/rotation."
  [obj x y z]
  (set! (.-location obj) #py [(double x) (double y) 0.0])
  (.update-tag obj)
  (.update (.-view-layer (.-context bpy)))
  (let [zmin (world-bound-z-min obj)]
    (set! (.-location obj) #py [(double x) (double y) (double (- z zmin))])
    (.update-tag obj)
    (.update (.-view-layer (.-context bpy)))
    obj))

(defn parent-with-pivot!
  "Parent obj under parent-name with Translation(-cx,-cy,0) matrix-parent-inverse."
  [obj parent-name [cx cy]]
  (when-let [p (object-by-name parent-name)]
    (set! (.-parent obj) p)
    (let [mpi (.Translation (.-Matrix mathutils)
                            #py [(double (- cx)) (double (- cy)) 0.0])]
      (set! (.-matrix-parent-inverse obj) (.copy mpi))))
  obj)

(defn- select-meshes-for-join!
  "Select meshes, clear parents keeping world transforms, set densest mesh active."
  [meshes]
  (.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
  (doseq [m meshes]
    (.select-set m true))
  (let [active (->> meshes
                    (sort-by #(count (.-vertices (.-data %))))
                    last)]
    (set! (.-active (.-objects (.-view-layer (.-context bpy)))) active)
    (.parent-clear (.-object (.-ops bpy)) ** :type "CLEAR_KEEP_TRANSFORM")
    (.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
    (doseq [m meshes]
      (when (object-by-name (.-name m))
        (.select-set m true)))
    (set! (.-active (.-objects (.-view-layer (.-context bpy)))) active)))

(defn- remove-leftover-imports!
  "Unlink leftover non-mesh objects from an import batch."
  [leftovers]
  (doseq [o leftovers]
    (when-let [obj (object-by-name (.-name o))]
      (.remove bpy.data/objects obj ** :do-unlink true))))

(defn- drop-imported-matching!
  "Unlink/remove imported objects whose name matches re. Returns kept objects."
  [imported re]
  (if-not re
    imported
    (let [drop? (fn [o] (boolean (re-find re (.-name o))))
          keep (filterv (complement drop?) imported)
          doomed (filterv drop? imported)]
      (doseq [o doomed]
        (when-let [obj (object-by-name (.-name o))]
          (.remove bpy.data/objects obj ** :do-unlink true)))
      keep)))

(defn coalesce-imported!
  "Join imported MESH objects into one named n; remove leftover non-mesh imports."
  [imported n]
  (let [meshes (filterv #(= "MESH" (.-type %)) imported)
        leftovers (filterv #(not= "MESH" (.-type %)) imported)]
    (when (empty? meshes)
      (throw (python/Exception "Import produced no MESH objects")))
    (select-meshes-for-join! meshes)
    (when (> (count meshes) 1)
      (.join (.-object (.-ops bpy))))
    (let [joined (.-object (.-context bpy))]
      (set! (.-name joined) n)
      (remove-leftover-imports! leftovers)
      joined)))

(defn- import-gltf-fresh!
  "Import filepath; return newly created objects."
  [filepath]
  (let [before (set (all-object-names))
        _ (.gltf (.-import-scene (.-ops bpy)) ** :filepath filepath)
        imported (->> bpy.data/objects
                      (filter #(not (contains? before (.-name %))))
                      vec)]
    (when (empty? imported)
      (throw (python/Exception (str "Import produced no objects: " filepath))))
    imported))

(defn import-gltf-as!
  "Import glTF/glb as object named n at identity (verts already in target frame).
  Replaces any prior object named n. Unlike place-gltf!, does not ground or scale."
  [n filepath]
  (when-not (path/exists filepath)
    (throw (python/Exception (str "glTF not found: " filepath))))
  (unlink-hierarchy! n)
  (let [imported (import-gltf-fresh! filepath)
        obj (coalesce-imported! imported n)]
    (set! (.-location obj) #py [0.0 0.0 0.0])
    {:name (.-name obj)
     :dims (object-dimensions obj)}))

(defn export-object-gltf!
  "Export object n to filepath as GLB (selection-only). Object should be unparented
  with verts already in the desired frame. Creates parent dirs."
  [n filepath]
  (if-let [obj (object-by-name n)]
    (let [dir (path/dirname filepath)]
      (when (and dir (not (path/exists dir)))
        (.makedirs os ** :path dir :exist_ok true))
      (.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
      (.select-set obj true)
      (set! (.-active (.-objects (.-view-layer (.-context bpy)))) obj)
      (.gltf (.-export-scene (.-ops bpy)) ** :filepath filepath
             :use_selection true :export_format "GLB")
      {:path filepath
       :bytes (path/getsize filepath)})
    (throw (python/Exception (str "Object not found: " n)))))

(defn- bake-mesh-rotation!
  "Bake euler degrees into mesh data so local axes match the placement frame."
  [obj rot-x-deg rot-z-deg]
  (let [rx (double (or rot-x-deg 0.0))
        rz (double (or rot-z-deg 0.0))]
    (when (or (not (zero? rx)) (not (zero? rz)))
      (set! (.-rotation-mode obj) "XYZ")
      (set! (.-rotation-euler obj) #py [(math/radians rx) 0.0 (math/radians rz)])
      (.select-all (.-object (.-ops bpy)) ** :action "DESELECT")
      (.select-set obj true)
      (set! (.-active (.-objects (.-view-layer (.-context bpy)))) obj)
      (.transform-apply (.-object (.-ops bpy)) **
                        :location false :rotation true :scale false)))
  obj)

(defn- apply-place-scale!
  "Uniform-scale obj from height/width/length/scale opts."
  [obj {:keys [height-m width-m length-m scale]}]
  (cond
    height-m (scale-to-height! obj height-m)
    width-m (scale-to-xy-width! obj width-m)
    length-m (scale-to-xy-length! obj length-m)
    scale (set! (.-scale obj) #py [(double scale) (double scale) (double scale)]))
  obj)

(defn- apply-place-rotation!
  "Set XYZ euler rotation from degrees."
  [obj rot-x-deg rot-z-deg]
  (set! (.-rotation-mode obj) "XYZ")
  (set! (.-rotation-euler obj) #py [(math/radians (double rot-x-deg))
                                    0.0
                                    (math/radians (double rot-z-deg))])
  obj)

(defn- parent-placed!
  "Parent obj under parent, optionally with pivot."
  [obj parent parent-pivot-xy]
  (when parent
    (if parent-pivot-xy
      (parent-with-pivot! obj parent parent-pivot-xy)
      (when-let [p (object-by-name parent)]
        (set! (.-parent obj) p))))
  obj)

(defn place-gltf!
  "Import glTF/glb at filepath as object named n (replaces prior hierarchy).

  Opts map:
  :location [x y z] — XY placement; Z is ground (bottom of bounds). Default [0 0 0]
  :height-m m — uniform scale so vertical extent = m (preferred for trees)
  :width-m m — uniform scale so min XY extent = m (preferred for parking cars)
  :length-m m — uniform scale so max XY extent = m (used when :width-m/:height-m absent)
  :scale s — uniform scale factor (ignored when height/width/length given)
  :mesh-rot-x-deg d — bake X rotation into mesh before scale (asset orientation)
  :mesh-rot-z-deg d — bake Z rotation into mesh before scale (asset orientation)
  :rot-z-deg d — Z rotation degrees (default 0)
  :rot-x-deg d — X rotation degrees / pitch (default 0)
  :parent name — parent under this object if present
  :parent-pivot-xy [cx cy] — with :parent, set site-root-style matrix-parent-inverse
  :skip-name-re re — drop imported objects whose name matches before join

  Returns {:name :dims :location :imported-count}, or
  {:name :skipped :missing-file :file} when filepath is absent."
  [n filepath {:keys [location rot-z-deg rot-x-deg mesh-rot-x-deg mesh-rot-z-deg parent parent-pivot-xy skip-name-re]
               :or {location [0.0 0.0 0.0] rot-z-deg 0.0 rot-x-deg 0.0}
               :as opts}]
  (if-not (path/exists filepath)
    (do
      (print (str "yardcraft: skip missing glTF: " filepath))
      {:name n :skipped :missing-file :file filepath})
    (do
      (unlink-hierarchy! n)
      (let [imported (-> (import-gltf-fresh! filepath)
                         (drop-imported-matching! skip-name-re))
            obj (coalesce-imported! imported n)
            [lx ly lz] location]
        (bake-mesh-rotation! obj mesh-rot-x-deg mesh-rot-z-deg)
        (apply-place-scale! obj opts)
        (apply-place-rotation! obj rot-x-deg rot-z-deg)
        (ground-at-z! obj lx ly lz)
        (parent-placed! obj parent parent-pivot-xy)
        (.update (.-view-layer (.-context bpy)))
        {:name (.-name obj)
         :dims (object-dimensions obj)
         :location (vec (.-location obj))
         :imported-count (count imported)}))))
