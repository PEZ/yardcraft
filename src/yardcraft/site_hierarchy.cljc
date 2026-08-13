(ns yardcraft.site-hierarchy
  "Site-root parenting and Outliner group hierarchy.

  Domain builders take facts map `s`. No persist-site!."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-house :as house])
  (:import bpy math mathutils))

(defn ensure-site-root!
  "Empty parent at world origin; identity rotation until orient-site-root!."
  [_s]
  (mesh/unlink-and-remove! "site-root")
  (.empty-add (.-object (.-ops bpy)) ** :type "PLAIN_AXES"
              :location #py [0.0 0.0 0.0])
  (let [root (.-object (.-context bpy))]
    (set! (.-name root) "site-root")
    (set! (.-hide-viewport root) true)
    (set! (.-hide-render root) true)
    (set! (.-rotation-euler root) #py [0.0 0.0 0.0])
    root))

(defn- root-excluded-name?
  "Names that must not be parented under site-root."
  [n]
  (or (#{"site-root" "site-sun"} n)
      (string/starts-with? n "site-north")
      (string/starts-with? n "site-fly-")))

(defn- unparented-name?
  "True when the named object exists and has no parent."
  [n]
  (when-let [obj (mesh/object-by-name n)]
    (nil? (.-parent obj))))

(defn- candidate-root-children
  "Unparented site-* names eligible for direct site-root parenting."
  []
  (->> (mesh/site-object-names)
       (remove root-excluded-name?)
       (filter unparented-name?)
       vec))

(defn- house-center-parent-inverse
  "Parent-inverse matrix mapping house XY center to world origin."
  [s]
  (let [[cx cy] (house/house-center-xy s)]
    (.Translation (.-Matrix mathutils)
                  #py [(double (- cx)) (double (- cy)) 0.0])))

(defn- parent-with-inverse!
  [obj root mpi]
  (set! (.-parent obj) root)
  (set! (.-matrix-parent-inverse obj) (.copy mpi)))

(defn- parent-names-under-root!
  "Parent each named object under root with the given parent-inverse."
  [root mpi names]
  (doseq [n names]
    (when-let [obj (mesh/object-by-name n)]
      (parent-with-inverse! obj root mpi)))
  names)

(defn parent-under-site-root!
  "Parent unparented site-* under site-root so house XY center maps to world origin.
  Skips objects already nested under site-grp-* (or any parent)."
  [s]
  (when-let [root (mesh/object-by-name "site-root")]
    (let [[cx cy] (house/house-center-xy s)
          mpi (house-center-parent-inverse s)
          names (candidate-root-children)]
      (parent-names-under-root! root mpi names)
      {:parented names :pivot-xy [cx cy]})))

(defn orient-site-root!
  "Rotate site-root at world origin so house-local +Y (road) points toward true north offset.
  Uses 0 when :site/north-offset-deg is nil (empty-site path)."
  [s]
  (when-let [root (mesh/object-by-name "site-root")]
    (let [deg (or (:site/north-offset-deg s) 0.0)
          offset (math/radians deg)]
      (set! (.-rotation-euler root) #py [0.0 0.0 (double (- offset))])
      {:name "site-root" :north-offset-deg deg})))

(def ^:private site-hierarchy
  "Outliner nesting under site-root. Node: [group-name & children].
  Child is either a nested node or a matcher [:exact name] / [:prefix prefix]."
  [["site-grp-lot"
    [:exact "site-terrain"]
    [:exact "site-draw-pad"]
    [:exact "site-road-frontage"]
    [:prefix "site-contour-"]
    [:prefix "site-fence"]]
   ["site-grp-driveway"
    [:exact "site-driveway"]
    [:exact "site-driveway-wall"]]
   ["site-grp-house"
    ["site-grp-house-body"
     [:exact "site-house"]
     [:exact "site-house-ground"]
     [:exact "site-house-roof"]
     [:exact "site-bay-window"]
     [:prefix "site-door-"]]
    ["site-grp-veranda"
     [:prefix "site-veranda-"]]
    ["site-grp-bod"
     [:exact "site-bod"]
     [:exact "site-bod-roof"]]]
   ["site-grp-terrace"
    ["site-grp-terrace-deck"
     [:prefix "site-terrace-west"]
     [:exact "site-terrace-east-low"]
     [:exact "site-terrace-bod-south"]
     [:prefix "site-terrace-south"]]
    ["site-grp-stairs"
     [:prefix "site-stair-"]]
    ["site-grp-supports"
     [:prefix "site-terrace-post"]]
    ["site-grp-railing"
     [:prefix "site-railing"]]
    ["site-grp-furniture"
     [:prefix "site-furniture"]]
    ["site-grp-terrace-roof"
     [:exact "site-terrace-roof-covering"]
     [:prefix "site-terrace-roof-frame"]
     [:prefix "site-terrace-roof-pole"]]]
   ["site-grp-sundial"
    [:prefix "site-sundial-"]]
   ["site-grp-props"
    [:prefix "site-car-"]
    [:prefix "site-tree-"]
    [:exact "site-mailbox"]]])

(defn- hierarchy-node?
  [x]
  (and (vector? x) (string? (first x))))

(defn- matcher-names
  "Resolve :exact / :prefix matcher to existing non-group site object names."
  [[kind val]]
  (case kind
    :exact (if (mesh/object-by-name val) [val] [])
    :prefix (->> (mesh/site-object-names)
                 (remove #(string/starts-with? % "site-grp-"))
                 (filter #(string/starts-with? % val))
                 vec)
    []))

(declare apply-hierarchy-node!)

(defn- parent-hierarchy-child!
  "Parent one hierarchy child (nested node or matcher) under group-name."
  [group-name child]
  (if (hierarchy-node? child)
    (do (apply-hierarchy-node! child)
        (mesh/parent-identity! (first child) group-name))
    (run! #(mesh/parent-identity! % group-name) (matcher-names child))))

(defn- apply-hierarchy-node!
  "Ensure group empty and parent children (nested groups or matched objects)."
  [[group-name & children]]
  (mesh/ensure-empty! group-name)
  (doseq [child children]
    (parent-hierarchy-child! group-name child))
  group-name)

(defn- parent-top-groups-under-root!
  "Parent top-level hierarchy groups under site-root with house-center pivot."
  [s top]
  (when-let [root (mesh/object-by-name "site-root")]
    (parent-names-under-root! root (house-center-parent-inverse s) top)))

(defn organize-hierarchy!
  "Nest site meshes under hidden site-grp-* empties per `hierarchy` tree.
  Top groups parent under site-root with the house-center pivot when root exists.
  Idempotent."
  [s hierarchy]
  (let [top (mapv apply-hierarchy-node! hierarchy)]
    (parent-top-groups-under-root! s top)
    {:groups top}))

(defn organize-site-hierarchy!
  "Nest site meshes under the default site Outliner tree; parent under site-root."
  [s]
  (organize-hierarchy! s site-hierarchy))
