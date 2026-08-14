(ns yardcraft.site-suggestions
  "Design suggestions: durable EDN patches, domain-scoped show/switch, promote plans.
  Session overlays via register-suggestion! (no EDN write).
  set-base! adopts into session site (persist-site!); file reset via
  (require '[yardcraft.site-data :as data :refer [site]] :reload-all) or
  reload-file-base! (also clears active) then ensure-site!.

  Suggestions dir `src/yardcraft/suggestions/` starts empty — drop `*.edn` patches
  there when authoring; create the directory with the first suggestion file."
  (:require [basilisp.string :as string]
            [basilisp.set :as set]
            [basilisp.edn :as edn]
            [yardcraft.site-data :as data]
            [yardcraft.site-mesh :as mesh]
            [yardcraft.site-furniture :as furniture]
            [yardcraft.site-terrace :as terrace]
            [yardcraft.site-paint :as paint]
            [yardcraft.site-hierarchy :as hierarchy])
  (:import os
           [os.path :as path]))

(def ^:private suggestions-dir "src/yardcraft/suggestions")

(def ^:private survey-ns-denylist
  #{"lot" "terrain" "house" "road" "site" "access" "neighbours" "neighbour"
    "sketch" "sun" "world" "sundial" "bay" "bod" "door" "veranda"})

(def ^:private active-attr "_yardcraft_active_suggestion")

(defn- active-state
  "Active suggestion bookkeeping surviving Basilisp ns reload (stored on bpy)."
  []
  (let [b (python/__import__ "bpy")]
    (when (hasattr b active-attr)
      (getattr b active-attr))))

(defn- set-active!
  [v]
  (setattr (python/__import__ "bpy") active-attr v)
  v)

(def ^:private session-attr "_yardcraft_session_suggestions")

(defn- session-suggestions
  "In-memory suggestion map surviving Basilisp ns reload (stored on bpy)."
  []
  (let [b (python/__import__ "bpy")]
    (if (hasattr b session-attr)
      (or (getattr b session-attr) {})
      {})))

(defn- set-session-suggestions!
  [m]
  (setattr (python/__import__ "bpy") session-attr m)
  m)

(defn deep-merge
  "Map-only recursive merge; non-maps (incl. vectors) replace."
  [a b]
  (if-not (and (map? a) (map? b))
    (or b a)
    (reduce-kv
     (fn [acc k v]
       (assoc acc k
              (if (and (map? v) (map? (get a k)))
                (deep-merge (get a k) v)
                v)))
     a
     b)))

