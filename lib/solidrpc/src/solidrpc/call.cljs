(ns solidrpc.call
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

(defn query
  "Returns a SolidJS signal getter backed by an SSE stream.
   Must be called inside a reactive owner (component body, createRoot, createEffect).
   Signal args (fn?) are read inside the effect so the stream resubscribes when they change."
  [fn-id & args]
  (assert (symbol? fn-id) "query requires a symbol")
  (let [[state set-state] (createSignal nil)]
    (createEffect
     (fn []
       (let [argsv (mapv (fn [a] (if (fn? a) (a) a)) args)
             cancel (stream/open fn-id argsv
                                 (fn [v] (when-not (ping? v) (set-state v)))
                                 (fn [e] (js/console.error "[rpc/query] error" (or (ex-data e) (str e)))))]
         (onCleanup cancel))))
    state))

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
