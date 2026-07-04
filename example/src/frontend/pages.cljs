(ns frontend.pages
  "The guide's content. Each page is prose plus example blocks whose
  source is inlined at compile time with shadow.resource/inline — the
  code you read IS the code that runs, they cannot drift apart."
  (:require [shadow.resource :as rc]
            [frontend.ui :as ui]
            [frontend.examples.perf :as perf]
            [frontend.examples.hello :as hello]
            [frontend.examples.elements :as elements]
            [frontend.examples.thunks :as thunks]
            [frontend.examples.satom :as satom]
            [frontend.examples.atoms :as atoms]
            [frontend.examples.atom-props :as atom-props]
            [frontend.examples.h-macro :as h-macro]
            [frontend.examples.show :as show]
            [frontend.examples.switch :as switch]
            [frontend.examples.dynamic :as dynamic]
            [frontend.examples.for-list :as for-list]
            [frontend.examples.index-list :as index-list]
            [frontend.examples.seqs :as seqs]
            [frontend.examples.fragments :as fragments]
            [frontend.examples.refs :as refs]
            [frontend.examples.portal :as portal]
            [frontend.examples.suspense :as suspense]
            [frontend.examples.error-boundary :as error-boundary]
            [frontend.examples.js-components :as js-components]))

(def sections
  [{:title "Introduction"
    :pages
    [{:id    :home
      :title "Why solidclj?"
      :body
      [:div
       [:div {:class "prose prose-gray max-w-none"}
        [:p "solidclj is Reagent-style hiccup on top of SolidJS. You write plain "
         "ClojureScript functions that return hiccup vectors — but instead of "
         "re-rendering components and diffing a virtual DOM, SolidJS wires "
         "fine-grained subscriptions directly to the DOM nodes that depend on "
         "each piece of state. When state changes, only those nodes update."]
        [:p "The grid below makes the difference visible. It is 2500 dots; 8 random "
         "dots change color every second. Every DOM node that changes gets a "
         "brief blue flash (that's global on this site — toggle it in the "
         "sidebar). On the Solid side, a tick touches exactly one dot. On the "
         "React side the state lives at the top, so every tick re-runs the whole "
         "grid — each dot is re-stamped with the render pass that produced it, "
         "which is the same thing React DevTools' “highlight updates” shows."]]
       [perf/demo]
       [:details {:class "mt-6 border border-gray-200 rounded-lg overflow-hidden"}
        [:summary {:class "px-4 py-2 text-sm font-medium text-gray-600 cursor-pointer bg-gray-50"}
         "Solid grid source"]
        [ui/code-block (rc/inline "frontend/examples/perf_solid.cljs")]]
       [:details {:class "mt-3 border border-gray-200 rounded-lg overflow-hidden"}
        [:summary {:class "px-4 py-2 text-sm font-medium text-gray-600 cursor-pointer bg-gray-50"}
         "React grid source"]
        [ui/code-block (rc/inline "frontend/examples/perf_react.cljs")]]]}]}

   {:title "Basics"
    :pages
    [{:id    :components
      :title "Components"
      :prose
      [:<>
       [:p "A component is a plain function that returns hiccup. There are no "
        "macros, no registration, no lifecycle protocol — putting the function "
        "in the first slot of a vector invokes it with the remaining elements "
        "as positional arguments."]
       [:p "Unlike Reagent, the component function runs " [:strong "once"] ". "
        "It builds the DOM; afterwards only the reactive regions inside it "
        "(thunks, signals, atoms — covered in the Reactivity section) ever "
        "run again."]]
      :examples
      [{:source    (rc/inline "frontend/examples/hello.cljs")
        :component hello/example}]}

     {:id    :elements
      :title "Elements & props"
      :prose
      [:<>
       [:p "HTML elements are keywords. Classes and ids can ride on the keyword "
        "itself (" [:code ":p.font-bold#title"] "), and the props map accepts "
        "several shapes for " [:code ":class"] " and " [:code ":style"] "."]
       [:p "Keep Tailwind variants like " [:code "hover:underline"] " and "
        "classes containing dots (" [:code "py-1.5"] ") in the "
        [:code ":class"] " string — the keyword shorthand splits on "
        [:code "."] " and can't represent them."]]
      :examples
      [{:source    (rc/inline "frontend/examples/elements.cljs")
        :component elements/example}]}]}

   {:title "Reactivity"
    :pages
    [{:id    :thunks
      :title "Signals & thunks"
      :prose
      [:<>
       [:p "The one rule to remember: SolidJS does " [:strong "not"] " re-run "
        "your component when state changes. You mark the dynamic region of the "
        "tree by wrapping it in a " [:code "(fn [] …)"] " — that thunk is what "
        "re-runs when a signal read inside it changes."]
       [:p "Solid's own primitives work directly: " [:code "createSignal"]
        " returns a getter/setter pair, and a getter placed in a child slot "
        "is live on its own."]
       [:p "Watch the flashes as you increment: the count paragraph only "
        "updates its text, and the even/odd thunk swaps in a fresh "
        [:code "<p>"] " because it returns a new element each run. The "
        "button never flashes — the component function ran once, and "
        "nothing outside a thunk is ever touched again."]]
      :examples
      [{:source    (rc/inline "frontend/examples/thunks.cljs")
        :component thunks/example}]}

     {:id    :satom
      :title "Reactive atoms — s/atom"
      :prose
      [:<>
       [:p [:code "s/atom"] " is the Reagent idiom brought over: a real atom ("
        [:code "swap!"] ", " [:code "reset!"] ", " [:code "add-watch"]
        " and validators all behave exactly like " [:code "cljs.core/atom"]
        ") that " [:strong "also"] " subscribes any reactive scope that derefs "
        "it. " [:code "(fn [] @my-atom)"] " is a live view of the atom."]
       [:p "Resetting to an " [:code "="] " value notifies watchers (normal "
        "atom semantics) but does not re-run reactive scopes, so no-op updates "
        "are free. One difference from Reagent: the component body is not a "
        "reactive scope here, so a bare " [:code "@a"] " at the top level of a "
        "component is a one-shot snapshot — deref inside thunks."]]
      :examples
      [{:source    (rc/inline "frontend/examples/satom.cljs")
        :component satom/example}]}

     {:id    :atoms
      :title "Plain atoms in hiccup"
      :prose
      [:<>
       [:p "Even a plain " [:code "cljs.core/atom"] " (or anything IDeref + "
        "IWatchable) renders live when passed " [:em "un-deref'd"] " into a "
        "child slot — the walker bridges it to a Solid signal and tears the "
        "watch down when the surrounding scope disposes."]
       [:p [:code "@atom"] " in hiccup is just a value: a snapshot taken when "
        "the component ran. The example renders both so you can watch one "
        "move and the other stand still."]]
      :examples
      [{:source    (rc/inline "frontend/examples/atoms.cljs")
        :component atoms/example}]}

     {:id    :atom-props
      :title "Atoms in props"
      :prose
      [:<>
       [:p "Atoms are live in prop position too: "
        [:code "[:input {:value my-atom}]"] " keeps the input in sync with "
        "the atom. This works for any prop, and for values inside "
        [:code ":style"] " and " [:code ":class"] " maps."]]
      :examples
      [{:source    (rc/inline "frontend/examples/atom_props.cljs")
        :component atom-props/example}]}

     {:id    :h-macro
      :title "The h macro"
      :prose
      [:<>
       [:p "Writing " [:code "(fn [] …)"] " around every dynamic expression "
        "gets old. The " [:code "h"] " macro walks a hiccup literal at compile "
        "time and wraps list-forms in child positions — like the "
        [:code "(if …)"] " below — into reactive thunks for you."]
       [:p "Explicit " [:code "(fn [] …)"] " forms are left alone, so you can "
        "mix both styles and keep fine-grained control where you want it."]]
      :examples
      [{:source    (rc/inline "frontend/examples/h_macro.cljs")
        :component h-macro/example}]}]}

   {:title "Control flow"
    :pages
    [{:id    :show
      :title "Show"
      :prose
      [:<>
       [:p [:code "[:show {:when … :fallback …}]"] " wraps Solid's "
        [:code "<Show>"] ": children render while " [:code ":when"] " is "
        "truthy, the fallback otherwise. " [:code ":when"] " accepts an atom, "
        "a signal getter, or a plain value."]]
      :examples
      [{:source    (rc/inline "frontend/examples/show.cljs")
        :component show/example}]}

     {:id    :switch
      :title "Switch & Match"
      :prose
      [:<>
       [:p "For more than two branches, " [:code "[:switch]"] " renders the "
        "first " [:code "[:match {:when …}]"] " whose condition is truthy, "
        "or the fallback when none is. " [:code ":match"] " is only valid "
        "directly inside " [:code ":switch"] "."]]
      :examples
      [{:source    (rc/inline "frontend/examples/switch.cljs")
        :component switch/example}]}

     {:id    :dynamic
      :title "Dynamic"
      :prose
      [:<>
       [:p [:code "[:dynamic {:component …}]"] " renders a component chosen "
        "at runtime — a tag string, a component fn, or an atom holding "
        "either. When the atom changes, the element is swapped while its "
        "children stay put."]]
      :examples
      [{:source    (rc/inline "frontend/examples/dynamic.cljs")
        :component dynamic/example}]}]}

   {:title "Lists"
    :pages
    [{:id    :for
      :title "Keyed lists — :for"
      :prose
      [:<>
       [:p [:code "[:for {:each xs} render-fn]"] " wraps Solid's "
        [:code "<For>"] ", which keys rows by item identity: swap!s that "
        "reuse existing items are diffed in place, and reordering moves DOM "
        "nodes instead of rebuilding them."]
       [:p "The render fn receives " [:code "(item index)"] " where "
        [:code "index"] " is a getter — call it as " [:code "(index)"]
        " inside a thunk to keep the position live. Watch the flashes when "
        "you reverse: only the numbers update, the row text doesn't."]]
      :examples
      [{:source    (rc/inline "frontend/examples/for_list.cljs")
        :component for-list/example}]}

     {:id    :index
      :title "Position-keyed lists — :index"
      :prose
      [:<>
       [:p [:code "[:index]"] " is " [:code ":for"] "'s sibling, keyed by "
        [:em "position"] " instead of identity: each row's DOM node is "
        "reused and only its content updates. Use it when items mutate in "
        "place; use " [:code ":for"] " when identities are stable and may "
        "move around."]
       [:p "Mind the flipped signature: the render fn gets "
        [:code "(item-getter index-number)"] " — the opposite of "
        [:code ":for"] "."]]
      :examples
      [{:source    (rc/inline "frontend/examples/index_list.cljs")
        :component index-list/example}]}

     {:id    :seqs
      :title "Plain sequences"
      :prose
      [:<>
       [:p "Any seq — like a " [:code "(for …)"] " comprehension — flattens "
        "into the parent's children. This renders once and is not keyed, so "
        "it's right for static lists; for changing collections reach for "
        [:code ":for"] " / " [:code ":index"] ". Add " [:code "^{:key …}"]
        " metadata to each vector to satisfy the dev-time checker."]]
      :examples
      [{:source    (rc/inline "frontend/examples/seqs.cljs")
        :component seqs/example}]}]}

   {:title "Advanced"
    :pages
    [{:id    :fragments
      :title "Fragments"
      :prose
      [:<>
       [:p [:code "[:<> …]"] " renders children as siblings with no wrapper "
        "element — essential where the parent dictates its children's tags, "
        "like " [:code "<dl>"] ", " [:code "<tr>"] " or CSS grid."]]
      :examples
      [{:source    (rc/inline "frontend/examples/fragments.cljs")
        :component fragments/example}]}

     {:id    :refs
      :title "Refs"
      :prose
      [:<>
       [:p [:code ":ref"] " takes a function that Solid calls with the DOM "
        "element once it exists. The usual pattern is stashing it in an atom "
        "— note this atom holds a DOM node, not app state, so nothing here "
        "is reactive."]]
      :examples
      [{:source    (rc/inline "frontend/examples/refs.cljs")
        :component refs/example}]}

     {:id    :portal
      :title "Portal"
      :prose
      [:<>
       [:p [:code "[:portal {:mount el}]"] " renders children into another "
        "DOM node — toasts, modals, tooltips — while they keep their place "
        "in the reactive graph: cleanup and reactivity behave as if they "
        "were still inside the component."]]
      :examples
      [{:source    (rc/inline "frontend/examples/portal.cljs")
        :component portal/example}]}

     {:id    :suspense
      :title "Suspense"
      :prose
      [:<>
       [:p [:code "createResource"] " turns an async fetch into a signal, and "
        [:code "[:suspense {:fallback …}]"] " shows the fallback until "
        "resources under it resolve. Re-fetches keep showing the stale value "
        "instead of falling back — watch the flash when the new value lands."]]
      :examples
      [{:source    (rc/inline "frontend/examples/suspense.cljs")
        :component suspense/example}]}

     {:id    :error-boundary
      :title "Error boundary"
      :prose
      [:<>
       [:p [:code "[:error-boundary]"] " catches errors thrown while "
        "rendering its children. The fallback fn receives the error and a "
        [:code "reset"] " callback that re-renders the children — clear the "
        "cause before calling it or you'll be right back."]]
      :examples
      [{:source    (rc/inline "frontend/examples/error_boundary.cljs")
        :component error-boundary/example}]}

     {:id    :js-components
      :title "JS components"
      :prose
      [:<>
       [:p [:code "[:> SomeJsComp {…} children]"] " invokes a JS Solid "
        "component: the props map becomes a JS object, fn-valued props pass "
        "as Solid accessors, and children go through as usual. Solid's own "
        "components are already exposed as keywords (" [:code ":show"] ", "
        [:code ":for"] ", …), so this is for npm libraries — the example "
        "defines its 'library' component inline against the raw hyperscript "
        "API."]]
      :examples
      [{:source    (rc/inline "frontend/examples/js_components.cljs")
        :component js-components/example}]}]}])
