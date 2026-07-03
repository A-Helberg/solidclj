(ns frontend.examples.fragments)

;; [:<>] renders its children as siblings, with no wrapper element —
;; here each row contributes a <dt> AND a <dd> to the parent <dl>.
(defn legend-row [label swatch-class]
  [:<>
   [:dt {:class "font-medium"}
    [:span {:class (str "inline-block w-3 h-3 rounded-sm mr-2 " swatch-class)}]
    label]
   [:dd {:class "text-gray-500 pl-5 mb-2"}
    "two siblings, no wrapper div"]])

(defn example []
  [:dl
   [legend-row "primary" "bg-blue-500"]
   [legend-row "success" "bg-green-500"]])
