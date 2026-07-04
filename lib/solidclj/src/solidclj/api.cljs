(ns solidclj.api
  (:refer-clojure :exclude [atom])
  (:require
   [solidclj.hiccup :as hiccup]
   [solidclj.missionary :as sm]
   [solidclj.satom :as satom]))

(def render hiccup/render)
(def render-to-string hiccup/render-to-string)
(def hydrate hiccup/hydrate-app)

(def atom
  "Reagent-style reactive atom: a real atom that also subscribes any
  SolidJS tracking scope that derefs it. See solidclj.satom."
  satom/atom)

(defn ?
  "Read a reactive value within the current Solid owner.

  Accepts either:
  - A **reactive atom** (s/atom, or a solidclj.missionary hold/resource)
    — bridges it to a Solid signal and returns the current value
    directly. Equivalent to deref'ing it inside a reactive thunk.
  - A **missionary flow** — materialises it into a Solid signal and
    returns the signal getter fn. Call the result to read the current
    value: ((? flow)). Deduplicated per owner; runs for the owner's
    lifetime. For a shareable, lazily-managed ref use
    solidclj.missionary/hold instead.

  Analogous to m/? in missionary."
  [x]
  (if (satisfies? satom/IReactiveAtom x)
    ((hiccup/atom->signal-getter x))
    (sm/flow-getter x)))
