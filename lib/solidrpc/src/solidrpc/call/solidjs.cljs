(ns solidrpc.call.solidjs
  "SolidJS adapter: query returns a signal getter; command returns a Promise."
  (:require
   [promesa.core :as p]
   [solidrpc.stream :as stream]
   [solidrpc.transit :as transit]
   ["solid-js" :refer [createSignal createEffect onCleanup]]))

(defn- ping? [v]
  (or (= v :keepalive)
      (and (map? v)
           (#{:keepalive :ping}
            (or (:solidrpc.sse/event v) (:event v) (:type v))))))

(deftype Signal [getter loading-getter error-getter]
  IFn
  (-invoke [_] (getter))
  ILookup
  (-lookup [_ k]
    (case k
      :loading? loading-getter
      :error    error-getter
      nil))
  (-lookup [_ k not-found]
    (case k
      :loading? loading-getter
      :error    error-getter
      not-found)))

(defn query
  "Returns a Signal backed by an SSE stream. Call it as (sig) for the value,
   or use (:loading sig) / (:error sig) for state.
   Must be called inside a reactive owner (component body, createRoot, createEffect)."
  [fn-id & args]
  (assert (symbol? fn-id) "query requires a symbol")
  (let [[state set-state]     (createSignal nil)
        [loading set-loading] (createSignal true)
        [error set-error]     (createSignal nil)]
    (createEffect
     (fn []
       (set-loading true)
       (set-error nil)
       (let [argsv  (mapv (fn [a] (if (fn? a) (a) a)) args)
             cancel (stream/open fn-id argsv
                                 (fn [v]
                                   (when-not (ping? v)
                                     (set-state v)
                                     (set-loading false)))
                                 (fn [e]
                                   (set-error e)
                                   (set-loading false)
                                   (js/console.error "[rpc/query] error" (or (ex-data e) (str e)))))]
         (onCleanup cancel))))
    (->Signal (fn [] (state)) (fn [] (loading)) (fn [] (error)))))

(defn suspense
  "Shows fallback while any of the given signals are loading, then renders children.
   signals - one or more Signal values returned by query
   fallback - hiccup to show while loading
   children - hiccup to show when all signals are loaded"
  [signals fallback children]
  (let [signals (if (sequential? signals) signals [signals])]
    [:show {:when     #(not (some (fn [s] ((:loading? s))) signals))
            :fallback fallback}
     children]))

(defn command
  "Sends a POST to /api/command. Returns a Promise resolving to the result."
  [fn-id & args]
  (let [fq (cond
             (symbol? fn-id) (if (namespace fn-id)
                               (str fn-id)
                               (throw (js/Error. "command requires a qualified symbol")))
             (string? fn-id) fn-id
             :else           (throw (js/Error. "command requires a symbol or string")))]
    (-> (js/fetch "/api/command"
                  (clj->js {:method  "POST"
                            :headers {"content-type" "application/transit+json"
                                      "accept"       "application/transit+json"}
                            :body    (transit/write {:fn-name fq :args (vec args)})}))
        (p/then (fn [^js resp]
                  (-> (.text resp)
                      (p/then transit/read)
                      (p/then (fn [data]
                                (if (:ok data)
                                  (:result data)
                                  (p/rejected (ex-info "command failed" data)))))))))))
