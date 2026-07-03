(ns frontend.app
  "Docs shell: sidebar navigation on the left, the selected guide page
  (prose + live examples) on the right."
  (:require ["solid-js" :refer [createSignal]]
            [frontend.pages :as pages]
            [frontend.flash :as flash]
            [frontend.ui :as ui]))

(def ^:private all-pages
  (vec (mapcat :pages pages/sections)))

(defn- find-page [id]
  (or (some #(when (= id (:id %)) %) all-pages)
      (first all-pages)))

(defn- initial-page-id []
  (let [h (.. js/location -hash)]
    (if (seq h)
      (:id (find-page (keyword (subs h 1))))
      (:id (first all-pages)))))

(defn- nav-item [id title current go]
  (fn []
    [:button {:class   (str "block w-full text-left px-2 py-1 rounded text-sm cursor-pointer "
                            (if (= id (current))
                              "bg-blue-100 text-blue-700 font-medium"
                              "text-gray-600 hover:bg-gray-100"))
              :onClick #(go id)}
     title]))

(defn- sidebar [current go]
  [:aside {:class "w-64 shrink-0 border-r border-gray-200 bg-gray-50"}
   [:div {:class "sticky top-0 max-h-screen overflow-y-auto p-4"}
    [:div {:class "flex items-baseline gap-2"}
     [:span {:class "text-xl font-bold text-gray-900"} "solidclj"]
     [:span {:class "text-xs text-gray-400"} "guide"]]

    (for [{:keys [title pages]} pages/sections]
      ^{:key title}
      [:div {:class "mt-5"}
       [:h2 {:class "text-xs font-semibold uppercase tracking-wider text-gray-400"} title]
       [:ul {:class "mt-2 space-y-0.5"}
        (for [{:keys [id title]} pages]
          ^{:key id}
          [:li [nav-item id title current go]])]])

    [:div {:class "mt-8 pt-4 border-t border-gray-200"}
     [:label {:class "flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none"}
      [:input {:type     "checkbox"
               :checked  flash/enabled?
               :onChange #(swap! flash/enabled? not)}]
      "Flash DOM updates"]]]])

(defn page-view [{:keys [title prose body examples]}]
  [:div {:class "max-w-4xl"}
   [:h1 {:class "text-2xl font-bold text-gray-900 mb-4"} title]
   (if body
     body
     [:<>
      [:div {:class "prose prose-gray max-w-none"} prose]
      (for [ex examples]
        ^{:key (:source ex)}
        [ui/example-block ex])])])

(defn app []
  (let [[current set-current] (createSignal (initial-page-id))
        go (fn [id]
             (set-current id)
             (set! (.. js/location -hash) (name id))
             (js/window.scrollTo 0 0))]
    [:div {:class "flex min-h-screen bg-white text-gray-900 antialiased"}
     [sidebar current go]
     [:main {:class "flex-1 min-w-0 px-8 py-10"}
      (fn [] [page-view (find-page (current))])]]))
