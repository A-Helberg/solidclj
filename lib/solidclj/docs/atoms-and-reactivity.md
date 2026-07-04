# Atoms and reactivity in solidclj

SolidJS does not re-run component functions on state change. Reactive state
lives in **`s/atom`** (`solidclj.api/atom`, implemented in `solidclj.satom`):
a real Clojure atom — `swap!`, `reset!`, `add-watch`, validators all behave
exactly like `cljs.core/atom` — whose deref *also* subscribes the Solid
tracking scope it runs in.

Plain `cljs.core/atom`s are **not** special. The renderer ignores them (dev
builds warn), and they never trigger updates. Use them for non-reactive
mutable holders — DOM refs, test scaffolding — and `s/atom` for anything the
UI should react to. Custom reference types can opt in by implementing
`solidclj.satom/IReactiveAtom` (plus `IDeref` + `IWatchable`).

## Three patterns

**1. Deref inside a `(fn [] …)` thunk** — the thunk re-runs when the atom changes:

```clojure
(:require [solidclj.api :as s])

(defonce temp (s/atom 21))

(fn [] [:p "it is " @temp "°C"])
```

**2. Bare deref under the `h` macro** — `@temp` reads as `(deref temp)`, a
list form, so `h` wraps it in its own thunk. This works in child slots *and*
prop values (except `:on*`/`:ref`, which are callbacks):

```clojure
(h [:p "it is " @temp "°C"])       ; text node updates in place
(h [:input {:value @temp}])        ; live prop
```

**3. Underef'd in a hiccup slot** — the walker bridges the s/atom to a Solid
signal automatically:

```clojure
[:div my-satom]                        ; child — updates on change
[:input {:value my-satom}]             ; prop
[:for {:each my-satom} render-fn]      ; list
[:dynamic {:component my-satom}]       ; component switching
[:show {:when my-satom} …]             ; condition
```

`s/?` still exists for missionary flows (`(s/? some-flow)` → getter fn) and
also accepts an s/atom, where it is equivalent to a deref.

## Pitfalls

**`@atom` outside any thunk is a snapshot.** Component bodies are not
tracking scopes (unlike Reagent, the component fn runs once), so a bare
deref at the top level of a component captures the value at mount:

```clojure
(defn c []
  [:div @temp])          ; frozen at mount — deref ran in the component body

(defn c []
  [:div (fn [] @temp)])  ; live — deref runs inside the thunk

(defn c []
  (h [:div @temp]))      ; live — h moves the deref into a thunk for you
```

**Plain atoms don't update anything.** `[:div my-plain-atom]` renders
nothing and warns in dev; `(fn [] @my-plain-atom)` runs once and never
re-runs. Reach for `s/atom`.

## Quick reference

```
(fn [] @my-satom)     live — deref subscribes the thunk
(h [:p @my-satom])    live — h auto-wraps derefs (children and props)
my-satom              underef'd in a hiccup slot → walker bridges, live
@my-satom             outside a thunk: snapshot, captured once
(s/? some-flow)       missionary flow → returns getter fn
plain (atom …)        ignored by the renderer (dev warning)
```
