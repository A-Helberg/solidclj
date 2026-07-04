(ns frontend.examples.satom-h
  (:require-macros [solidclj.hiccup-macros :refer [h]])
  (:require [solidclj.api :as s]
            [frontend.ui :as ui]))

(defonce temp (s/atom 21))

;; @temp reads as (deref temp) — a list form — so h wraps each deref
;; in its own reactive thunk. No (fn []) in sight, and the temperature
;; line updates a single text node: watch only the number flash.
(defn example []
  (h [:div {:class "space-y-3"}
      [:p {:class "font-mono"} "temperature: " @temp "°C"]
      (if (< @temp 25)
        [:p {:class "text-blue-600"} "Comfortable."]
        [:p {:class "text-red-600"} "Getting warm!"])
      [:div {:class "flex gap-2"}
       [ui/button {:on-click #(swap! temp dec)} "−1°"]
       [ui/button {:on-click #(swap! temp inc)} "+1°"]]]))
