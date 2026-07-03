(ns frontend.examples.elements)

(defn example []
  [:div {:class "space-y-2"}
   ;; classes and an id straight from the keyword
   [:p.font-semibold.text-blue-600#tag-shorthand
    "Classes and an id from the tag keyword"]

   ;; :class accepts a string, a vector, or a map of toggles
   [:p {:class ["font-mono" "text-sm"]}
    "…or a vector of classes"]
   [:p {:class {:underline true :line-through false}}
    "…or a map of class → boolean"]

   ;; :style accepts a map (kebab-case keys) or a raw CSS string
   [:p {:style {:color "rebeccapurple" :font-weight 600}}
    "Style maps use kebab-case keys"]
   [:p {:style "color: #16a34a"}
    "…or plain CSS strings"]])
