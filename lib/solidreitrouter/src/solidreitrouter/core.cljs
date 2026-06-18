(ns solidreitrouter.core
  (:require [reitit.frontend      :as rf]
            [reitit.frontend.easy :as rfe]))

(defonce current-route (atom nil))

(defn- on-navigate [match _history]
  (reset! current-route (:data match)))

(defn init!
  "Initialise the router. Call once at app startup, before render.

  routes — standard reitit route table (same one used server-side).
  opts   — optional map forwarded to rfe/start!.
           Defaults to {:use-fragment false} (HTML5 pushState, no hash).

  Resets current-route to the matched route's :data map on every
  navigation, or nil when no route matches."
  ([routes]
   (init! routes {}))
  ([routes opts]
   (when ^boolean goog.DEBUG
     (when (some? @current-route)
       (js/console.warn "[solidreitrouter] init! called more than once.")))
   (rfe/start! (rf/router routes)
               on-navigate
               (merge {:use-fragment false} opts))))
