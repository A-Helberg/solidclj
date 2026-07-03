(ns frontend.examples.dynamic
  (:require [frontend.ui :as ui]))

(defonce tag (atom "h2"))

(defn example []
  [:div {:class "space-y-3"}
   [:div {:class "flex gap-2"}
    (for [t ["h2" "p" "em" "code"]]
      ^{:key t}
      [ui/button {:on-click #(reset! tag t)} t])]

   ;; :component may be a tag string, a component fn, or an atom of one
   [:dynamic {:component tag}
    "Same content, different element"]])
