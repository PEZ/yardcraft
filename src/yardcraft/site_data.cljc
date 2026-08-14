(ns yardcraft.site-data
  "Canonical site facts — empty template.

  House-NW: origin at NW house corner; +X along the house; +Y toward the access road.
  World +Y = true north via site-root Z-rotation by −:site/north-offset-deg.
  Z=0 = constructed house platform (= RH00 via :terrain/z0-rh00 when set); pad/floor
  detail lives on keys like :house/floor-z and :…/note entries in this map — not
  invented elsewhere. Lot outline: :lot/polygon-xy.

  Prefer :…/note keys on facts for source/provenance prose.
  Fill keys as you measure and confirm. Do not invent measurements.
  Keys below document the shape builders expect; values start nil / empty.")

;;; --- Identity / geo / sun (fill when known) ---

(def site-meta-facts
  "Identity, geo, sun, world. Leave lat/lon nil until you choose a CRS/datum story."
  {:site/name nil
   :site/address nil
   :site/area-m2 nil
   :site/project "Explore yard / patio / parking options"
   :site/timezone "UTC"
   :site/north-offset-deg nil
   :site/north-offset-note "Degrees east of true north that house-local +Y points. site-root Z-rotates by −this."
   :site/lat-deg nil
   :site/lon-deg nil
   :sun/date "2026-06-21"
   :sun/time-of-day "13:00"
   ;; Viewport-safe defaults for empty-site (no solar calc without lat/lon)
   :sun/elevation-deg 45.0
   :sun/azimuth-deg 180.0
   :sun/energy 4.0
   :world/color [0.45 0.52 0.65]
   :world/strength 0.15
   :sundial/radius-m nil})

(def site-access-facts
  "Access streets and neighbours — fill when known."
  {:access/road nil
   :access/direction nil})

(def site-house-massing-facts
  "House / outbuilding massing. :house/size-m required (with lot polygon) for full ensure-site!."
  {:house/size-m nil
   :house/schematic-height-m nil
   :house/floor-z nil
   :house/pad-height-m nil
   :house/roof-overhang-m nil
   :house/roof-style nil
   :bod/size-m nil
   :bod/height-m nil
   :bod/gap-to-house-m nil
   :bay/east-offset-m nil
   :bay/width-m nil
   :bay/depth-m nil})

(def site-openings-facts
  "Doors and entrance veranda."
  {:door/height-m nil
   :door/width-m nil
   :door/thickness-m nil
   :door/west nil
   :door/south nil
   :door/north nil
   :veranda/size-m nil})

(def site-lot-facts
  "Lot polygon and road strip. :lot/polygon-xy required (with house size) for full ensure-site!."
  {:lot/polygon-xy nil
   :lot/sides-m nil
   :lot/interior-angles-deg nil
   :lot/road-setback-m nil
   :road/width-m nil
   :road/inner-edge-xy nil
   :road/z-profile nil
   :road/stitch-to-x nil})

(def site-terrace-facts
  "Terrace, stairs, railings, canopy — fill when designing."
  {:terrace/depth-m nil
   :terrace/east-extend-m nil
   :terrace/slab-thickness-m nil
   :terrace/roofed-depth-m nil
   :stairs/west nil
   :stairs/east nil
   :railing/height-m nil})

(def site-driveway-facts
  "Driveway polygon and seating."
  {:driveway/polygon-xy nil
   :driveway/z-break-y nil
   :driveway/car-clearance-m 0.04})

(def site-fence-facts
  "Road-frontage fence (schematic)."
  {:fence/height-m nil
   :fence/gate-width-m nil})

(def site-furniture-facts
  "Schematic terrace furniture for fit-checking (not product geometry).
  Leave nil / omit groups until you place furniture."
  {:furniture/leg-size-m 0.045
   :furniture/leg-inset-m 0.05
   :furniture/dining nil
   :furniture/cafe nil
   :furniture/lounger nil
   :furniture/bbq nil})

(def site-props-facts
  "Optional glTF props. Download via yardcraft-assets skill + ATTRIBUTION.md.
  Empty plantings / cars = nothing placed. Missing files are skipped (never crash)."
  {:trees/plantings []
   :mailbox/file nil
   :mailbox/xy nil
   :mailbox/height-m nil
   ;; Example only — see place-cars! docstring
   :cars/placements []})

(def site-sketch-facts
  "Light-table sketch overlay specs. Point :image-path at your source-images/ or
  recipe/example-source-images/ while learning the workflow."
  {:sketch/specs {:elevation-lines {:object-name "draft-elevation-sketch"
                                    :image-path "recipe/example-source-images/elevation-lines.jpg"
                                    :px-size nil
                                    :corner-px nil
                                    :note "Example overlay — replace with your traced elevation sketch + measured corner-px."}
                  :lot-road {:object-name "draft-lot-road"
                             :image-path "recipe/example-source-images/lot-road.jpg"
                             :px-size nil
                             :corner-px nil
                             :note "Example lot/road sketch."}
                  :house-bod-terrace {:object-name "draft-house-bod-terrace"
                                      :image-path "recipe/example-source-images/house-shed-terrace.jpg"
                                      :px-size nil
                                      :corner-px nil
                                      :note "Example house/bod/terrace sketch."}}})

(def site-terrain-facts
  "Terrain datum, plateau, contours. Vertical datum is yours to declare (country skill)."
  {:terrain/z0-rh00 nil
   :terrain/grid-step-m 1.0
   :terrain/corner-rh00 nil
   :terrain/plateau-rh00 nil
   :terrain/plateau-xy nil
   :terrain/contours nil})

(defmacro ^:private assemble-site
  "Combine domain fact maps into the canonical site map at compile time."
  []
  (merge @(resolve 'site-meta-facts)
         @(resolve 'site-access-facts)
         @(resolve 'site-house-massing-facts)
         @(resolve 'site-openings-facts)
         @(resolve 'site-lot-facts)
         @(resolve 'site-terrace-facts)
         @(resolve 'site-furniture-facts)
         @(resolve 'site-driveway-facts)
         @(resolve 'site-fence-facts)
         @(resolve 'site-props-facts)
         @(resolve 'site-sketch-facts)
         @(resolve 'site-terrain-facts)))

(defn site-from-facts
  "Merge current site-*-facts defs into a site map."
  []
  (merge site-meta-facts
         site-access-facts
         site-house-massing-facts
         site-openings-facts
         site-lot-facts
         site-terrace-facts
         site-furniture-facts
         site-driveway-facts
         site-fence-facts
         site-props-facts
         site-sketch-facts
         site-terrain-facts))

(def site
  "Trusted survey / living measurements. Domains read this map; do not invent values."
  (assemble-site))

(defn persist-site!
  "Write facts map to yardcraft.site-data/site (Var root + Python module attr)."
  [s']
  (alter-var-root #'site (fn [_] s'))
  (set! (.-site (.-module (find-ns 'yardcraft.site-data))) s')
  (when-let [consumer (find-ns 'yardcraft.site)]
    (set! (.-site (.-module consumer)) s'))
  s')

(defn reset-site-from-facts!
  "persist-site! from site-from-facts (session only; does not re-read disk)."
  []
  (persist-site! (site-from-facts)))
