(ns frontend.core
  (:require
   [solidclj.api :refer [render]]
   [frontend.app :as app]))

(defn ^:export init []
  (render [app/app] (.getElementById js/document "app")))
