(ns yardcraft.site
  "Yardcraft site orchestration — Phase 1 stub; empty-site kernel lands next."
  (:require [yardcraft.site-data :refer [site]]))

(defn ensure-site!
  "Placeholder until empty-site kernel is wired."
  [s]
  {:status :stub :site-keys (keys s)})

(comment
  (ensure-site! site)
  :rcf)
