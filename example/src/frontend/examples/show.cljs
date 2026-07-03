(ns frontend.examples.show
  (:require [solidclj.api :as s]
            [frontend.ui :as ui]))

(defonce online? (s/atom true))

(defn example []
  [:div {:class "space-y-3"}
   [:show {:when     online?
           :fallback [:p {:class "text-gray-400"} "Offline — reconnect to continue."]}
    [:p {:class "text-green-600"} "Connected."]]
   [ui/button {:on-click #(swap! online? not)} "Toggle connection"]])
