(ns frontend.examples.satom
  (:require [solidclj.api :as s]
            [frontend.ui :as ui]))

;; s/atom is a real atom — swap!, reset!, add-watch all work — but
;; deref'ing it inside a reactive thunk also subscribes the thunk.
(defonce temp (s/atom 21))

(defn example []
  [:div {:class "space-y-3"}
   (fn [] [:p {:class "font-mono"} "temperature: " @temp "°C"])

   (fn []
     (if (< @temp 25)
       [:p {:class "text-blue-600"} "Comfortable."]
       [:p {:class "text-red-600"} "Getting warm!"]))

   [:div {:class "flex gap-2"}
    [ui/button {:on-click #(swap! temp dec)} "−1°"]
    [ui/button {:on-click #(swap! temp inc)} "+1°"]]])
