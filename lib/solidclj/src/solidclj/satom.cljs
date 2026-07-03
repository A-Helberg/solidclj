(ns solidclj.satom
  "A Reagent-style reactive atom backed by a SolidJS signal.

  `satom/atom` returns something that IS an atom — `swap!`, `reset!`,
  `add-watch`, validators and `@` all behave exactly like
  `cljs.core/atom`. The difference is what happens when you deref it
  inside a SolidJS tracking scope (a render thunk, `createEffect`,
  `createMemo`, …): the read also subscribes that computation to the
  atom, so the computation re-runs when the atom's value changes.

  That makes the Reagent idiom work directly in hiccup:

      (def temp (s/atom 21))

      [:div
       (fn [] [:p \"it is \" @temp \"°C\"])]   ;; re-runs on swap!/reset!

  Outside a tracking scope, deref is a plain read — no subscription,
  no warning, no owner required.

  Semantics
  =========
  - `swap!`/`reset!` notify watchers unconditionally (normal atom
    behaviour), but reactive propagation is deduplicated with CLJS `=`:
    resetting to an equal value does not re-run Solid computations.
  - An s/atom also satisfies IDeref + IWatchable, so everything that
    works with a plain atom in hiccup (un-deref'd children, prop
    values, `:each`, …) works with an s/atom too.
  - Component bodies are NOT tracking scopes in solidclj (unlike
    Reagent, the component fn runs once). A bare `@a` at the top level
    of a component is a one-shot snapshot; deref inside a `(fn [] …)`
    thunk is live."
  (:refer-clojure :exclude [atom])
  (:require ["solid-js" :refer [createSignal getListener]]))

(deftype SAtom [^:mutable state ^:mutable meta validator ^:mutable watches
                signal-get signal-set]
  Object
  (equiv [this other] (-equiv this other))

  IAtom

  IDeref
  (-deref [_]
    ;; Inside a Solid tracking scope, touch the mirror signal so the
    ;; running computation subscribes. The value itself always comes
    ;; from `state` — signal and state are written together in -reset!.
    (when (some? (getListener))
      (signal-get))
    state)

  IReset
  (-reset! [a new-value]
    (when-not (nil? validator)
      (when-not (validator new-value)
        (throw (js/Error. "Validator rejected reference state"))))
    (let [old-value state]
      (set! state new-value)
      ;; fn form so values that are themselves functions are stored,
      ;; not invoked as updater fns by Solid's setter.
      (signal-set (fn [_] new-value))
      (when-not (nil? watches)
        (-notify-watches a old-value new-value))
      new-value))

  ISwap
  (-swap! [a f]          (-reset! a (f state)))
  (-swap! [a f x]        (-reset! a (f state x)))
  (-swap! [a f x y]      (-reset! a (f state x y)))
  (-swap! [a f x y more] (-reset! a (apply f state x y more)))

  IWatchable
  (-notify-watches [a old new]
    (doseq [[k f] watches]
      (f k a old new)))
  (-add-watch [a k f]
    (set! watches (assoc watches k f))
    a)
  (-remove-watch [a k]
    (set! watches (dissoc watches k))
    a)

  IMeta
  (-meta [_] meta)

  IEquiv
  (-equiv [o other] (identical? o other))

  IHash
  (-hash [o] (goog/getUid o))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<SAtom: ")
    (pr-writer state writer opts)
    (-write writer ">")))

(defn atom
  "Like cljs.core/atom, but deref inside a SolidJS tracking scope
  (render thunk / effect / memo) subscribes that computation to the
  atom. See the namespace docstring for details.

      (def a (satom/atom 0))
      (def b (satom/atom {} :meta m :validator map?))"
  ([x]
   (let [[g s] (createSignal x #js {:equals (fn [a b] (= a b))})]
     (SAtom. x nil nil nil g s)))
  ([x & {:keys [meta validator]}]
   (let [[g s] (createSignal x #js {:equals (fn [a b] (= a b))})]
     (SAtom. x meta validator nil g s))))
