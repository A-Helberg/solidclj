(ns frontend.examples.atoms
  (:require [frontend.ui :as ui]))

;; even a plain cljs.core/atom renders live when passed UN-deref'd
(defonce counter (atom 0))

(defn example []
  [:div {:class "space-y-3"}
   [:p {:class "font-mono"}
    "live: " counter
    " — snapshot at mount: " @counter]
   [ui/button {:on-click #(swap! counter inc)} "swap! inc"]])
