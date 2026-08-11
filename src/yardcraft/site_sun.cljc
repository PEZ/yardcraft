(ns yardcraft.site-sun
  "Sun aiming, world ambient, true-north marker, and roof sundial.

  Domain builders take facts map `s`. Aim helpers return updated `:site` for the
  caller to persist — this ns does not call persist-site!."
  (:require [basilisp.string :as string]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-house :as house])
  (:import bpy math mathutils
           [datetime :as datetime]
           [zoneinfo :as zoneinfo]))

(def ^:private utc-tz
  (.-utc (.-timezone datetime)))

(defn- hhmm-match
  "Match HH:MM groups or throw."
  [time-str]
  (or (re-matches #"(\\d{1,2}):(\\d{2})" time-str)
      (throw (python/Exception (str "Bad time-of-day (want HH:MM): " time-str)))))

(defn- assert-hhmm-range!
  "Validate hour/minute range; return [h mi]."
  [h mi time-str]
  (when (or (< h 0) (> h 23) (< mi 0) (> mi 59))
    (throw (python/Exception (str "Time out of range: " time-str))))
  [h mi])

(defn parse-hhmm
  "Parse \"H:MM\" or \"HH:MM\" → [hour minute]. Throws on bad input."
  [time-str]
  (let [m (hhmm-match time-str)]
    (assert-hhmm-range! (int (nth m 1)) (int (nth m 2)) time-str)))

(defn parse-iso-date
  "Parse \"YYYY-MM-DD\" → [year month day]."
  [date-str]
  (let [m (re-matches #"(\\d{4})-(\\d{2})-(\\d{2})" date-str)]
    (when-not m
      (throw (python/Exception (str "Bad date (want YYYY-MM-DD): " date-str))))
    [(int (nth m 1)) (int (nth m 2)) (int (nth m 3))]))

(defn local-sun-datetime
  "Timezone-aware local datetime from site :sun/date, :site/timezone, and HH:MM."
  [s time-str]
  (let [{:site/keys [timezone]
         :sun/keys [date]} s
        [y mo d] (parse-iso-date date)
        [h mi] (parse-hhmm time-str)
        tz (zoneinfo/ZoneInfo timezone)]
    (datetime/datetime y mo d h mi ** :tzinfo tz)))

(defn- julian-day
  "Julian day number + fractional day from UTC calendar fields."
  [y mo d h]
  (let [a (math/floor (/ (- 14 mo) 12))
        yy (+ y 4800 (- a))
        mm (+ mo (* 12 a) -3)
        jdn (+ d
               (math/floor (/ (+ (* 153 mm) 2) 5))
               (* 365 yy)
               (math/floor (/ yy 4))
               (- (math/floor (/ yy 100)))
               (math/floor (/ yy 400))
               -32045)]
    (+ jdn -0.5 (/ h 24.0))))

(defn- solar-ra-dec
  "Right ascension + declination (radians) from days since J2000."
  [n]
  (let [L (math/radians (mod (+ 280.46 (* 0.9856474 n)) 360.0))
        g (math/radians (mod (+ 357.528 (* 0.9856003 n)) 360.0))
        lambda (math/radians (mod (+ (math/degrees L)
                                     (* 1.915 (math/sin g))
                                     (* 0.020 (math/sin (* 2.0 g))))
                                  360.0))
        ep (math/radians (- 23.439 (* 0.0000004 n)))]
    {:ra (math/atan2 (* (math/cos ep) (math/sin lambda)) (math/cos lambda))
     :dec (math/asin (* (math/sin ep) (math/sin lambda)))}))

(defn- elev-az-from-ha
  "Elevation + azimuth (degrees) from hour angle, lat, and declination."
  [ha lat dec]
  (let [elev (math/asin (+ (* (math/sin lat) (math/sin dec))
                           (* (math/cos lat) (math/cos dec) (math/cos ha))))
        az (math/atan2 (- (math/sin ha))
                       (- (* (math/tan dec) (math/cos lat))
                          (* (math/sin lat) (math/cos ha))))]
    {:elevation-deg (math/degrees elev)
     :azimuth-deg (mod (math/degrees az) 360.0)}))

(defn sun-position
  "Solar elevation + azimuth (degrees). dt is timezone-aware (any tz).
  Azimuth: clockwise from true north (0=N, 90=E). Elevation: above horizon."
  [lat-deg lon-deg dt]
  (let [utc (.astimezone dt utc-tz)
        y (.-year utc)
        mo (.-month utc)
        d (.-day utc)
        h (+ (.-hour utc) (/ (.-minute utc) 60.0) (/ (.-second utc) 3600.0))
        jd (julian-day y mo d h)
        n (- jd 2451545.0)
        {:keys [ra dec]} (solar-ra-dec n)
        gmst (mod (+ 280.46061837 (* 360.98564736629 n)) 360.0)
        lst (math/radians (mod (+ gmst lon-deg) 360.0))
        ha (- lst ra)
        lat (math/radians lat-deg)]
    (elev-az-from-ha ha lat dec)))

(defn ensure-sun!
  "SUN light aimed by elevation + true azimuth (world +Y = true north). Object: site-sun.
  Blender Z rotation = 180° − azimuth (clockwise from north).
  Defaults elevation 45 / azimuth 180 / energy 4 when keys missing (empty-site)."
  [s]
  (mesh/unlink-and-remove! "site-sun")
  (let [elev (or (:sun/elevation-deg s) 45.0)
        az (or (:sun/azimuth-deg s) 180.0)
        e (math/radians (- 90.0 elev))
        a (math/radians (- 180.0 az))
        energy (if (neg? elev) 0.0 (or (:sun/energy s) 4.0))]
    (.light-add (.-object (.-ops bpy)) ** :type "SUN" :location #py [0.0 0.0 0.0])
    (let [sun (.-object (.-context bpy))]
      (set! (.-name sun) "site-sun")
      (set! (.-energy (.-data sun)) (double energy))
      (set! (.-angle (.-data sun)) 0.009)
      (set! (.-rotation-euler sun) #py [(double e) 0.0 (double a)])
      (.update (.-view-layer (.-context bpy)))
      {:name (.-name sun)
       :elevation-deg elev
       :azimuth-deg az})))

(defn aim-sun-at-clock
  "Aim site-sun for s's :sun/date + time-str.
  Returns map with :site (updated facts — caller persists) + aim fields.
  Side-effects: ensure-sun! on the aimed elevations."
  [s time-str]
  (let [{:site/keys [lat-deg lon-deg]
         :sun/keys [date]
         :as s} s
        dt (local-sun-datetime s time-str)
        {:keys [elevation-deg azimuth-deg]} (sun-position lat-deg lon-deg dt)
        s' (assoc s
                  :sun/time-of-day time-str
                  :sun/elevation-deg elevation-deg
                  :sun/azimuth-deg azimuth-deg)
        aimed (ensure-sun! s')]
    (merge aimed
           {:site s'
            :time-of-day time-str
            :date date
            :above-horizon? (pos? elevation-deg)})))

(defn ensure-world!
  "Soft sky Background on the scene world (ambient fill for Rendered shading).
  Defaults a soft sky when :world/color or :world/strength missing."
  [s]
  (let [color (or (:world/color s) [0.45 0.52 0.65])
        strength (or (:world/strength s) 0.15)
        [r g b] color
        world (.-world (.-scene (.-context bpy)))
        _ (set! (.-use-nodes world) true)
        bg (aget (.-nodes (.-node-tree world)) "Background")]
    (set! (-> bg .-inputs (aget "Color") .-default-value)
          #py [(double r) (double g) (double b) 1.0])
    (set! (-> bg .-inputs (aget "Strength") .-default-value) (double strength))
    {:color color :strength strength}))

(defn ensure-north-marker!
  "Arrow along true north (world +Y). Objects: site-north, site-north-head."
  [_s]
  (mesh/unlink-and-remove! "site-north")
  (mesh/unlink-and-remove! "site-north-head")
  (let [base-x 2.0
        base-y 2.0
        shaft-len 5.0
        mid-y (+ base-y (/ shaft-len 2.0))
        tip-y (+ base-y shaft-len)]
    (mesh/add-box! "site-north" [0.15 shaft-len 0.15] [base-x mid-y 0.4])
    (mesh/add-box! "site-north-head" [0.5 0.7 0.15] [base-x (+ tip-y 0.2) 0.4])
    {:name "site-north" :head "site-north-head"}))

(defn- true-north-local-xy
  "Unit XY in lot-local frame that maps to world +Y (true north) after site-root rotation."
  [s]
  (let [θ (math/radians (:site/north-offset-deg s))]
    [(- (math/sin θ)) (math/cos θ)]))

(defn- sundial-top-z
  "Z of dial face top: on house roof upper surface."
  [s]
  (+ (:house/floor-z s) (:house/schematic-height-m s) 0.12 0.025))

(defn- clear-sundial!
  []
  (run! mesh/unlink-and-remove!
        (filter #(string/starts-with? % "site-sundial")
                (mesh/site-object-names))))

(defn- hour-tick-placement
  "Local XY offset + size for solar hour h on the dial."
  [{:keys [h lat radius nx ny east-x east-y]}]
  (let [H (math/radians (* 15.0 (- h 12.0)))
        θ (math/atan2 (* (math/sin H) (math/sin lat)) (math/cos H))
        lx (+ (* nx (math/cos θ)) (* east-x (math/sin θ)))
        ly (+ (* ny (math/cos θ)) (* east-y (math/sin θ)))
        r0 (* radius 0.55)
        r1 (* radius 0.95)
        tx (* lx (/ (+ r0 r1) 2.0))
        ty (* ly (/ (+ r0 r1) 2.0))]
    {:h h :lx lx :ly ly :tx tx :ty ty :r0 r0 :r1 r1 :noon? (= h 12)}))

(defn- add-hour-tick!
  "Place one hour tick mesh; return object name."
  [{:keys [cx cy z]} {:keys [h lx ly tx ty r0 r1 noon?]}]
  (let [n (str "site-sundial-hour-" h)
        tick (mesh/add-box! n
                            [(if noon? 0.07 0.03) (- r1 r0) 0.04]
                            [(+ cx tx) (+ cy ty) (+ z 0.04)])
        tdir (.normalized (mathutils/Vector #py [(double lx) (double ly) 0.0]))]
    (set! (.-rotation-euler tick) (.to-euler (.to-track-quat tdir "Y" "Z")))
    n))

(defn- add-sundial-hour-ticks!
  "Hour ticks for solar hours 4–21. Opts: :s :cx :cy :z :radius :nx :ny."
  [{:keys [s cx cy z radius nx ny]}]
  (let [lat (math/radians (:site/lat-deg s))
        east-x ny
        east-y (- nx)
        dial {:cx cx :cy cy :z z}
        base {:lat lat :radius radius :nx nx :ny ny :east-x east-x :east-y east-y}]
    (mapv (fn [h]
            (->> (assoc base :h h)
                 hour-tick-placement
                 (add-hour-tick! dial)))
          (range 4 22))))

(defn- add-sundial-face!
  "Cylinder dial face at [cx cy z]; returns object."
  [cx cy z radius]
  (.primitive-cylinder-add (.-mesh (.-ops bpy)) **
                           :radius (double radius)
                           :depth 0.05
                           :location #py [(double cx) (double cy) (double z)]
                           :vertices 48)
  (let [face (.-object (.-context bpy))]
    (set! (.-name face) "site-sundial-face")
    face))

(defn- add-sundial-gnomon!
  "Polar-aligned gnomon from dial center toward true north at latitude."
  [{:keys [cx cy z radius nx ny lat]}]
  (let [style-len (* radius 0.85)
        dx (* nx (math/cos lat))
        dy (* ny (math/cos lat))
        dz (math/sin lat)
        gnomon (mesh/add-box! "site-sundial-gnomon"
                              [0.04 0.04 style-len]
                              [(+ cx (* dx (/ style-len 2.0)))
                               (+ cy (* dy (/ style-len 2.0)))
                               (+ z (* dz (/ style-len 2.0)))])
        dir (.normalized (mathutils/Vector #py [(double dx) (double dy) (double dz)]))]
    (set! (.-rotation-euler gnomon) (.to-euler (.to-track-quat dir "Z" "Y")))
    gnomon))

(defn ensure-sundial!
  "Horizontal sundial on house roof. Gnomon → true north at latitude.
  Shadow reads solar time (XII = solar noon ≈ 13:00 civil with summer DST)."
  [s]
  (clear-sundial!)
  (let [radius (:sundial/radius-m s)
        [cx cy] (house/house-center-xy s)
        z (sundial-top-z s)
        [nx ny] (true-north-local-xy s)
        lat (math/radians (:site/lat-deg s))
        _ (add-sundial-face! cx cy z radius)
        _ (add-sundial-gnomon! {:cx cx :cy cy :z z :radius radius :nx nx :ny ny :lat lat})
        hour-names (add-sundial-hour-ticks! {:s s :cx cx :cy cy :z z
                                             :radius radius :nx nx :ny ny})]
    {:face "site-sundial-face"
     :gnomon "site-sundial-gnomon"
     :hours hour-names}))
