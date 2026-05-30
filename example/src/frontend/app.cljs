(ns frontend.app
  (:require-macros [solidclj.hiccup-macros :refer [h]])
  (:require
   ["solid-js" :refer [createSignal]]
   ["recharts" :refer [LineChart Line XAxis YAxis CartesianGrid Tooltip Legend ResponsiveContainer]]
   [promesa.core :as p]
   [api.clock :as clock]
   [solidrpc.call.solidjs :as solidrpc]
   [solidclj.react :as react]
   [solidclj.react.reagent :as react.reagent]
   [frontend.reagent-counter :as reagent-counter]))

;; ---------------------------------------------------------------------------
;; Atom counter — demonstrates the atom-as-live-value bridge in hiccup.cljs.
;; Pass the atom in directly → live; deref it → static snapshot.
;; ---------------------------------------------------------------------------

(defonce counter (atom 0))

;; Global singleton atom — survives hot-reload because of defonce.
;; All mounts of this component share the same counter.
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

;; Local atom created fresh on every mount — each instance gets its own
;; independent counter starting at -10.  Proves the atom-as-live-value
;; bridge works for plain locals, not just named defonce globals.
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

;; ---------------------------------------------------------------------------
;; [:for] — demonstrates Solid's <For> driven by an atom of items.
;; Add/pop/reverse keep item identities stable so Solid diffs in place;
;; the per-row index updates because Solid passes it as a signal getter.
;; ---------------------------------------------------------------------------

(defonce todos        (atom [{:id 1 :text "buy milk"}
                             {:id 2 :text "walk dog"}
                             {:id 3 :text "ship feature"}]))
(defonce next-todo-id (atom 4))

(def ^:private btn-style
  "margin-right: 0.5rem; padding: 0.3rem 0.6rem;
   background: #2563eb; color: white; border: none;
   border-radius: 0.375rem; cursor: pointer; font-size: 0.9rem")

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

;; ---------------------------------------------------------------------------
;; Live clock — SSE query
;; ---------------------------------------------------------------------------

(defn clock-widget []
  ;; `time` is a SolidJS signal getter. Anything that reads it must live
  ;; inside a `(fn [])` so SolidJS knows that's the region to re-run on
  ;; signal change.
  (let [time (clock/time-flow)]
    [:div {:style "font-family: monospace; font-size: 1.4rem; margin: 1rem 0"}
     (fn []
       (if (time)
         [:span {:style "color: #2563eb"} (time)]
         [:span {:style "color: #9ca3af"} "connecting…"]))]))

;; ---------------------------------------------------------------------------
;; h macro demo — same clock widget rewritten without manual (fn [])
;;
;; The h macro walks the hiccup literal at compile time and auto-wraps
;; list-form expressions in child positions as (fn []) thunks, so you
;; can write (if signal ...) directly instead of (fn [] (if signal ...)).
;; Plain (fn []) forms are left alone when you want a fine-grained scope.
;; ---------------------------------------------------------------------------

(defn clock-widget-h []
  (let [time (clock/time-flow)]
    (h [:div {:style "font-family: monospace; font-size: 1.4rem; margin: 1rem 0"}
        (if ((:loading? time))
          [:span {:style "color: #9ca3af"} "connecting…"]
          [:span {:style "color: #2563eb"} (time)])])))

;; ---------------------------------------------------------------------------
;; Suspense demo — slow clock with loading fallback
;; ---------------------------------------------------------------------------

(defn slow-clock-widget []
  (let [time (clock/slow-time-flow)]
    [solidrpc/suspense time
     [:span {:style "color: #9ca3af"} "waiting for slow clock…"]
     (h [:span {:style "color: #2563eb; font-family: monospace; font-size: 1.4rem"}
         (time)])]))

;; ---------------------------------------------------------------------------
;; Echo command — HTTP POST
;; ---------------------------------------------------------------------------

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

;; ---------------------------------------------------------------------------
;; React bridge demo — recharts LineChart embedded in the SolidJS tree.
;;
;; `chart-data` is a CLJS atom holding the data vector. Clicking "Add point"
;; swaps in a new value; the bridge watches the atom and calls root.render()
;; so recharts receives fresh data without the SolidJS tree re-running.
;; ---------------------------------------------------------------------------

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
     :data   chart-data        ;; atom — bridge watches and re-renders on change
     :margin {:top 5 :right 20 :left 0 :bottom 5}}
    (react/el CartesianGrid {:strokeDasharray "3 3"})
    (react/el XAxis         {:dataKey "name"})
    (react/el YAxis         {})
    (react/el Tooltip       {})
    (react/el Legend        {})
    (react/el Line          {:type "monotone" :dataKey "pv" :stroke "#2563eb" :dot true})
    (react/el Line          {:type "monotone" :dataKey "uv" :stroke "#16a34a" :dot true})]])

;; ---------------------------------------------------------------------------
;; Reagent bridge demo — counter component with r/atom internal state.
;;
;; `:step` is a CLJS atom. Changing it via the buttons updates the prop
;; passed into the Reagent component on the next root.render() call,
;; proving that atom-prop bridging works through frontend.react.reagent.
;; ---------------------------------------------------------------------------

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

;; ---------------------------------------------------------------------------
;; Root
;; ---------------------------------------------------------------------------

(defn app
  []
  [:div {:style "max-width: 640px; margin: 4rem auto; padding: 0 1rem;
                 font-family: system-ui, sans-serif"}
   [:h1 {:style "font-size: 1.5rem; font-weight: 700; margin-bottom: 0.5rem;"}
    "testapp"]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "Live clock (SSE query)"]
   [clock-widget]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "Live clock — rewritten with h macro (no manual fn wrappers)"]
   [clock-widget-h]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "Slow clock (suspense — 3s delay before first value)"]
   [slow-clock-widget]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "Echo command (HTTP POST)"]
   [echo-widget]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "Atom counter (live atom in hiccup)"]
   [atom-counter]
   [atom-counter2]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "Keyed list ([:for] with atom-backed items)"]
   [for-widget]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "React bridge — recharts LineChart (atom-prop reactivity)"]
   [chart-widget]

   [:h2 {:style "font-size: 1rem; font-weight: 600; color: #6b7280; margin: 1.5rem 0 0.25rem"}
    "Reagent bridge — counter with r/atom internal state + atom prop"]
   [reagent-widget]])
