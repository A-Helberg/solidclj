(ns frontend.examples.atom-props
  (:require-macros [solidclj.hiccup-macros :refer [h]])
  (:require [solidclj.api :as s]
            [solidclj.docs.ui :as ui]))

(defonce text (s/atom "solidclj"))

;; under h, a deref in a prop value becomes a live accessor:
;; {:value @text} compiles to {:value (fn [] @text)}
(defn example []
  (h [:div {:class "space-y-3"}
      [ui/input {:value    @text
                 :on-input #(reset! text (.. % -target -value))}]
      [:p {:class "font-mono"} "reversed: " (apply str (reverse @text))]]))
