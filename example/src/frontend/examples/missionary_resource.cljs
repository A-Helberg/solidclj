(ns frontend.examples.missionary-resource
  (:require-macros [solidclj.hiccup-macros :refer [h]])
  (:require [missionary.core :as m]
            [solidclj.missionary :as sm]
            [frontend.ui :as ui]))

;; A task is a recipe for one async value; running it again re-executes
;; the whole recipe. This one pretends to fetch for 900 ms.
(def fetch-time
  (m/sp (m/? (m/sleep 900))
        (.toLocaleTimeString (js/Date.))))

;; resource is eager: the fetch starts when the component body runs.
;; While it's in flight, the deref under [:suspense] suspends to the
;; boundary's fallback; reload! cancels and re-runs the task.
(defn example []
  (let [server-time (sm/resource fetch-time)]
    (h [:div {:class "space-y-3"}
        [:suspense {:fallback [:p {:class "text-gray-400"} "fetching…"]}
         (fn [] [:p {:class "font-mono"} "server time: " @server-time])]
        [ui/button {:on-click #(sm/reload! server-time)} "reload"]])))
