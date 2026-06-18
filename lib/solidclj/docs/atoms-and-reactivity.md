# Atoms and reactivity in solidclj

SolidJS does not re-run component functions on state change. Reactivity requires either passing atoms **underef'd** into hiccup slots, or using `s/?` inside thunks.

## Two patterns

**1. Underef'd in a hiccup slot** — the walker bridges the atom to a Solid signal automatically:

```clojure
[:div my-atom]                        ; child — re-renders on change
[:input {:value my-atom}]             ; prop
[:for {:each my-atom} render-fn]      ; list
[:dynamic {:component my-atom}]       ; component switching
[:show {:when my-atom} …]             ; nil/non-nil toggle only — see below
```

**2. `s/?` inside a `(fn [] …)` thunk** — use when you need the atom's value in Clojure logic:

```clojure
(:require [solidclj.api :as s])

(fn []
  (let [page (get pages (:page (s/? router/current-route)) not-found)]
    [page]))
```

`s/?` registers a Solid signal read. The thunk re-runs whenever the atom changes.

## Pitfalls

**`@atom` inside a thunk is not reactive.** `@` captures the value at the moment the thunk first runs — a snapshot. The thunk is never triggered again when the atom changes, so the UI stays frozen on that initial value:
```clojure
(fn [] [:div @my-atom])       ; wrong — frozen at first render
(fn [] [:div (s/? my-atom)])  ; right — updates on every change
```

**`(:key atom)` reads off the atom object, not its value** — always nil:
```clojure
(:page router/current-route)      ; wrong — nil
(:page (s/? router/current-route)) ; right
```

**`[:show {:when atom}]` only reacts to nil↔truthy changes.** If the atom changes from one truthy value to another (e.g. two different route maps), Show does not re-run its children. Use `s/?` in a plain thunk for value-dispatch.

## Quick reference

```
my-atom              underef'd in hiccup slot → walker bridges, live
@my-atom             snapshot — captures value once, never updates
(s/? my-atom)        reactive read inside (fn [] …) thunks
(s/? some-flow)      missionary flow → returns getter fn
(fn [] …)            reactive thunk — use s/? inside for atom reads
```
