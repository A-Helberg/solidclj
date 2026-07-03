(ns solidclj.api
  (:refer-clojure :exclude [atom])
  (:require
   [solidclj.hiccup :as hiccup]
   [solidclj.satom :as satom]))

(def render hiccup/render)
(def render-to-string hiccup/render-to-string)
(def hydrate hiccup/hydrate-app)
(def ? hiccup/?)

(def atom
  "Reagent-style reactive atom: a real atom that also subscribes any
  SolidJS tracking scope that derefs it. See solidclj.satom."
  satom/atom)

