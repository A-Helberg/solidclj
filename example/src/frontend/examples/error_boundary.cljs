(ns frontend.examples.error-boundary
  (:require ["solid-js" :refer [createSignal]]
            [frontend.ui :as ui]))

(defn example []
  (let [[boom set-boom] (createSignal false)]
    [:div {:class "space-y-3"}
     [ui/button {:on-click #(set-boom true)} "Throw"]

     [:error-boundary {:fallback (fn [err reset]
                                   [:div {:class "space-y-2"}
                                    [:p {:class "text-red-600"}
                                     "Caught: " (.-message err)]
                                    [ui/button {:on-click (fn []
                                                            (set-boom false)
                                                            (reset))}
                                     "Recover"]])}
      (fn []
        (when (boom) (throw (js/Error. "kaboom")))
        [:p {:class "text-green-600"} "All good."])]]))
