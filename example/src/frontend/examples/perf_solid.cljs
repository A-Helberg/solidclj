(ns frontend.examples.perf-solid
  "A 50×50 grid where every dot is its own s/atom. Updating a dot
  touches exactly one DOM node — nothing else re-renders."
  (:require ["solid-js" :refer [onCleanup]]
            [solidclj.api :as s]))

(def size 50)

(def palette
  ["#3b82f6" "#22c55e" "#eab308" "#ef4444" "#a855f7" "#e5e7eb"])

(defonce cells
  (vec (repeatedly (* size size) #(s/atom "#e5e7eb"))))

(defn- tick!
  "Recolor one random dot."
  []
  (reset! (nth cells (rand-int (count cells))) (rand-nth palette)))

(defn grid []
  (let [interval (js/setInterval tick! 125)] ;; 8 dots/second
    (onCleanup #(js/clearInterval interval))
    [:div {:style (str "display:grid;gap:2px;"
                       "grid-template-columns:repeat(" size ",10px)")}
     (map-indexed
      (fn [i cell]
        ^{:key i}
        [:div {:style (fn [] (str "width:10px;height:10px;border-radius:5px;"
                                  "background:" @cell))}])
      cells)]))
