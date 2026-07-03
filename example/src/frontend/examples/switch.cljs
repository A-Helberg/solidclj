(ns frontend.examples.switch
  (:require [solidclj.api :as s]
            [frontend.ui :as ui]))

(defonce status (s/atom :loading))

(defn example []
  [:div {:class "space-y-3"}
   [:switch {:fallback [:p {:class "text-gray-400"} "Unknown status."]}
    [:match {:when (fn [] (= :loading @status))}
     [:p {:class "text-blue-600"} "Loading…"]]
    [:match {:when (fn [] (= :ready @status))}
     [:p {:class "text-green-600"} "Ready!"]]
    [:match {:when (fn [] (= :error @status))}
     [:p {:class "text-red-600"} "Something failed."]]]

   [:div {:class "flex gap-2"}
    (for [st [:loading :ready :error]]
      ^{:key st}
      [ui/button {:on-click #(reset! status st)} (name st)])]])
