(ns yardcraft.scene
  "Observe the Blender scene: census, object-info, render-check!.

  Query before mutate. render-check! writes a temporary PNG and restores
  camera plus render settings. Show that PNG in chat before viewport handoff."
  (:import bpy
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

(defn- default-check-path
  []
  (path/join (tempfile/gettempdir) "yardcraft-visual-check.png"))

(defn render-check!
  "Render a temporary PNG for visual self-check; restore camera and render settings.
  Returns {:path :ok?} or {:path :ok? false :error ...}. Does not change suggestion/base."
  ([]
   (render-check! (default-check-path)))
  ([png-path]
   (let [scene (.-scene (.-context bpy))
         snap (snapshot-render)]
     (if-not (.-camera scene)
       {:path png-path :ok? false :error :no-camera}
       (try
         (set! (.-filepath (.-render scene)) png-path)
         (set! (.-file-format (.-image-settings (.-render scene))) "PNG")
         (.render (.-render (.-ops bpy)) ** :write_still true)
         {:path png-path :ok? true}
         (catch python/Exception e
           {:path png-path :ok? false :error (str e)})
         (finally
           (restore-render! snap)))))))

(comment
  (census)
  (object-info "site-root")
  (render-check!)
  (render-check! (path/join (tempfile/gettempdir) "yardcraft-visual-check-show.png")))
