(ns frontend.core
  (:require
   [solidclj.api :refer [render]]
   [frontend.flash :as flash]
   [frontend.app :as app]))

(defonce ^:private dispose* (atom nil))

(defn- mount! []
  (reset! dispose* (render [app/app] (.getElementById js/document "app"))))

(defn ^:export init []
  ;; observe <body> (not #app) so portal'd elements flash too
  (flash/install! js/document.body)
  (mount!))

(defn ^:dev/after-load reload []
  (when-let [dispose @dispose*] (dispose))
  (mount!))
