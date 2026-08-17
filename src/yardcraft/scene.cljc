(ns yardcraft.scene
  "Observe the Blender scene: census, object-info, render-check!.

  Query before mutate. render-check! writes a temporary PNG and restores
  camera plus render settings. Optional :look-at (object name or world xyz)
  aims a temporary camera. Show that PNG in chat before viewport handoff."
  (:import bpy math mathutils
           [os.path :as path]
           tempfile))

(defn- xyz
  [v]
  [(double (aget v 0)) (double (aget v 1)) (double (aget v 2))])

(defn- world-xyz
  [obj]
  (xyz (.to_translation (.-matrix-world obj))))

(defn- object-record
  [obj]
  (let [parent (.-parent obj)]
    {:name (.-name obj)
     :type (.-type obj)
     :parent (when parent (.-name parent))
     :location (world-xyz obj)
     :rotation (xyz (.-rotation-euler obj))
     :scale (xyz (.-scale obj))
     :hide-viewport? (.-hide-viewport obj)
     :hide-render? (.-hide-render obj)
     :children (mapv #(.-name %) (.-children obj))}))

(defn object-info
  "World-space record for the object named n, or nil."
  [n]
  (when-let [obj (.get (.-objects (.-data bpy)) n)]
    (object-record obj)))

(defn census
  "Sorted object-info records for every object in bpy.data.objects."
  []
  (->> (.-objects (.-data bpy))
       (map object-record)
       (sort-by :name)
       vec))

(defn- snapshot-render
  []
  (let [scene (.-scene (.-context bpy))
        render (.-render scene)
        img (.-image-settings render)
        cam (.-camera scene)]
    {:filepath (.-filepath render)
     :file-format (.-file-format img)
     :resolution-x (.-resolution-x render)
     :resolution-y (.-resolution-y render)
     :resolution-percentage (.-resolution-percentage render)
     :frame (.-frame-current scene)
     :camera-name (when cam (.-name cam))
     :camera-location (when cam (xyz (.-location cam)))
     :camera-rotation (when cam (xyz (.-rotation-euler cam)))
     :camera-lens (when (and cam (= (.-type cam) "CAMERA"))
                    (.-lens (.-data cam)))}))

(defn- restore-render!
  [{:keys [filepath file-format resolution-x resolution-y
           resolution-percentage frame camera-name
           camera-location camera-rotation camera-lens]}]
  (let [scene (.-scene (.-context bpy))
        render (.-render scene)
        img (.-image-settings render)]
    (set! (.-filepath render) filepath)
    (set! (.-file-format img) file-format)
    (set! (.-resolution-x render) resolution-x)
    (set! (.-resolution-y render) resolution-y)
    (set! (.-resolution-percentage render) resolution-percentage)
    (set! (.-frame-current scene) frame)
    (when-let [cam (and camera-name (.get (.-objects (.-data bpy)) camera-name))]
      (set! (.-camera scene) cam)
      (when-let [[x y z] camera-location]
        (set! (.-location cam) #py [(double x) (double y) (double z)]))
      (when-let [[x y z] camera-rotation]
        (set! (.-rotation-euler cam) #py [(double x) (double y) (double z)]))
      (when (and camera-lens (= (.-type cam) "CAMERA"))
        (set! (.-lens (.-data cam)) camera-lens)))))

(def ^:private check-cam-name "yardcraft-render-check-cam")

(defn- remove-check-cam!
  []
  (when-let [obj (.get (.-objects (.-data bpy)) check-cam-name)]
    (.remove (.-objects (.-data bpy)) obj ** :do-unlink true)))

(defn- world-center+span
  [obj]
  (let [mw (.-matrix-world obj)
        corners (mapv (fn [c]
                        (.__matmul__ mw (mathutils/Vector #py [(double (aget c 0))
                                                               (double (aget c 1))
                                                               (double (aget c 2))])))
                      (.-bound-box obj))
        xs (mapv #(aget % 0) corners)
        ys (mapv #(aget % 1) corners)
        zs (mapv #(aget % 2) corners)
        min-x (apply min xs)
        max-x (apply max xs)
        min-y (apply min ys)
        max-y (apply max ys)
        min-z (apply min zs)
        max-z (apply max zs)]
    {:center [(/ (+ min-x max-x) 2.0)
              (/ (+ min-y max-y) 2.0)
              (/ (+ min-z max-z) 2.0)]
     :span (max (- max-x min-x) (- max-y min-y) (- max-z min-z) 1.0)}))

(defn- look-at-name
  [n]
  (if-let [obj (.get (.-objects (.-data bpy)) n)]
    (world-center+span obj)
    {:error :unknown-object :name n}))

(defn- resolve-look-at
  [look-at]
  (cond
    (string? look-at) (look-at-name look-at)
    (keyword? look-at) (look-at-name (name look-at))
    (and (sequential? look-at) (= 3 (count look-at)))
    {:center (mapv double look-at) :span 1.0}
    :else {:error :bad-look-at :look-at look-at}))

(defn- normalize3
  [[x y z]]
  (let [len (math/sqrt (+ (* x x) (* y y) (* z z)))]
    (if (< len 1.0E-6)
      [0.6 -0.7 0.45]
      [(/ x len) (/ y len) (/ z len)])))

(defn- perch-dir
  [center scene-cam]
  (let [[cx cy cz] center
        [sx sy sz] (if scene-cam
                     (world-xyz scene-cam)
                     [(+ cx 6.0) (- cy 8.0) (+ cz 5.0)])]
    (normalize3 [(- sx cx) (- sy cy) (- sz cz)])))

(defn- perch-xyz
  [[cx cy cz] [dx dy dz] distance]
  [(+ cx (* dx distance))
   (+ cy (* dy distance))
   (+ cz (* dz distance))])

(defn- aim-camera!
  [cam [fx fy fz] [tx ty tz]]
  (let [look (mathutils/Vector #py [(- tx fx) (- ty fy) (- tz fz)])
        eul (.to_euler (.to_track_quat look "-Z" "Y"))]
    (set! (.-location cam) #py [(double fx) (double fy) (double fz)])
    (set! (.-rotation-euler cam) #py [(aget eul 0) (aget eul 1) (aget eul 2)])
    cam))

(defn- ensure-check-cam!
  [from-xyz target-xyz scene-cam]
  (remove-check-cam!)
  (.camera-add (.-object (.-ops bpy)) ** :location #py [0.0 0.0 0.0])
  (let [cam (.-object (.-context bpy))]
    (set! (.-name cam) check-cam-name)
    (set! (.-show-passepartout (.-data cam)) false)
    (when (and scene-cam (= (.-type scene-cam) "CAMERA"))
      (set! (.-lens (.-data cam)) (.-lens (.-data scene-cam))))
    (aim-camera! cam from-xyz target-xyz)
    (set! (.-camera (.-scene (.-context bpy))) cam)
    cam))

(defn- apply-look-at!
  [{:keys [center span]} distance scene-cam]
  (let [dir (perch-dir center scene-cam)
        dist (or distance (max 8.0 (* 2.2 span)))
        from (perch-xyz center dir dist)]
    (ensure-check-cam! from center scene-cam)))

(defn- write-still!
  [png-path]
  (let [scene (.-scene (.-context bpy))]
    (set! (.-filepath (.-render scene)) png-path)
    (set! (.-file-format (.-image-settings (.-render scene))) "PNG")
    (.render (.-render (.-ops bpy)) ** :write_still true)))

(defn- default-check-path
  []
  (path/join (tempfile/gettempdir) "yardcraft-visual-check.png"))

(defn- run-check!
  [png-path aim distance]
  (let [scene (.-scene (.-context bpy))
        snap (snapshot-render)]
    (if (and (not (.-camera scene)) (not aim))
      {:path png-path :ok? false :error :no-camera}
      (try
        (when aim
          (apply-look-at! aim distance (.-camera scene)))
        (write-still! png-path)
        {:path png-path :ok? true :look-at (some-> aim :center)}
        (catch python/Exception e
          {:path png-path :ok? false :error (str e)})
        (finally
          (restore-render! snap)
          (remove-check-cam!))))))

(defn render-check!
  "Render a temporary PNG for visual self-check; restore camera and render settings.
  Optional opts {:look-at name-or-[x y z] :distance m} aims a temp camera.
  Returns {:path :ok?} or {:path :ok? false :error ...}. Does not change suggestion/base."
  ([]
   (render-check! (default-check-path) nil))
  ([png-path]
   (render-check! png-path nil))
  ([png-path {:keys [look-at distance]}]
   (let [aim (when look-at (resolve-look-at look-at))]
     (if (:error aim)
       (assoc aim :path png-path :ok? false)
       (run-check! png-path aim distance)))))

(comment
  (census)
  (object-info "site-root")
  (render-check!)
  (render-check! (path/join (tempfile/gettempdir) "yardcraft-visual-check-show.png"))
  (render-check! (path/join (tempfile/gettempdir) "yardcraft-visual-check-look.png")
                 {:look-at "site-sundial-face"}))
