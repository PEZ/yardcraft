(ns yardcraft.site-ui
  "View3D N-panel Yardcraft controls — register! from the basilisp-blender REPL.
  Complements RCFs: sun date/time, canopy covering, suggestion show/base."
  (:require [basilisp.string :as string]
            [basilisp-blender.utils :refer [class-make*]]
            [yardcraft.site-data :refer [site]]
            [yardcraft.site :as site]
            [yardcraft.site-fly :as fly]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-terrace :as terrace]
            [yardcraft.site-suggestions :as sug]
            [yardcraft.site-sun :as sun]
            [yardcraft.site-viewport :as viewport]))

(defn- bpy-mod
  "Fresh bpy module (reload-safe; avoid stale (:import bpy) gensym)."
  []
  (python/__import__ "bpy"))

(def ^:private covering-name "site-terrace-roof-covering")

(def ^:private suppress-updates?* false)

(def ^:private classes* nil)

(def ^:private finished #py #{"FINISHED"})

(def ^:private date-enum
  [["pre_summer" "May 15" "2026-05-15"]
   ["midsummer" "June 21" "2026-06-21"]
   ["high_summer" "July 15" "2026-07-15"]
   ["patio_season" "August 2" "2026-08-02"]])

(def ^:private date-id->iso
  {"pre_summer" "2026-05-15"
   "patio_season" "2026-08-02"
   "midsummer" "2026-06-21"
   "high_summer" "2026-07-15"})

(def ^:private iso->date-id
  {"2026-05-15" "pre_summer"
   "2026-08-02" "patio_season"
   "2026-06-21" "midsummer"
   "2026-07-15" "high_summer"
   "2026-12-21" "winter_solstice"})

(defn- suppress-updates?
  []
  @#'suppress-updates?*)

(defn- set-suppress-updates!
  [v]
  (alter-var-root #'suppress-updates?* (constantly v)))

(defn- set-classes!
  [xs]
  (alter-var-root #'classes* (constantly xs)))

(defn- minutes->hhmm
  [m]
  (let [h (quot m 60)
        mi (mod m 60)
        pad (fn [n] (str (when (< n 10) "0") n))]
    (str (pad h) ":" (pad mi))))

(defn- hhmm->minutes
  [time-str]
  (let [[h mi] (sun/parse-hhmm time-str)]
    (+ (* h 60) mi)))

(defn- clamp-minutes
  [m]
  (max 360 (min 1320 m)))

(def ^:private tod-min-sec (* 6.0 3600.0))

(def ^:private tod-max-sec (* 22.0 3600.0))

(def ^:private tod-default-sec (* 13.0 3600.0))

(defn- clamp-seconds
  [s]
  (max tod-min-sec (min tod-max-sec (double s))))

(defn- seconds->hhmm
  [s]
  (minutes->hhmm (int (+ 0.5 (/ (clamp-seconds s) 60.0)))))

(defn- hhmm->seconds
  [time-str]
  (* (double (hhmm->minutes time-str)) 60.0))

(defn- coerce-date-id
  [iso-or-id]
  (or (get iso->date-id iso-or-id)
      (when (contains? date-id->iso iso-or-id) iso-or-id)
      "patio_season"))

(defn- canopy-visible?
  []
  (if-let [obj (mesh/object-by-name covering-name)]
    (not (.-hide_viewport obj))
    true))

(defn- on-canopy-update
  [props _context]
  (when-not (suppress-updates?)
    (terrace/set-terrace-roof-covering-visible! (.-canopy_visible props))))

(defn- on-date-update
  [props _context]
  (when-not (suppress-updates?)
    (site/set-sun-date! site (get date-id->iso (.-sun_date props)))))

(defn- on-time-update
  [props _context]
  (when-not (suppress-updates?)
    (site/preview-time-of-day! site (seconds->hhmm (.-sun_time_of_day props)))))

(defn- suggestion-enum-id
  [id]
  (string/replace (name id) "-" "_"))

(defn- suggestion-keyword
  [enum-id]
  (keyword (string/replace enum-id "_" "-")))

(defn- suggestion-items
  [_props _context]
  (python/list
   (cons (python/tuple ["__base__" "Base" ""])
         (map (fn [{:keys [id title source]}]
                (python/tuple [(suggestion-enum-id id)
                               (str (or title (name id))
                                    (when (= source :session) " (session)"))
                               ""]))
              (sug/list-suggestions)))))

(defn- date-items
  []
  (python/list
   (map (fn [[id label _iso]]
          (python/tuple [id label ""]))
        date-enum)))

(defn- make-sun-tod-prop
  []
  (let [mk (fn [unit]
             (.FloatProperty (.-props (bpy-mod))
                             ** :name "Time of day"
                             :description "Local clock time (preview on scrub; Apply to commit)"
                             :min tod-min-sec
                             :max tod-max-sec
                             :soft_min tod-min-sec
                             :soft_max tod-max-sec
                             :step 90000
                             :default tod-default-sec
                             :precision 0
                             :unit unit
                             :update on-time-update))]
    (try
      (mk "TIME_ABSOLUTE")
      (catch python/Exception _
        (mk "TIME")))))

(defn- make-settings-class
  []
  (let [canopy (.BoolProperty (.-props (bpy-mod))
                              ** :name "Canopy covering"
                              :description "Show terrace roof covering"
                              :default true
                              :update on-canopy-update)
        sun-date (.EnumProperty (.-props (bpy-mod))
                                ** :name "Sun date"
                                :description "Named day for sun aim"
                                :items (date-items)
                                :default "patio_season"
                                :update on-date-update)
        sun-tod (make-sun-tod-prop)
        sug-id (.EnumProperty (.-props (bpy-mod))
                              ** :name "Suggestion"
                              :description "Design suggestion (Show/Base to apply)"
                              :items (suggestion-items nil nil)
                              :default "__base__")
        ann (python/dict {"canopy_visible" canopy
                          "sun_date" sun-date
                          "sun_time_of_day" sun-tod
                          "suggestion_id" sug-id})]
    (python/type "YARDCRAFT_PG_settings"
                 (python/tuple [(.-PropertyGroup (.-types (bpy-mod)))])
                 (python/dict {"canopy_visible" canopy
                               "sun_date" sun-date
                               "sun_time_of_day" sun-tod
                               "suggestion_id" sug-id
                               "__annotations__" ann}))))

(defn- make-apply-time-op
  []
  (class-make* "YARDCRAFT_OT_apply_time"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.apply_time"} bl_idname
                ^{:default "Apply time"} bl_label
                ^{:default "Commit staged time of day (loungers + persist)"} bl_description]
               (execute [context]
                        (let [props (.-yardcraft (.-scene context))]
                          (site/set-time-of-day! site (seconds->hhmm (.-sun_time_of_day props)))
                          finished))))

(defn- make-show-suggestion-op
  []
  (class-make* "YARDCRAFT_OT_show_suggestion"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.show_suggestion"} bl_idname
                ^{:default "Show suggestion"} bl_label
                ^{:default "Apply selected design suggestion"} bl_description]
               (execute [context]
                        (let [props (.-yardcraft (.-scene context))
                              enum-id (.-suggestion_id props)]
                          (if (or (not enum-id) (= enum-id "__base__"))
                            finished
                            (let [id (suggestion-keyword enum-id)]
                              (try
                                (when (not= (sug/active-id) id)
                                  (sug/show! site id))
                                (set-suppress-updates! true)
                                (try
                                  (set! (.-suggestion_id (.-yardcraft (.-scene context)))
                                        (suggestion-enum-id id))
                                  (finally
                                    (set-suppress-updates! false)))
                                finished
                                (catch python/Exception e
                                  (.report self #py #{"ERROR"}
                                           (str "Show failed: " e))
                                  #py #{"CANCELLED"}))))))))

(defn- make-show-base-op
  []
  (class-make* "YARDCRAFT_OT_show_base"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.show_base"} bl_idname
                ^{:default "Base"} bl_label
                ^{:default "Restore base site (clear active suggestion)"} bl_description]
               (execute [context]
                        (sug/show-base! site)
                        (set-suppress-updates! true)
                        (try
                          (set! (.-suggestion_id (.-yardcraft (.-scene context))) "__base__")
                          (finally
                            (set-suppress-updates! false)))
                        finished)))

(defn- make-frame-lot-top-op
  []
  (class-make* "YARDCRAFT_OT_frame_lot_top"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.frame_lot_top"} bl_idname
                ^{:default "Lot north"} bl_label
                ^{:default "Top orthographic view framing the lot with ~6% margin (world north up)."} bl_description]
               (execute [_context]
                        (viewport/frame-lot-top! site)
                        finished)))

(defn- make-frame-lot-house-op
  []
  (class-make* "YARDCRAFT_OT_frame_lot_house"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.frame_lot_house"} bl_idname
                ^{:default "House north"} bl_label
                ^{:default "Top ortho framing the lot; house west left / road up (site-root axes)."} bl_description]
               (execute [_context]
                        (viewport/frame-lot-top-house! site)
                        finished)))

(defn- make-frame-house-south-op
  []
  (class-make* "YARDCRAFT_OT_frame_house_south"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.frame_house_south"} bl_idname
                ^{:default "House south"} bl_label
                ^{:default "Ortho looking straight at the house south facade."} bl_description]
               (execute [_context]
                        (viewport/frame-house-south! site)
                        finished)))

(defn- make-frame-house-east-op
  []
  (class-make* "YARDCRAFT_OT_frame_house_east"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.frame_house_east"} bl_idname
                ^{:default "House east"} bl_label
                ^{:default "Ortho looking straight at the house east facade."} bl_description]
               (execute [_context]
                        (viewport/frame-house-east! site)
                        finished)))

(defn- make-view-fly-camera-op
  []
  (class-make* "YARDCRAFT_OT_view_fly_camera"
               [(.-Operator (.-types (bpy-mod)))]
               [^{:default "yardcraft.view_fly_camera"} bl_idname
                ^{:default "Fly cam"} bl_label
                ^{:default "Build/refresh fly tour, make site-fly-camera active, enter camera view."} bl_description]
               (execute [_context]
                        (fly/ensure-fly-tour! site)
                        finished)))

(defn- sync-suggestion-enum-from-active!
  "Keep dropdown on the active suggestion after scene rebuilds. Does not
  force Base when none is active (preserves staged pre-Show selection)."
  [props]
  (when-let [active (sug/active-id)]
    (let [want (suggestion-enum-id active)]
      (when (not= (.-suggestion_id props) want)
        (set-suppress-updates! true)
        (try
          (set! (.-suggestion_id props) want)
          (catch python/Exception _)
          (finally
            (set-suppress-updates! false)))))))

(defn- make-panel
  []
  (class-make* "YARDCRAFT_PT_panel"
               [(.-Panel (.-types (bpy-mod)))]
               [^{:default "VIEW3D_PT_yardcraft"} bl_idname
                ^{:default "Yardcraft"} bl_label
                ^{:default "VIEW_3D"} bl_space_type
                ^{:default "UI"} bl_region_type
                ^{:default "Yardcraft"} bl_category]
               (draw [context]
                     (let [layout (.-layout self)
                           props (.-yardcraft (.-scene context))]
                       (when props
                         (sync-suggestion-enum-from-active! props)
                         (.prop layout props "sun_date" ** :text "Sun date")
                         (.prop layout props "sun_time_of_day"
                                ** :text (seconds->hhmm (.-sun_time_of_day props))
                                :slider true)
                         (.operator layout "yardcraft.apply_time" ** :text "Apply time")
                         (.separator layout)
                         (.prop layout props "canopy_visible" ** :text "Canopy covering")
                         (.separator layout)
                         (.prop layout props "suggestion_id" ** :text "Suggestion")
                         (let [row (.row layout)]
                           (.operator row "yardcraft.show_suggestion" ** :text "Show")
                           (.operator row "yardcraft.show_base" ** :text "Base"))
                         (.separator layout)
                         (.label layout ** :text "View")
                         (let [row (.row layout)]
                           (.operator row "yardcraft.frame_lot_top" ** :text "Lot north")
                           (.operator row "yardcraft.frame_lot_house" ** :text "House north"))
                         (let [row (.row layout)]
                           (.operator row "yardcraft.frame_house_south" ** :text "House south")
                           (.operator row "yardcraft.frame_house_east" ** :text "House east"))
                         (let [row (.row layout)]
                           (.operator row "yardcraft.view_fly_camera" ** :text "Fly cam")))))))

(defn- seed-props!
  [props]
  (set-suppress-updates! true)
  (try
    (let [date-id (coerce-date-id (or (:sun/date site) "2026-08-02"))
          time (or (:sun/time-of-day site) "13:00")
          active (sug/active-id)]
      (set! (.-sun_date props) date-id)
      (set! (.-sun_time_of_day props) (clamp-seconds (hhmm->seconds time)))
      (set! (.-canopy_visible props) (canopy-visible?))
      (try
        (set! (.-suggestion_id props) (if active (suggestion-enum-id active) "__base__"))
        (catch python/Exception _
          (set! (.-suggestion_id props) "__base__"))))
    (finally
      (set-suppress-updates! false))))

(defn unregister!
  "Remove Yardcraft N-panel classes and Scene.yardcraft pointer."
  []
  (when (hasattr (.-Scene (.-types (bpy-mod))) "yardcraft")
    (delattr (.-Scene (.-types (bpy-mod))) "yardcraft"))
  (doseq [cls (reverse (or @#'classes* []))]
    (when (hasattr (.-types (bpy-mod)) (.-__name__ cls))
      (.unregister_class (.-utils (bpy-mod)) cls)))
  (set-classes! nil)
  :unregistered)

(defn register!
  "Register Yardcraft N-panel (idempotent). Call after ensure-site! for meaningful controls."
  []
  (unregister!)
  (let [pg (make-settings-class)
        ot-time (make-apply-time-op)
        ot-show (make-show-suggestion-op)
        ot-base (make-show-base-op)
        ot-lot-top (make-frame-lot-top-op)
        ot-lot-house (make-frame-lot-house-op)
        ot-house-south (make-frame-house-south-op)
        ot-house-east (make-frame-house-east-op)
        ot-fly-cam (make-view-fly-camera-op)
        panel (make-panel)
        classes [pg ot-time ot-show ot-base
                 ot-lot-top ot-lot-house ot-house-south ot-house-east
                 ot-fly-cam panel]]
    (run! #(.register_class (.-utils (bpy-mod)) %) classes)
    (set! (.-yardcraft (.-Scene (.-types (bpy-mod))))
          (.PointerProperty (.-props (bpy-mod)) ** :type pg))
    (set-classes! classes)
    (seed-props! (.-yardcraft (.-scene (.-context (bpy-mod)))))
    :registered))

(defn reload!
  "Unregister, reload this ns, register again."
  []
  (unregister!)
  (require 'yardcraft.site-ui :reload)
  ((resolve 'yardcraft.site-ui/register!))
  :reloaded)

(comment
  ;; Basilisp: trailing :reload drops :as — two-step require (same as site-suggestions).
  (require 'yardcraft.site-ui :reload)
  (require '[yardcraft.site-ui :as ui])
  (ui/register!)
  (ui/unregister!)
  (ui/reload!)
  ;; Session suggestion (no EDN file) — then re-register to refresh enum:
  ;; (require '[yardcraft.site-suggestions :as sug])
  ;; (sug/register-suggestion! {:suggestion/id :my-idea
  ;;                            :suggestion/title "My idea"
  ;;                            :suggestion/note "Design option — session only."
  ;;                            :suggestion/domains #{:terrace}
  ;;                            :suggestion/patch {:terrace/depth-m 5.2}})
  ;; (ui/register!)
  :rcf)
