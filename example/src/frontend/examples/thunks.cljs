(ns frontend.examples.thunks
  (:require ["solid-js" :refer [createSignal]]
            [frontend.ui :as ui]))

(defn example []
  (let [[n set-n] (createSignal 0)]
    [:div {:class "space-y-3"}
     ;; a signal getter in a child slot is live
     [:p {:class "font-mono"} "count: " n]

     ;; anything computed from a signal must be wrapped in (fn []) —
     ;; that's the region SolidJS re-runs when n changes
     (fn []
       (if (even? (n))
         [:p {:class "text-green-600"} "n is even"]
         [:p {:class "text-orange-600"} "n is odd"]))

     [ui/button {:on-click #(set-n (inc (n)))} "Increment"]]))
