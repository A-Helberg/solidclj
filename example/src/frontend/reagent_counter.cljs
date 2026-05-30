(ns frontend.reagent-counter
  (:require [reagent.core :as r]))

;; A deliberately stateful Reagent component — uses r/atom internally so
;; Reagent manages its own re-renders independently of SolidJS.
(defn counter
  "Accepts {:label str :step int} props from the SolidJS bridge.
  Demonstrates that r/atom reactivity inside a Reagent component works
  correctly when mounted via frontend.react.reagent/component."
  [{:keys [label step] :or {label "count" step 1}}]
  (let [n (r/atom 0)]
    (fn [{:keys [label step] :or {label "count" step 1}}]
      [:div {:style {:font-family "monospace" :margin "0.5rem 0"}}
       [:span {:style {:font-size "1.2rem" :margin-right "0.75rem"}}
        label ": " @n]
       [:button {:on-click #(swap! n + step)
                 :style {:margin-right "0.4rem" :padding "0.2rem 0.6rem"
                         :background "#2563eb" :color "white"
                         :border "none" :border-radius "0.375rem" :cursor "pointer"}}
        (str "+" step)]
       [:button {:on-click #(swap! n - step)
                 :style {:padding "0.2rem 0.6rem"
                         :background "#dc2626" :color "white"
                         :border "none" :border-radius "0.375rem" :cursor "pointer"}}
        (str "-" step)]])))
