---
name: sweden-lantmateriet-min-karta
description: >-
  Lantmäteriet Min Karta APIs and elevation reconciliation for Swedish sites.
  Use when the user mentions Min Karta, Lantmäteriet, LM höjd, RH00 sampling,
  lm_height_grid, site-lm, terrain-z-with-lm, SWEREF99, reconciling DEM with
  light-table facts, road Z profile, or overriding LM terrain in the Blender model.
---

# Sweden — Lantmäteriet Min Karta (sample → reconcile → override)

Use [Min Karta](https://minkarta.lantmateriet.se/) and its höjd APIs as an elevation **source** for natural slope and road-profile shape — then reconcile with constructed / light-table ground truth before changing terrain in Blender.

**CRS / datum (declare and stick to them):**

| Frame | Definition |
|---|---|
| **SWEREF99 TM** | LM API east / north (EPSG:3006) |
| **RH00** | Height system returned by the höjd API (`:hojd` in metres) |
| **house-NW** | Local model frame: origin at NW house corner; +X along house; +Y toward access road; **Z = 0** = constructed platform RH00 stored as `:terrain/z0-rh00` |

Country skills for other nations should likewise declare CRS + vertical datum up front.

## Prerequisites

Load before using this skill:

1. **`basilisp`** — dialect / Python interop
2. **`basilisp-blender`** — nREPL-in-Blender, `bpy`, Yardcraft session bootstrap
3. **`babashka`** — host-side HTTP (`bb` session); prefer over curl/python
4. **`clojure`** — shared Clojure conventions (structural edits, REPL-first)

Related: **`yardcraft-light-table`** — hand-traced contours and driveway edges that outrank raw LM where validated.

After connect: confirm `user/init!` added `src/` to `sys.path`. Tooling: `src/yardcraft/site_lm.cljc` (`yardcraft.site-lm`); terrain blend in `yardcraft.site-lot`; orchestration in `yardcraft.site`. Facts: `yardcraft.site-data` / `site.md`. **Do not invent site measurements.**

## Division of labor

| Role | Owns |
|---|---|
| Agent | Spot höjd samples (bb REPL); compare RH00 to constructed Z; REPL terrain experiments visible in Blender; promote to `site-data` / `site-lm` / `site-lot` only after human confirms |
| Human | Ground-truth judgment when LM contradicts on-site observation |

```
λ lm_loop.
  bb_sample ∨ REPL_lm-z-at → compare_to_constructed → ask_human_when_conflict
  | REPL_ensure-site!_partial → viewport_check → promote_when_happy
  | LM ≡ source | site_data ∧ light_table ≡ ground_truth_near_construction
```

## Site identity (consumer-filled)

Do **not** hardcode a private lot id in the template. When working a Swedish parcel:

| Field | How to obtain |
|---|---|
| Property UUID | From Min Karta property search / selection (cadastral UUID string) |
| Parcel polygon | Property geometry API (SWEREF99 TM) |
| App | OpenLayers + React at `https://minkarta.lantmateriet.se/` |

The selected lot in the UI does **not** carry DEM — heights come from the positionsinformation / höjd APIs.

## APIs (host `https://minkarta.lantmateriet.se`)

All paths relative to that host. Point höjd works **unauthenticated** from Babashka.

| API | Method | Path / params |
|---|---|---|
| Point height (RH00) | `GET` | `/api/positionsinformation/positionsinformation/v1/hojd?transactionId=<uuid>&east=<E>&north=<N>` → `:hojd` (m, RH00) |
| Property geometry | `GET` | `/api/searchservice/fastighetsgeometri/v1` + property-UUID query param → SWEREF99 TM polygon (`enhetsutbredning` / `yta`) |
| Property info | `GET` | `/api/searchservice/fastighetsinfo/v1` + same property-UUID query param |
| Elevation profile | `POST` | `/api/hojdprofil/hojdprofil/v1` — profile along a line when dense sampling along a segment is needed |

### Babashka spot sample

```clojure
(require '[babashka.http-client :as http]
         '[cheshire.core :as json])

(def base "https://minkarta.lantmateriet.se")
;; Pass the consumer's lot UUID — never commit a private home id into the template.
(def lot-id "<cadastral-property-uuid>")

(defn hojd-at [e n]
  (let [url (str base "/api/positionsinformation/positionsinformation/v1/hojd"
                 "?transactionId=" (random-uuid) "&east=" e "&north=" n)]
    (:hojd (json/parse-string (:body (http/get url)) true))))

;; Example: (hojd-at <east> <north>)  ; SWEREF99 TM metres
```

Browser exploration with **Epupp** against Min Karta is fine for discovery; for durable host work prefer **`bb`**.

### Dense grid fetch

Prefer a parameterized bb script under `recipe/scripts/` (see `fetch_height_grid.clj`) that samples höjd on a SWEREF grid inside **your** lot polygon (writes e.g. `out/lm-height-grid.edn`). Promote/cache a model grid under `src/yardcraft/data/` only for the consumer project — house-NW `{:xy [x y] :rh00 …}` plus optional `:road-z-profile`. Re-fetch only when coverage or fit changes; prefer point samples for spot checks.

## Coordinate frames

| Frame | Definition |
|---|---|
| **SWEREF99 TM** | LM API east / north (EPSG:3006) |
| **house-NW** | Origin = NW corner of house; +X east along house; +Y toward access road; **Z = 0** = constructed platform RH00 (`:terrain/z0-rh00`) |

Fit SWEREF ↔ house-NW from known control points (lot corners, road edge). Store the fit you verify; do not invent control points.

Model Z from RH00: `(- rh00 (:terrain/z0-rh00 site))` — see `lot/rh00->z` when present.

## Runtime (`yardcraft.site-lm`)

```clojure
(require '[yardcraft.site-lm :as lm])

(lm/load-grid!)           ; load cached grid EDN when present
(lm/lm-rh00-at [-5.0 0.0]) ; IDW over nearest samples → RH00
(lm/lm-z-at site [-5.0 0.0]) ; → model Z
(lm/road-z-profile)       ; when a road profile was cached
(lm/with-lm-road site)      ; overlay :road/z-profile from grid EDN
```

`ensure-site!` and draw staging may call `lm/with-lm-road` so the road strip uses an LM-derived profile unless facts override. Confirm against live orchestration.

## Reconciliation policy

LM is a **source**, not automatic truth over human / site facts.

### Trust constructed / light-table story

- House platform / plateau (`:terrain/plateau-xy`, `:terrain/z0-rh00`)
- Terrace deck, outbuilding pads, driveway as **constructed** surfaces
- Hand-traced contours / driveway edges from light table after human validation (`yardcraft-light-table`)

### LM is useful for

- Natural bank / slope shape away from constructed pads
- Road profile **shape** along the access road
- Cross-checks via höjd API at explicit points

### Known failure modes

- LM absolute heights can disagree with on-site **relative** relationships
- Mid-road API vs lot-edge samples differ slightly
- Vegetation, curb, vehicles, or DEM artifacts bias point heights

### When LM contradicts observation

**Ask the human.** Do not silently average away the observation.

## Override patterns (typical)

| Mechanism | Behavior |
|---|---|
| Selective blend (`lot/terrain-z-with-lm` or similar) | Trust near plateau / constructed features; `lm/lm-z-at` elsewhere |
| Road Z (`lm/with-lm-road`) | Overlays `:road/z-profile` from grid EDN; `site-data` may keep a coarse fallback |
| Partial rebuilds | `yardcraft.site/sync-site-hierarchy!` after `ensure-lot!`, `ensure-terrace!`, etc. |

Confirm exact fn names in the live `site_lot` / `site_lm` sources for your checkout.

## Workflow

1. **Query before mutate** — sample höjd at house-NW points of interest (convert to SWEREF if calling API directly).
2. **Compare** — LM RH00 vs constructed RH00 (`+ model-z :terrain/z0-rh00`) or `lm/lm-rh00-at` vs feature RH00 in facts.
3. **REPL experiment** — tweak blend / extras; `(ensure-site! site)` or partial `ensure-lot!`; check viewport.
4. **Human feedback** — especially when LM conflicts with walked observation.
5. **Promote** — durable changes → `site_data.cljc`, `site_lm.cljc`, `site_lot.cljc`, `site_driveway.cljc` as appropriate; re-fetch grid only when needed.

## Invariants

- **Do not invent site measurements** — extend `yardcraft.site-data` / `site.md` when confirmed.
- **LM ≠ survey truth** near constructed pads; blend and overrides encode human judgment.
- **Prefer point samples** over full grid re-fetch for spot checks.
- **`sync-site-hierarchy!` after partial rebuilds** — new meshes inherit `site-root` rotation.
- **REPL → viewport → promote** — same gate as other Yardcraft skills.
- **No private lot identity in the template** — pass the cadastral property UUID and control points as consumer inputs.
