(ns frontend.examples.js-components
  (:require ["solid-js" :refer [createSignal]]
            ["solid-js/h" :as h-module]
            [frontend.ui :as ui]))

(def h (or (.-default h-module) h-module))

;; A Solid component written against the raw JS API — this stands in
;; for anything you'd import from an npm Solid library.
(defn fancy-badge [props]
  (h "span" #js {:class "inline-block rounded-full bg-purple-100 text-purple-700 px-3 py-1 text-sm"}
     (fn [] (.-label props))))

;; [:>] invokes a JS component: props become a JS object; fn-valued
;; props are passed as Solid accessors.
(defn example []
  (let [[n set-n] (createSignal 0)]
    [:div {:class "space-y-3"}
     [:> fancy-badge {:label (fn [] (str "clicked " (n) " times"))}]
     [ui/button {:on-click #(set-n (inc (n)))} "Click"]]))
