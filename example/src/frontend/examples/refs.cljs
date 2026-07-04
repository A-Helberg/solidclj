(ns frontend.examples.refs
  (:require [solidclj.docs.ui :as ui]))

(defn example []
  (let [input-el (atom nil)] ;; holds the DOM node, not app state
    [:div {:class "flex gap-2"}
     [ui/input {:placeholder "click focus →"
                :ref         #(reset! input-el %)}]
     [ui/button {:on-click #(some-> @input-el .focus)} "Focus"]]))
