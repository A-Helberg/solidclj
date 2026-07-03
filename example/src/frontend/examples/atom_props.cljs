(ns frontend.examples.atom-props
  (:require [solidclj.api :as s]
            [frontend.ui :as ui]))

(defonce text (s/atom "solidclj"))

(defn example []
  [:div {:class "space-y-3"}
   ;; an atom as a prop value is live — the input follows the atom
   [ui/input {:value    text
              :on-input #(reset! text (.. % -target -value))}]

   ;; and deref inside a thunk derives from it reactively
   (fn []
     [:p {:class "font-mono"}
      "reversed: " (apply str (reverse @text))])])
