(ns solidclj.react
  "Bridge for rendering React / Reagent components inside a solidclj.hiccup
  / SolidJS tree.

  Usage — React (leaf component)
  --------------------------------
      [react/component DatePicker {:value date-atom :onChange handler}]

  Usage — React (component with children)
  ----------------------------------------
  Use `react/el` to build the React sub-tree, then pass children as extra
  positional args to `react/component`:

      [react/component LineChart {:width 600 :height 300 :data data-atom}
       (react/el XAxis   {:dataKey \"name\"})
       (react/el YAxis   {})
       (react/el Tooltip {})
       (react/el Line    {:dataKey \"uv\" :stroke \"#2563eb\"})]

  `react/el` mirrors `React.createElement` but accepts a CLJS props map.
  Children passed to `react/el` are React elements (returned by other
  `react/el` calls), not hiccup.

  Props
  -----
  The props map passed to `component` / `reagent-component` may contain:
    - Static scalars — captured at mount, never updated.
    - Atoms — watched; root.render() is called on every change.
    - Functions — passed through as-is (event handlers, render props, …).

  Props passed to `react/el` are resolved once at call time (no atom
  bridging — el produces a static React element description).

  Lifecycle
  ---------
  A <div> container is inserted by SolidJS; the React/Reagent root mounts
  into it after the DOM is ready (`onMount`). When the enclosing SolidJS
  owner disposes, the root is unmounted and atom watchers are removed."
  (:require ["react" :refer [createElement]]
            ["react-dom/client" :refer [createRoot]]
            ["solid-js" :refer [onMount onCleanup]]
            ["solid-js/h" :as solid-h]))

(defn- atom-like? [x]
  (and (satisfies? IDeref x)
       (satisfies? IWatchable x)))

(defn- resolve-props
  "Returns a plain CLJS map with atom values replaced by their current @val."
  [props]
  (reduce-kv (fn [m k v]
               (assoc m k (if (atom-like? v) @v v)))
             {} props))

(defn el
  "Creates a React element for use as a child of `component`.
  Props is a CLJS map (converted to JS); children are React elements.

      (react/el Line {:dataKey \"uv\" :stroke \"#2563eb\"})"
  [js-comp props & children]
  (apply createElement js-comp (clj->js props) children))

(defn mount-bridge
  "Shared mounting logic. `make-root` constructs the React/Reagent root from
  the container div; `make-element` receives resolved props and returns the
  React element for root.render(). Children are captured in make-element's
  closure and remain stable across re-renders."
  [props make-root make-element]
  (solid-h
   (fn [_]
     (let [container (js/document.createElement "div")
           root*     (volatile! nil)]
       (letfn [(do-render []
                 (.render ^js @root* (make-element (resolve-props props))))]
         (onMount
          (fn []
            (vreset! root* (make-root container))
            (do-render)
            (doseq [[_k v] props :when (atom-like? v)]
              (let [wk (gensym "solidclj.react/watch-")]
                (add-watch v wk (fn [_ _ _ _] (do-render)))
                (onCleanup #(remove-watch v wk))))
            (onCleanup #(.unmount ^js @root*)))))
       container))
   nil))

(defn component
  "Renders a React component into the SolidJS tree. Children (if any) should
  be React elements produced by `react/el`.

      ;; No children
      [react/component MyComp {:value counter-atom}]

      ;; With children
      [react/component LineChart {:data data-atom :width 600 :height 300}
       (react/el XAxis {:dataKey \"name\"})
       (react/el Line  {:dataKey \"uv\" :stroke \"#2563eb\"})]"
  [js-comp props & children]
  (mount-bridge props
                createRoot
                (fn [resolved]
                  (apply createElement js-comp (clj->js resolved) children))))

