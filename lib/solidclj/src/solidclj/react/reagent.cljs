(ns solidclj.react.reagent
  "Reagent bridge for solidclj.react. Separate namespace so that
  `solidclj.react` compiles without reagent on the classpath.
  Consumers who have reagent as a dependency require this namespace;
  everyone else only needs `solidclj.react`."
  (:require [solidclj.react :refer [mount-bridge]]
            [reagent.core :as r]
            [reagent.dom.client :as rdom]))

(defn component
  "Renders a Reagent component into the SolidJS tree. Uses Reagent's own
  rendering pipeline so `r/atom` reactivity and hiccup return values work
  correctly.

      [react.reagent/component MyReagentView {:selected date-atom}]"
  [reagent-comp props]
  (mount-bridge props
                rdom/create-root
                (fn [resolved] (r/as-element [reagent-comp resolved]))))
