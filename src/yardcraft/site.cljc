(ns yardcraft.site
  "Yardcraft site model — orchestration entrypoint, paint, sun/north, viewport.

  Units: 1 Blender unit = 1 meter.
  House-NW frame when facts exist: origin at NW corner; +X along house; +Y toward
  access road; Z=0 at constructed house platform.
  World horizontal origin = house center after parenting; world +Y = true north.

  Facts: yardcraft.site-data/site
  Scene objects use the `site-` name prefix; clear-site! removes them.

  Empty facts: (ensure-site! s) → site-root + sun/world/viewport defaults only."
  (:require [basilisp.string :as string]
            [yardcraft.site-data :as data :refer [site]]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-paint :as paint]
            [yardcraft.site-viewport :as viewport]
            [yardcraft.site-sun :as sun]
            [yardcraft.site-hierarchy :as hierarchy]
            [yardcraft.site-draw :as draw]
            [yardcraft.site-house :as house]
            [yardcraft.site-lm :as lm]
            [yardcraft.site-lot :as lot]
            [yardcraft.site-terrace :as terrace]
            [yardcraft.site-furniture :as furniture]
            [yardcraft.site-driveway :as driveway]
            [yardcraft.site-fence :as fence]
            [yardcraft.site-plan :as plan]
            [yardcraft.site-props :as props]
            [yardcraft.site-sketch :as sketch]
            [yardcraft.site-fly :as fly]
            [yardcraft.site-ui :as ui]))

(defn facts-sufficient?
  "True when core geometry facts exist for a full site rebuild.
  Requires :house/size-m and :lot/polygon-xy."
  [s]
  (boolean (and (:house/size-m s)
                (:lot/polygon-xy s))))

