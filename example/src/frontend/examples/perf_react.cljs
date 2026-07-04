(ns frontend.examples.perf-react
  "The same 50×50 grid rendered by React through solidclj's bridge.
  State lives at the top, so every tick re-renders the whole grid:
  React re-runs createElement for all 2500 dots and re-stamps each one
  (data-tick marks the render pass that produced it — the same thing
  React DevTools' 'highlight updates' visualises)."
  (:require ["react" :refer [createElement]]
            ["solid-js" :refer [onCleanup]]
            [solidclj.api :as s]
            [solidclj.react :as react]))

(def size 50)

(def palette
  ["#3b82f6" "#22c55e" "#eab308" "#ef4444" "#a855f7" "#e5e7eb"])

(defonce state
  (s/atom {:cells (vec (repeat (* size size) "#e5e7eb"))
           :tick  0}))

(defn- tick!
  "Recolor one random dot; the whole grid re-renders."
  []
  (swap! state
         (fn [{:keys [cells tick]}]
           {:cells (assoc cells (rand-int (count cells)) (rand-nth palette))
            :tick  (inc tick)})))

(defn- Grid
  "A plain React component."
  [^js props]
  (let [cells (.. props -state -cells)
        tick  (.. props -state -tick)]
    (createElement "div"
                   #js {:style #js {:display             "grid"
                                    :gap                 "2px"
                                    :gridTemplateColumns (str "repeat(" size ",10px)")}}
                   (.map cells
                         (fn [color i]
                           (createElement "div"
                                          #js {:key       i
                                               :data-tick tick
                                               :style     #js {:width        "10px"
                                                               :height       "10px"
                                                               :borderRadius "5px"
                                                               :background   color}}))))))

(defn grid []
  (let [interval (js/setInterval tick! 125)] ;; 8 dots/second
    (onCleanup #(js/clearInterval interval))
    [react/component Grid {:state state}]))
