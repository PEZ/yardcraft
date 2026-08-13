(ns yardcraft.site-demo-hierarchy
  "Outliner grouping for the welcome demo under site-root (altan-style).
  Fly/sun stay world-level via site-hierarchy root exclusions."
  (:require [yardcraft.site-hierarchy :as hierarchy]))

(def ^:private demo-hierarchy
  "Outliner nesting for the welcome demo under site-root.
  Fly path/camera and site-sun stay world-level (excluded from site-root)."
  [["site-grp-lot"
    [:exact "site-demo-terrain"]]
   ["site-grp-yardcraft"
    ["site-grp-yard"
     [:prefix "site-demo-patio-0-"]
     [:prefix "site-demo-patio-1-"]
     [:prefix "site-demo-patio-2-"]
     [:prefix "site-demo-patio-3-"]]
    ["site-grp-craft"
     [:exact "site-demo-brick"]
     [:prefix "site-demo-patio-4-"]
     [:prefix "site-demo-patio-5-"]
     [:prefix "site-demo-patio-6-"]
     [:prefix "site-demo-patio-7-"]
     [:prefix "site-demo-patio-8-"]
     ["site-grp-stairs"
      [:prefix "site-demo-stair-"]
      [:prefix "site-railing-demo-"]]
     ["site-grp-pedestal"
      [:exact "site-demo-pedestal"]
      [:prefix "site-sundial-"]]]]
   ["site-grp-furniture"
    [:prefix "site-furniture"]]])

(defn sync-demo-hierarchy!
  "Organize demo Outliner groups and parent under site-root (identity pivot; no house).
  Does not rotate site-root (demo keeps north-offset 0)."
  [s]
  {:hierarchy (hierarchy/organize-hierarchy! s demo-hierarchy)
   :root (hierarchy/parent-under-site-root! s)})
