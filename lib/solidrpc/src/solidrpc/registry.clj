(ns solidrpc.registry)

(defonce ^:private registry (atom {}))

(defn register!
  "Register a var for remote dispatch. Call this at startup for every ^:api fn."
  [v]
  {:pre [(var? v)]}
  (swap! registry assoc (str (symbol v)) v)
  v)

(defn lookup
  "Returns the registered var for the given fully-qualified name string, or nil."
  [fn-name]
  (get @registry fn-name))
