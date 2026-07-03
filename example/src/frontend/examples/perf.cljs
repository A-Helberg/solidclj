(ns frontend.examples.perf
  "Toggle between the Solid-rendered and React-rendered dot grids.
  Watch the blue update-flashes: Solid touches one dot per tick,
  React re-stamps all 2500."
  (:require ["solid-js" :refer [createSignal]]
            [frontend.examples.perf-solid :as perf-solid]
            [frontend.examples.perf-react :as perf-react]))

(defn demo []
  (let [[mode set-mode] (createSignal :solid)
        tab (fn [m label]
              (fn []
                [:button {:class   (str "px-4 py-1.5 rounded-md text-sm font-medium cursor-pointer "
                                        (if (= m (mode))
                                          "bg-blue-600 text-white"
                                          "bg-gray-100 text-gray-600 hover:bg-gray-200"))
                          :onClick #(set-mode m)}
                 label]))]
    [:div {:class "space-y-4"}
     [:div {:class "flex gap-2"}
      (tab :solid "Solid")
      (tab :react "React")]
     (fn []
       (case (mode)
         :solid [perf-solid/grid]
         :react [perf-react/grid]))]))