(defn clear-site!
  "Removes scene objects except draft-* contour sketches.
  Returns {:removed names}."
  []
  (let [names (->> (mesh/all-object-names)
                   (remove #(string/starts-with? % "draft-")))]
    (run! mesh/unlink-and-remove! names)
    {:removed names}))

(defn sync-site-hierarchy!
  "Nest site-* under site-grp-* and parent under site-root (house-center pivot).
  Call after any partial ensure-*! that creates/replaces meshes."
  [s]
  {:hierarchy (hierarchy/organize-site-hierarchy! s)
   :root (hierarchy/parent-under-site-root! s)})

(defn ensure-terrace-furniture-scene!
  "Partial furniture rebuild, then sync under site-root and paint."
  [s]
  (let [furn (furniture/ensure-terrace-furniture! s)
        hier (sync-site-hierarchy! s)
        painted (paint/paint-site! s)]
    (merge furn hier {:paint painted})))

(defn set-time-of-day!
  "Aim site-sun for local clock time \"HH:MM\"; re-orient loungers when furniture exists.
  Requires :site/lat-deg :site/lon-deg :site/timezone :sun/date."
  [s time-str]
  (let [r (sun/aim-sun-at-clock s time-str)
        s' (:site r)]
    (data/persist-site! s')
    (when (facts-sufficient? s')
      (furniture/orient-loungers-to-sun! s')
      (sync-site-hierarchy! s')
      (paint/paint-site! s'))
    (dissoc r :site)))

(defn set-sun-date!
  "Set :sun/date; re-aim sun. Requires lat/lon for solar calc."
  [s date-str]
  (sun/parse-iso-date date-str)
  (let [time (or (:sun/time-of-day s) "13:00")
        r (sun/aim-sun-at-clock (assoc s :sun/date date-str) time)
        s' (:site r)]
    (data/persist-site! s')
    (when (facts-sufficient? s')
      (furniture/orient-loungers-to-sun! s')
      (sync-site-hierarchy! s')
      (paint/paint-site! s'))
    (dissoc r :site)))

(defn preview-time-of-day!
  "Aim site-sun for HH:MM only — no persist, lounger orient, sync, or paint."
  [s time-str]
  (dissoc (sun/aim-sun-at-clock s time-str) :site))

(defn ensure-draw-structures!
  "Fast reference massing for contour draw — no terrain.
  No-ops geometry when facts insufficient."
  [s]
  (if-not (facts-sufficient? s)
    (do
      (hierarchy/ensure-site-root! s)
      (hierarchy/orient-site-root! s)
      {:status :empty-defaults :house? false})
    (let [s (lm/with-lm-road s)]
      (draw/ensure-draw-root! s)
      (when-not (mesh/object-by-name "site-house")
        (lot/ensure-road! s)
        (driveway/ensure-driveway! s)
        (house/ensure-house-ground! s)
        (house/ensure-house! s)
        (house/ensure-house-roof! s)
        (house/ensure-doors! s)
        (house/ensure-veranda! s)
        (house/ensure-bay-window! s)
        (house/ensure-bod! s)
        (house/ensure-bod-roof! s)
        (terrace/ensure-terrace! s)
        (terrace/ensure-terrace-roof! s))
      (let [hier (sync-site-hierarchy! s)
            orient (hierarchy/orient-site-root! s)
            _ (paint/paint-site! s)]
        (assoc hier :orient orient :house? (some? (mesh/object-by-name "site-house")))))))

(defn show-draw-structures!
  "Unhide draw structures; builds them (fast, no terrain) if missing."
  [s]
  (ensure-draw-structures! s)
  (when (facts-sufficient? s)
    (draw/unhide-draw-structures! s)))

(defn prepare-contour-draw!
  "Flat pad + fast massing for contour draw. No-ops when facts insufficient."
  [s]
  (if-not (facts-sufficient? s)
    {:status :empty-defaults}
    (let [structs (ensure-draw-structures! s)
          pad (lot/ensure-draw-pad! s)
          _ (draw/adopt-under-site-root! s (:name pad))
          mat (mesh/ensure-material! "site-mat-draw-pad" (:draw-pad mesh/material-colors))
          _ (mesh/assign-material! (:name pad) mat)
          terrain (draw/hide-terrain-for-draw!)
          hidden (draw/hide-draw-structures! s)
          frame (viewport/frame-lot-top! s)
          _ (viewport/show-material-colors!)]
      {:pad pad :structures structs :hidden hidden :terrain terrain :frame frame})))

(defn site-terrain-extra-features
  "Constructed-platform features the terrain must meet."
  [s]
  (-> (driveway/driveway-terrain-features s)
      (into (lot/road-stitch-features s))
      (conj {:kind :polygon
             :xy (house/bod-footprint-xy s)
             :rh00 (+ (:terrain/z0-rh00 s 0.0) (house/bod-floor-z s) -0.05)})
      (conj {:kind :clamp-polygon
             :xy (driveway/driveway-polygon-xy s)
             :rh00 (+ (:terrain/z0-rh00 s 0.0) (driveway/driveway-bod-z s) -0.05)})))

(defn ensure-trees-scene!
  "Partial tree rebuild, then sync under site-root."
  [s]
  (let [extras (site-terrain-extra-features s)
        trees (props/place-trees! s extras)
        hier (sync-site-hierarchy! s)]
    (merge {:trees trees} hier)))

(defn- ensure-lot-driveway!
  [s extras terrain-z]
  {:lot (lot/ensure-lot! s extras)
   :contours (lot/ensure-contours! s)
   :road (lot/ensure-road! s)
   :driveway (driveway/ensure-driveway! s)
   :driveway-wall (driveway/ensure-driveway-wall! s terrain-z)
   :fence (fence/ensure-fence! s terrain-z)})

(defn- ensure-house-family!
  [s]
  {:house-ground (house/ensure-house-ground! s)
   :house (house/ensure-house! s)
   :house-roof (house/ensure-house-roof! s)
   :doors (house/ensure-doors! s)
   :veranda (house/ensure-veranda! s)
   :bay (house/ensure-bay-window! s)
   :bod (house/ensure-bod! s)
   :bod-roof (house/ensure-bod-roof! s)})

(defn- ensure-terrace-family!
  [s]
  {:terrace (terrace/ensure-terrace! s)
   :terrace-roof (terrace/ensure-terrace-roof! s)
   :furniture (furniture/ensure-terrace-furniture! s)
   :sundial (sun/ensure-sundial! s)})

(defn- place-site-props!
  [s extras]
  {:trees (props/place-trees! s extras)
   :mailbox (props/place-mailbox! s extras)
   :cars (props/place-cars! s)})

(defn- finish-site-scene!
  [s]
  (merge (sync-site-hierarchy! s)
         {:orient (hierarchy/orient-site-root! s)
          :sun (sun/ensure-sun! s)
          :world (sun/ensure-world! s)
          :north (sun/ensure-north-marker! s)}))

(defn regenerate-terrain-cache!
  "Rebuild site-terrain from features and rewrite assets/terrain/site-terrain.glb.
  Requires sufficient facts. Cache file is optional and gitignored."
  [s]
  (when-not (facts-sufficient? s)
    (throw (python/Exception "regenerate-terrain-cache! needs :house/size-m and :lot/polygon-xy")))
  (let [s (lm/with-lm-road s)
        extras (site-terrain-extra-features s)]
    (lot/ensure-lot! s extras {:rebuild? true})))

(defn- ensure-empty-site!
  "site-root + sun/world/viewport defaults — no house/lot/road/terrain meshes."
  [s]
  (clear-site!)
  (hierarchy/ensure-site-root! s)
  (let [orient (hierarchy/orient-site-root! s)
        sun-r (sun/ensure-sun! s)
        world-r (sun/ensure-world! s)
        north-r (sun/ensure-north-marker! s)]
    (viewport/hide-relationship-lines!)
    {:status :empty-defaults
     :orient orient
     :sun sun-r
     :world world-r
     :north north-r}))

(defn ensure-site!
  "Rebuild site from facts.

  Insufficient facts (:house/size-m or :lot/polygon-xy nil):
    clear → site-root → sun/world/north + viewport defaults. No geometry meshes.

  Sufficient facts:
    full schematic rebuild; terrain loads from assets/terrain/site-terrain.glb when
    present; use regenerate-terrain-cache! after terrain feature changes."
  [s]
  (if-not (facts-sufficient? s)
    (ensure-empty-site! s)
    (do
      (clear-site!)
      (hierarchy/ensure-site-root! s)
      (let [s (lm/with-lm-road s)
            extras (site-terrain-extra-features s)
            features (into (lot/terrain-features s) extras)
            constructed (conj (vec extras)
                              {:kind :polygon
                               :xy (:terrain/plateau-xy s)
                               :rh00 (:terrain/plateau-rh00 s)})
            terrain-z (fn [p] (lot/terrain-z-with-lm s features constructed p))
            built (merge (ensure-lot-driveway! s extras terrain-z)
                         (ensure-house-family! s)
                         (ensure-terrace-family! s)
                         (place-site-props! s extras)
                         (finish-site-scene! s))]
        (paint/paint-site! s)
        (viewport/hide-relationship-lines!)
        (assoc built :status :full)))))

(comment
  ;; N-panel UI (View3D sidebar tab Yardcraft) — once per Blender session
  (do
    (viewport/show-rendered!)
    (ensure-site! site))

  (ui/register!)
  #_(ui/unregister!)
  #_(ui/reload!)

  (clear-site!)
  ;; Solar aim needs :site/lat-deg :site/lon-deg filled first:
  #_(set-sun-date! site "2026-06-21")
  #_(set-time-of-day! site "18:00")

  ;; Fly tour — populate tour-*-spec in site-fly then:
  #_(fly/ensure-fly-tour! site)

  :rcf)
