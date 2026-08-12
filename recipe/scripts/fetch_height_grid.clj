#!/usr/bin/env bb
(ns fetch-height-grid
  "Example höjd grid fetch for Lantmäteriet Min Karta style APIs.
  Pass --lot-id <objektidentitet> — never hardcode a private lot id in the template.
  See recipe/skills/references/sweden-lantmateriet/ for CRS (SWEREF99) + RH00."
  (:require [babashka.cli :as cli]
            [babashka.http-client :as http]
            [babashka.fs :as fs]
            [cheshire.core :as json]))

(def base "https://minkarta.lantmateriet.se")

(defn hojd-at [e n]
  (let [url (str base "/api/positionsinformation/positionsinformation/v1/hojd"
                 "?transactionId=" (random-uuid)
                 "&east=" e "&north=" n)
        body (json/parse-string (:body (http/get url)) true)]
    {:east e :north n :hojd (:hojd body)}))

(defn -main [& args]
  (let [{:keys [lot-id out step]
         :or {out "src/yardcraft/data/lm_height_grid.edn"
              step 5.0}}
        (cli/parse-opts args {:spec {:lot-id {:require true}
                                     :out {}
                                     :step {:coerce :double}}})]
    (println "Fetching geometry for lot-id" lot-id)
    (let [geom (-> (str base "/api/searchservice/fastighetsgeometri/v1?objektidentitet=" lot-id)
                   http/get :body (json/parse-string true))
          ;; Minimal stub: write metadata only — extend sampling using the Sweden reference skill
          edn {:lot-id lot-id
               :source base
               :note "Extend this script using recipe/skills/references/sweden-lantmateriet/ — sample höjd inside the parcel ring."
               :geometry-keys (keys geom)}]
      (fs/create-dirs (fs/parent out))
      (spit out (pr-str edn))
      (println "Wrote" out "(metadata stub — flesh out per Sweden reference skill)"))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
