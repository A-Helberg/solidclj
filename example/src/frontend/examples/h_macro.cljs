(ns frontend.examples.h-macro
  (:require-macros [solidclj.hiccup-macros :refer [h]])
  (:require [solidclj.api :as s]
            [frontend.ui :as ui]))

(defonce logged-in? (s/atom false))

;; h walks the hiccup at compile time and wraps expressions like the
;; (if …) below in (fn []) reactive thunks for you.
(defn example []
  (h [:div {:class "space-y-3"}
      (if @logged-in?
        [:p {:class "text-green-600"} "Welcome back!"]
        [:p {:class "text-gray-500"} "Please log in."])
      [ui/button {:on-click #(swap! logged-in? not)} "Toggle login"]]))
