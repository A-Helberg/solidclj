(ns frontend.app
  (:require [solidclj.docs :as docs]
            [frontend.flash :as flash]
            [frontend.pages :as pages]))

(defn app []
  [docs/app {:title          "solidclj"
             :subtitle       "guide"
             :sections       pages/sections
             :sidebar-footer flash/toggle}])
