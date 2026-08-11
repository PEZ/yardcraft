(ns yardcraft.site-paint
  "Viewport material assignment for site-* meshes."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh]))

(defn- color
  [k]
  (get mesh/material-colors k))

(defn- mat!
  "Ensure a material from material-colors key + optional kwargs."
  [name color-key & {:as opts}]
  (apply mesh/ensure-material! name (color color-key) (mapcat identity opts)))

(defn- ensure-mats
  "Build keyword→material map. Reads :terrace/roof-opacity from s."
  [s]
  {:terrain (mat! "site-mat-terrain" :terrain)
   :contour (mat! "site-mat-contour" :contour)
   :draw-pad (mat! "site-mat-draw-pad" :draw-pad)
   :house (mat! "site-mat-house" :house)
   :house-pad (mat! "site-mat-house-pad" :house-pad)
   :roof (mat! "site-mat-roof" :roof)
   :bod (mat! "site-mat-bod" :bod)
   :bay (mat! "site-mat-bay" :bay)
   :terrace (mat! "site-mat-terrace" :terrace :roughness 0.85)
   :terrace-roof (mat! "site-mat-terrace-roof" :terrace-roof
                       :alpha (:terrace/roof-opacity s 1.0))
   :driveway (mat! "site-mat-driveway" :driveway :roughness 0.9)
   :driveway-wall (mat! "site-mat-driveway-wall" :driveway-wall :roughness 0.9)
   :road (mat! "site-mat-road" :road :roughness 0.9)
   :stairs (mat! "site-mat-stairs" :stairs :roughness 0.85)
   :railing (mat! "site-mat-railing" :railing :roughness 0.85)
   :furniture (mat! "site-mat-furniture" :furniture :roughness 0.85)
   :support (mat! "site-mat-support" :support :roughness 0.9)
   :door (mat! "site-mat-door" :door)
   :veranda (mat! "site-mat-veranda" :veranda :roughness 0.8)
   :north (mat! "site-mat-north" :north)
   :sundial (mat! "site-mat-sundial" :sundial)
   :sundial-mark (mat! "site-mat-sundial-mark" :sundial-mark)
   :sundial-noon (mat! "site-mat-sundial-noon" :sundial-noon)})

(defn- named-pairs
  [m]
  [["site-terrain" (:terrain m)]
   ["site-road-frontage" (:road m)]
   ["site-draw-pad" (:draw-pad m)]
   ["site-house-ground" (:house-pad m)]
   ["site-house" (:house m)]
   ["site-house-roof" (:roof m)]
   ["site-bod" (:bod m)]
   ["site-bod-roof" (:roof m)]
   ["site-bay-window" (:bay m)]
   ["site-terrace-south-main" (:terrace m)]
   ["site-terrace-south-apron" (:terrace m)]
   ["site-terrace-west" (:terrace m)]
   ["site-terrace-west-bridge" (:terrace m)]
   ["site-terrace-east-low" (:terrace m)]
   ["site-terrace-bod-south" (:terrace m)]
   ["site-terrace-roof-covering" (:terrace-roof m)]
   ["site-driveway" (:driveway m)]
   ["site-driveway-wall" (:driveway-wall m)]
   ["site-door-west" (:door m)]
   ["site-door-south" (:door m)]
   ["site-door-north" (:door m)]
   ["site-north" (:north m)]
   ["site-north-head" (:north m)]
   ["site-sundial-face" (:sundial m)]
   ["site-sundial-gnomon" (:sundial m)]
   ["site-sundial-hour-12" (:sundial-noon m)]])

(defn- prefixed
  [names prefix mat]
  (map #(vector % mat)
       (filter #(string/starts-with? % prefix) names)))

(defn- sundial-hour-pairs
  [names mat]
  (map #(vector % mat)
       (filter #(and (string/starts-with? % "site-sundial-hour")
                     (not= % "site-sundial-hour-12"))
               names)))

(defn- prefix-pairs
  [m]
  (let [names (mesh/site-object-names)]
    (concat (prefixed names "site-stair" (:stairs m))
            (prefixed names "site-railing" (:railing m))
            (prefixed names "site-furniture" (:furniture m))
            (prefixed names "site-fence" (:railing m))
            (prefixed names "site-terrace-post" (:support m))
            (prefixed names "site-terrace-roof-frame" (:support m))
            (prefixed names "site-terrace-roof-pole" (:support m))
            (prefixed names "site-contour-" (:contour m))
            (prefixed names "site-veranda" (:veranda m))
            (sundial-hour-pairs names (:sundial-mark m)))))

(defn paint-site!
  "Apply paints to current site-* meshes. Takes facts map s."
  [s]
  (let [m (ensure-mats s)
        pairs (concat (named-pairs m) (prefix-pairs m))]
    {:assigned (mapv (fn [[n mat]] (mesh/assign-material! n mat)) pairs)}))