(defn- survey-ns
  [k]
  (or (namespace k) (first (string/split (name k) #"/" 2))))

(defn- id->filename
  [id]
  (str (string/replace (name id) #"-" "_") ".edn"))

(defn- suggestions-root
  []
  (path/join (os/getcwd) suggestions-dir))

(defn- suggestion-path
  [id]
  (path/join (suggestions-root) (id->filename id)))

(defn- read-edn-file
  [p]
  (edn/read-string
   (with-open [f (python/open p "r" ** :encoding "utf-8")]
     (.read f))))

(defn- assert-suggestion-id!
  [id s path]
  (when (not= (:suggestion/id s) id)
    (throw (ex-info "Suggestion id mismatch"
                    {:expected id :found (:suggestion/id s) :path path}))))

(defn load-suggestion
  "Load by id: session registry first, else `src/yardcraft/suggestions/<id>.edn`."
  [id]
  (if-let [s (get (session-suggestions) id)]
    s
    (let [p (suggestion-path id)]
      (when-not (path/isfile p)
        (throw (ex-info "Suggestion not found (session or file)"
                        {:id id :path p})))
      (let [s (read-edn-file p)]
        (assert-suggestion-id! id s p)
        s))))

(defn- list-file-suggestions
  []
  (let [dir (suggestions-root)]
    (->> (os/listdir dir)
         (filter #(string/ends-with? % ".edn"))
         (map (fn [file]
                (let [{:suggestion/keys [id title domains]}
                      (read-edn-file (path/join dir file))]
                  {:id id :title title :domains domains
                   :source :file :file file})))
         vec)))

(defn- list-session-suggestion-summaries
  []
  (mapv (fn [[id {:suggestion/keys [title domains]}]]
          {:id id :title title :domains domains :source :session})
        (session-suggestions)))

(defn list-suggestions
  "Summaries for session-registered suggestions and `src/yardcraft/suggestions/*.edn`.
  Session entries win when the same id exists as a file."
  []
  (let [session (list-session-suggestion-summaries)
        session-ids (set (map :id session))
        files (->> (list-file-suggestions)
                   (remove #(contains? session-ids (:id %))))]
    (->> (concat session files)
         (sort-by :id)
         vec)))

(defn active-id
  "Current suggestion id, or nil when showing base only."
  []
  (:id (active-state)))

(def ^:private known-domains #{:furniture :terrace :full :demo})

(defn- assert-patch-allowed!
  [{:suggestion/keys [patch allows-survey-keys?]}]
  (when-not allows-survey-keys?
    (let [bad (filter #(contains? survey-ns-denylist (survey-ns %)) (keys patch))]
      (when (seq bad)
        (throw (ex-info "Survey keys denied in suggestion patch" {:keys bad}))))))

(defn- assert-known-domains!
  [domains]
  (let [unknown (set/difference domains known-domains)]
    (when (seq unknown)
      (throw (ex-info "Unknown suggestion domain" {:domains unknown})))))

(defn- assert-suggestion-shape!
  [{:suggestion/keys [id title domains patch] :as s}]
  (doseq [[ok? msg] [[(keyword? id) "Suggestion requires :suggestion/id keyword"]
                     [(string? title) "Suggestion requires :suggestion/title string"]
                     [(set? domains) "Suggestion requires :suggestion/domains set"]
                     [(map? patch) "Suggestion requires :suggestion/patch map"]]]
    (when-not ok?
      (throw (ex-info msg {:suggestion s :id id}))))
  (assert-known-domains! domains)
  (assert-patch-allowed! s)
  s)

(defn register-suggestion!
  "Register or replace an in-memory suggestion. Does not write EDN.
  Registry survives Basilisp ns reload (stored on bpy).
  Re-register the Yardcraft UI (`yardcraft.site-ui/register!`) so the enum picks it up."
  [s]
  (let [s (assert-suggestion-shape! s)
        id (:suggestion/id s)]
    (set-session-suggestions! (assoc (session-suggestions) id s))
    id))

(defn unregister-suggestion!
  "Remove a session-registered suggestion (no-op if absent)."
  [id]
  (set-session-suggestions! (dissoc (session-suggestions) id))
  id)

(defn clear-session-suggestions!
  "Drop all session-registered suggestions."
  []
  (set-session-suggestions! {})
  :cleared)

(defn- materialize-suggestion
  [base {:suggestion/keys [patch]}]
  (deep-merge base (or patch {})))

(defn effective-site
  "Facts with a suggestion patch applied.
  1-arity: active suggestion after show!, or base when none active.
  2-arity: always apply suggestion id (does not require show! / active-id).
  Use for quote-plan SVG and any read that should match a design option."
  ([base]
   (if-let [id (active-id)]
     (effective-site base id)
     base))
  ([base id]
   (materialize-suggestion base (load-suggestion id))))

(def ^:private terrace-teardown
  [[:exact "site-terrace"]
   [:exact "site-terrace-south"]
   [:exact "site-terrace-bod-south"]
   [:exact "site-terrace-west"]
   [:exact "site-terrace-west-bridge"]
   [:exact "site-terrace-east-low"]
   [:exact "site-terrace-south-low"]
   [:prefix "site-terrace-south-"]
   [:prefix "site-stair-west"]
   [:prefix "site-stair-east"]
   [:prefix "site-stair-sw"]
   [:prefix "site-stair-se"]
   [:prefix "site-railing"]
   [:prefix "site-terrace-post"]
   [:exact "site-terrace-roof"]
   [:exact "site-terrace-roof-covering"]
   [:prefix "site-terrace-roof-frame"]
   [:prefix "site-terrace-roof-pole"]])

(defn- site-clear!
  []
  ((ns-resolve 'yardcraft.site 'clear-site!)))

(defn- site-ensure!
  [s]
  ((ns-resolve 'yardcraft.site 'ensure-site!) s))

(def ^:private domain-registry
  {:furniture {:teardown [[:prefix "site-furniture"]]}
   :terrace {:teardown terrace-teardown}
   :full {:teardown []}
   :demo {:teardown [[:prefix "site-demo-stair"]
                     [:prefix "site-railing-demo"]
                     [:exact "site-demo-pedestal"]
                     [:prefix "site-sundial"]]}})

(defn- matcher-object-names
  [[kind val]]
  (cond
    (= kind :exact) (if (mesh/object-by-name val) [val] [])
    (= kind :prefix) (->> (mesh/site-object-names)
                          (filter #(string/starts-with? % val))
                          vec)
    :else []))

(defn- teardown-matchers!
  [matchers]
  (run! mesh/unlink-and-remove!
        (distinct (mapcat matcher-object-names matchers))))

(defn- teardown-domain!
  [domain-key]
  (if (= domain-key :full)
    (site-clear!)
    (when-let [{:keys [teardown]} (get domain-registry domain-key)]
      (teardown-matchers! teardown))))

(defn- rebuild-domain!
  [domain-key s]
  (cond
    (= domain-key :furniture) (furniture/ensure-terrace-furniture! s)
    (= domain-key :terrace)
    (do (terrace/ensure-terrace! s)
        (terrace/ensure-terrace-roof! s))
    (= domain-key :full) (site-ensure! s)
    (= domain-key :demo)
    ((ns-resolve 'yardcraft.site-demo 'ensure-demo-overlays!) s)
    :else (throw (ex-info "Unregistered domain" {:domain domain-key}))))

(defn- rebuild-domains!
  [domains domains-B effective-B base]
  (let [ordered (into (vec (remove #{:full} domains)) [:full])]
    (doseq [d ordered
            :when (contains? domains d)]
      (rebuild-domain! d (if (contains? domains-B d) effective-B base)))))

(defn- switch-domains!
  [base domains-A domains-B effective-B]
  (let [U (set/union domains-A domains-B)]
    (assert-known-domains! U)
    (run! teardown-domain! U)
    (rebuild-domains! U domains-B effective-B base)
    U))

(defn- demo-active? []
  ((ns-resolve 'yardcraft.site-demo 'demo-active?)))

(defn- adopt-demo-facts! [s]
  ((ns-resolve 'yardcraft.site-demo 'adopt-demo-facts!) s))

(defn- refresh-demo-appearance! [s]
  ((ns-resolve 'yardcraft.site-demo 'refresh-demo-appearance!) s))

(defn- sync-scene!
  "Adopt rebuilt meshes under site-root (north-offset). Avoids site/sync after reload breakage."
  [s]
  (if (demo-active?)
    (refresh-demo-appearance! s)
    {:hierarchy (hierarchy/organize-site-hierarchy! s)
     :root (hierarchy/parent-under-site-root! s)}))

(defn show!
  "Apply suggestion id on base: union teardown, rebuild B from effective, A\\B from base.
  Never calls persist-site!."
  [base id]
  (let [B (load-suggestion id)
        _ (assert-patch-allowed! B)
        {:suggestion/keys [domains]} B
        domains-B (or domains #{})
        _ (assert-known-domains! domains-B)
        domains-A (or (:domains (active-state)) #{})
        effective-B (materialize-suggestion base B)
        U (switch-domains! base domains-A domains-B effective-B)]
    (if (demo-active?)
      (do (adopt-demo-facts! effective-B)
          (refresh-demo-appearance! effective-B))
      (do (sync-scene! effective-B)
          (paint/paint-site! effective-B)))
    (set-active! {:id id :domains domains-B})
    {:id id :domains domains-B :union U}))

(defn- demo-base-facts [base]
  (-> base
      (assoc :demo/a-back-stair? false)
      (dissoc :demo/pedestal-xy)))

(defn show-base!
  "Teardown active domains and rebuild them from pure base; clear active state."
  [base]
  (if-let [{:keys [domains]} (active-state)]
    (let [base' (if (demo-active?) (demo-base-facts base) base)
          U (switch-domains! base' domains #{} base')]
      (if (demo-active?)
        (do (adopt-demo-facts! base')
            (refresh-demo-appearance! base'))
        (do (sync-scene! base')
            (paint/paint-site! base')))
      (set-active! nil)
      {:cleared true :domains domains :union U})
    {:cleared false}))

(defn clear-active!
  "Clear suggestion bookkeeping without rebuilding.
  Use after (ensure-site! site) when site was reloaded from file."
  []
  (set-active! nil)
  {:active nil})

(defn set-base!
  "Adopt suggestion as session site (persist-site! only — not the file).
  [base] uses the active suggestion; [base id] adopts that id.
  Shows the suggestion first if it is not already active so the scene matches."
  ([base]
   (if-let [id (active-id)]
     (set-base! base id)
     (throw (ex-info "No active suggestion; pass an id" {:op :set-base!}))))
  ([base id]
   (when (not= (active-id) id)
     (show! base id))
   (let [B (load-suggestion id)
         _ (assert-patch-allowed! B)
         effective (materialize-suggestion base B)]
     (data/persist-site! effective)
     (set-active! nil)
     {:adopted id :site effective})))

(defn reload-file-base!
  "Reset session site from site-*-facts (file defs) and clear active.
  Trailing :reload-all on yardcraft.site-data also restores site; this is convenience
  that also clears active suggestion state.
  Does not rebuild the scene — call (site/ensure-site! site) after."
  []
  (let [file-site (data/reset-site-from-facts!)]
    (set-active! nil)
    {:site file-site :from-file true}))

(def ^:private promote-ns->facts-var
  {"terrace" 'site-terrace-facts
   "stairs" 'site-terrace-facts
   "railing" 'site-terrace-facts
   "furniture" 'site-furniture-facts
   "driveway" 'site-driveway-facts
   "fence" 'site-fence-facts
   "trees" 'site-props-facts
   "mailbox" 'site-props-facts})

(defn- facts-var-for-key
  [k]
  (or (get promote-ns->facts-var (survey-ns k))
      (throw (ex-info "No promote target for patch key" {:key k}))))

(defn- current-facts-for-var
  [facts-sym]
  (var-get (ns-resolve 'yardcraft.site-data facts-sym)))

(defn- group-patch-by-facts-var
  [patch]
  (reduce-kv
   (fn [acc k v]
     (update acc (facts-var-for-key k) assoc k v))
   {}
   patch))

(defn- promote-changes
  [patch]
  (mapv (fn [[k v]]
          {:facts-var (facts-var-for-key k)
           :key k
           :value v})
        patch))

(defn- promote-replacement
  [facts-sym patch-subset]
  (deep-merge (current-facts-for-var facts-sym) patch-subset))

(defn promote-plan
  "Read-only promote preview for adopting a suggestion into site_data.cljc.
  Returns {:suggestion/id … :changes [{:facts-var 'site-furniture-facts :key … :value …} …]
  :replacements {'site-furniture-facts {…full merged map…} …}}.
  :replacements values are meant to replace the corresponding def body in site_data.cljc."
  [id]
  (let [s (load-suggestion id)
        _ (assert-patch-allowed! s)
        {:suggestion/keys [patch]} s
        by-var (group-patch-by-facts-var patch)]
    {:suggestion/id id
     :changes (promote-changes patch)
     :replacements (into {}
                         (map (fn [[facts-sym patch-subset]]
                                [facts-sym (promote-replacement facts-sym patch-subset)])
                              by-var))}))
