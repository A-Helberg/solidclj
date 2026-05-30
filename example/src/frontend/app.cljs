(ns frontend.app
  (:require-macros [solidclj.hiccup-macros :refer [h]])
  (:require
   ["solid-js" :refer [createSignal createEffect onCleanup]]
   ["recharts" :refer [LineChart Line XAxis YAxis CartesianGrid Tooltip Legend]]
   [promesa.core :as p]
   [api.clock :as clock]
   [solidrpc.call.solidjs :as solidrpc]
   [solidrpc.transit :as transit]
   [solidclj.react :as react]
   [solidclj.react.reagent :as react.reagent]
   [frontend.reagent-counter :as reagent-counter]))

;; ---------------------------------------------------------------------------
;; Shared styles
;; ---------------------------------------------------------------------------

(def ^:private btn-style
  "margin-right: 0.5rem; padding: 0.3rem 0.6rem;
   background: #2563eb; color: white; border: none;
   border-radius: 0.375rem; cursor: pointer; font-size: 0.9rem")

(def ^:private h2-style
  "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem")

;; ---------------------------------------------------------------------------
;; Render page — atom counters, [:for], React bridge, Reagent bridge
;; ---------------------------------------------------------------------------

(defonce counter (atom 0))

(defn atom-counter []
  [:div {:style "margin: 1rem 0"}
   [:div {:style "font-family: monospace; font-size: 1.2rem"}
    "live: " counter
    " (snapshot at mount: " @counter ")"]
   [:button {:onClick #(swap! counter inc)
             :style   "margin-top: 0.5rem; padding: 0.4rem 0.8rem;
                       background: #16a34a; color: white; border: none;
                       border-radius: 0.375rem; cursor: pointer; font-size: 1rem"}
    "increment"]])

(defn atom-counter2 []
  (let [counter' (atom -10)]
    [:div {:style "margin: 1rem 0"}
     [:div {:style "font-family: monospace; font-size: 1.2rem"}
      "live: " counter'
      " (snapshot at mount: " @counter' ")"]
     [:button {:onClick #(swap! counter' inc)
               :style   "margin-top: 0.5rem; padding: 0.4rem 0.8rem;
                       background: #16a34a; color: white; border: none;
                       border-radius: 0.375rem; cursor: pointer; font-size: 1rem"}
      "increment"]]))

(defonce todos        (atom [{:id 1 :text "buy milk"}
                             {:id 2 :text "walk dog"}
                             {:id 3 :text "ship feature"}]))
(defonce next-todo-id (atom 4))

(defn for-widget []
  [:div {:style "margin: 1rem 0"}
   [:div {:style "margin-bottom: 0.5rem"}
    [:button {:style   btn-style
              :onClick (fn []
                         (let [id (swap! next-todo-id inc)]
                           (swap! todos conj {:id id :text (str "task " id)})))}
     "add"]
    [:button {:style   btn-style
              :onClick #(when (seq @todos) (swap! todos pop))}
     "pop"]
    [:button {:style   btn-style
              :onClick #(swap! todos (comp vec reverse))}
     "reverse"]]
   [:ul {:style "font-family: monospace; padding-left: 1.25rem"}
    [:for {:each todos}
     (fn [item index]
       [:li
        "#" (fn [] (inc (index)))
        " — " (:text item)
        " (id " (:id item) ")"])]]])

(defonce chart-data
  (atom [{:name "Mon" :uv 400 :pv 240}
         {:name "Tue" :uv 300 :pv 139}
         {:name "Wed" :uv 200 :pv 380}
         {:name "Thu" :uv 278 :pv 390}
         {:name "Fri" :uv 189 :pv 480}]))

(def ^:private days ["Sat" "Sun" "Mon²" "Tue²" "Wed²"])
(defonce ^:private day-idx (atom 0))

(defn chart-widget []
  [:div
   [:div {:style "margin-bottom: 0.5rem; display: flex; gap: 0.5rem"}
    [:button {:style   btn-style
              :onClick (fn []
                         (let [day (nth days (mod @day-idx (count days)))]
                           (swap! chart-data conj {:name day
                                                   :uv   (+ 100 (rand-int 400))
                                                   :pv   (+ 100 (rand-int 400))})
                           (swap! day-idx inc)))}
     "Add point"]
    [:button {:style   btn-style
              :onClick #(when (> (count @chart-data) 1)
                          (swap! chart-data (comp vec drop-last)))}
     "Remove point"]]
   [react/component LineChart
    {:width  600
     :height 300
     :data   chart-data
     :margin {:top 5 :right 20 :left 0 :bottom 5}}
    (react/el CartesianGrid {:strokeDasharray "3 3"})
    (react/el XAxis         {:dataKey "name"})
    (react/el YAxis         {})
    (react/el Tooltip       {})
    (react/el Legend        {})
    (react/el Line          {:type "monotone" :dataKey "pv" :stroke "#2563eb" :dot true})
    (react/el Line          {:type "monotone" :dataKey "uv" :stroke "#16a34a" :dot true})]])

(defonce step (atom 1))

(defn reagent-widget []
  [:div
   [:div {:style "margin-bottom: 0.5rem; display: flex; gap: 0.5rem; align-items: center"}
    [:span {:style "font-size: 0.85rem; color: #6b7280"} "step:"]
    (for [n [1 5 10]]
      ^{:key n}
      [:button {:style   btn-style
                :onClick #(reset! step n)}
       (str n)])]
   [react.reagent/component reagent-counter/counter {:label "reagent counter" :step step}]])

(defn render-page []
  [:div
   [:h2 {:style h2-style} "Atom counter (live atom in hiccup)"]
   [atom-counter]
   [atom-counter2]

   [:h2 {:style h2-style} "Keyed list ([:for] with atom-backed items)"]
   [for-widget]

   [:h2 {:style h2-style} "React bridge — recharts LineChart (atom-prop reactivity)"]
   [chart-widget]

   [:h2 {:style h2-style} "Reagent bridge — counter with r/atom internal state + atom prop"]
   [reagent-widget]])

;; ---------------------------------------------------------------------------
;; Server page — SSE queries, commands, diff streaming
;; ---------------------------------------------------------------------------

(defn clock-widget []
  (let [time (clock/time-flow)]
    [:div {:style "font-family: monospace; font-size: 1.4rem; margin: 1rem 0"}
     (fn []
       (if (time)
         [:span {:style "color: #2563eb"} (time)]
         [:span {:style "color: #9ca3af"} "connecting…"]))]))

(defn clock-widget-h []
  (let [time (clock/time-flow)]
    (h [:div {:style "font-family: monospace; font-size: 1.4rem; margin: 1rem 0"}
        (if ((:loading? time))
          [:span {:style "color: #9ca3af"} "connecting…"]
          [:span {:style "color: #2563eb"} (time)])])))

(defn slow-clock-widget []
  (let [time (clock/slow-time-flow)]
    [solidrpc/suspense time
     [:span {:style "color: #9ca3af"} "waiting for slow clock…"]
     (h [:span {:style "color: #2563eb; font-family: monospace; font-size: 1.4rem"}
         (time)])]))

(defn echo-widget []
  (let [[input  set-input]  (createSignal "hello")
        [result set-result] (createSignal nil)
        [error  set-error]  (createSignal nil)
        on-click (fn []
                   (-> (clock/echo (input))
                       (p/then  (fn [r] (set-result r) (set-error nil)))
                       (p/catch (fn [e] (set-error (str e)) (set-result nil)))))]
    [:div {:style "margin: 1rem 0;"}
     [:input {:value     (input)
              :onInput   #(set-input (-> % .-target .-value))
              :style     "padding: 0.4rem 0.6rem; border: 1px solid #d1d5db;
                          border-radius: 0.375rem; font-size: 1rem"}]
     [:button {:onClick on-click
               :style   "margin-left: 0.5rem; padding: 0.4rem 0.8rem;
                         background: #2563eb; color: white; border: none;
                         border-radius: 0.375rem; cursor: pointer; font-size: 1rem"}
      "Echo"]
     (fn []
       (when (result)
         [:pre {:style "margin-top: 0.5rem; background: #f3f4f6;
                        padding: 0.5rem; border-radius: 0.375rem"}
          (pr-str (result))]))
     (fn []
       (when (error)
         [:p {:style "color: #dc2626"} (error)]))]))

(defn- scoreboard-table [scores]
  (let [rows (->> scores (sort-by val >) (map-indexed vector))]
    [:table {:style "border-collapse: collapse; width: 100%; font-family: monospace"}
     [:thead
      [:tr
       [:th {:style "text-align: left;  padding: 0.25rem 0.5rem; border-bottom: 1px solid #e5e7eb; color: #6b7280; font-weight: 500"} "#"]
       [:th {:style "text-align: left;  padding: 0.25rem 0.5rem; border-bottom: 1px solid #e5e7eb; color: #6b7280; font-weight: 500"} "Player"]
       [:th {:style "text-align: right; padding: 0.25rem 0.5rem; border-bottom: 1px solid #e5e7eb; color: #6b7280; font-weight: 500"} "Score"]]]
     [:tbody
      (for [[rank [player score]] rows]
        ^{:key player}
        [:tr
         [:td {:style "padding: 0.2rem 0.5rem; color: #9ca3af"} (inc rank)]
         [:td {:style "padding: 0.2rem 0.5rem"} player]
         [:td {:style "padding: 0.2rem 0.5rem; text-align: right; color: #2563eb; font-weight: 600"} score]])]]))

(def ^:private max-log-entries 8)

(defn diff-stream-widget []
  (let [scores        (clock/scoreboard-flow)
        [log set-log] (createSignal [])]
    (createEffect
     (fn []
       (let [qs  (transit/write {:fn-name "api.clock/scoreboard-flow" :args []})
             url (str "/api/query?q=" (js/encodeURIComponent qs))
             es  (js/EventSource. url)
             add (fn [type data]
                   (let [size (.-length data)
                         ts   (.toLocaleTimeString (js/Date.))]
                     (set-log (fn [prev]
                                (vec (take-last max-log-entries
                                               (conj prev {:type type :size size :ts ts})))))))]
         (.addEventListener es "full"  (fn [e] (add :full  (.-data e))))
         (.addEventListener es "patch" (fn [e] (add :patch (.-data e))))
         (onCleanup #(.close es)))))
    [:div {:style "margin: 1rem 0"}
     (fn []
       (if (scores)
         [scoreboard-table (scores)]
         [:span {:style "color: #9ca3af; font-family: monospace"} "connecting…"]))
     [:div {:style "margin-top: 1rem"}
      [:div {:style "font-size: 0.75rem; font-weight: 600; color: #6b7280; margin-bottom: 0.25rem; text-transform: uppercase; letter-spacing: 0.05em"}
       "SSE event log"]
      [:div {:style "font-family: monospace; font-size: 0.8rem; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 0.375rem; padding: 0.5rem; min-height: 4rem"}
       (fn []
         (if (seq (log))
           [:div
            (for [{:keys [type size ts]} (reverse (log))]
              ^{:key ts}
              [:div {:style "display: flex; gap: 0.75rem; padding: 0.1rem 0"}
               [:span {:style "color: #9ca3af"} ts]
               [:span {:style (str "font-weight: 600; color: "
                                   (if (= type :full) "#16a34a" "#2563eb"))}
                (name type)]
               [:span {:style "color: #6b7280"} (str size " bytes")]])]
           [:span {:style "color: #9ca3af"} "waiting for events…"]))]]]))

(defn server-page []
  [:div
   [:h2 {:style h2-style} "Live clock (SSE query)"]
   [clock-widget]

   [:h2 {:style h2-style} "Live clock — h macro (no manual fn wrappers)"]
   [clock-widget-h]

   [:h2 {:style h2-style} "Slow clock (suspense — 3s delay before first value)"]
   [slow-clock-widget]

   [:h2 {:style h2-style} "Echo command (HTTP POST)"]
   [echo-widget]

   [:h2 {:style h2-style} "Diff streaming — scoreboard (first event :full, rest :patch)"]
   [diff-stream-widget]])

;; ---------------------------------------------------------------------------
;; Nav + root
;; ---------------------------------------------------------------------------

(def ^:private pages [:render :server])

(def ^:private page-labels {:render "Render" :server "Server"})

(defn- nav [current-page set-page]
  [:nav {:style "display: flex; gap: 0.5rem; margin-bottom: 2rem; border-bottom: 1px solid #e5e7eb; padding-bottom: 0.75rem"}
   (for [p pages]
     ^{:key p}
     (fn []
       (let [active? (= (current-page) p)]
         [:button
          {:onClick #(set-page p)
           :style   (str "padding: 0.4rem 1rem; border: none; border-radius: 0.375rem; cursor: pointer; font-size: 0.95rem; font-weight: 500; "
                         (if active?
                           "background: #2563eb; color: white;"
                           "background: transparent; color: #6b7280;"))}
          (page-labels p)])))])

(defn app []
  (let [[page set-page] (createSignal :render)]
    [:div {:style "max-width: 640px; margin: 4rem auto; padding: 0 1rem;
                   font-family: system-ui, sans-serif"}
     [:h1 {:style "font-size: 1.5rem; font-weight: 700; margin-bottom: 1.25rem"} "testapp"]
     [nav page set-page]
     (fn []
       (case (page)
         :render [render-page]
         :server [server-page]))]))
