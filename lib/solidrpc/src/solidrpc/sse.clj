(ns solidrpc.sse
  (:require
   [manifold.deferred :as dfr]
   [manifold.stream :as s]
   [taoensso.timbre :as log]
   [solidrpc.patch :as patch]
   [solidrpc.transit :as transit]))

(def headers
  {"Content-Type"      "text/event-stream; charset=utf-8"
   "Cache-Control"     "no-cache, no-transform, no-store"
   "X-Accel-Buffering" "no"})

(defn- sse-event [event payload]
  (str "event: " (name event) "\n"
       "data: " (transit/write payload) "\n\n"))

(defn- sse-message [m]
  (cond
    (= m :keepalive)
    (sse-event :keepalive 1)

    (and (map? m) (= :exception (:solidrpc.sse/event m)))
    (sse-event :exception (:solidrpc.sse/payload m))

    ;; Full value on first emission
    (and (map? m) (= :full (:rpc/type m)))
    (sse-event :full (:rpc/data m))

    ;; Patch on subsequent emissions
    (and (map? m) (= :patch (:rpc/type m)))
    (sse-event :patch (:rpc/data m))

    :else
    (str "data: " (transit/write m) "\n\n")))

(defn- ensure-stream [flow]
  (if (s/stream? flow)
    flow
    (let [single (s/stream 1)]
      @(s/put! single flow)
      (s/close! single)
      single)))

(defn- diff-stream
  "Transforms a source stream into one that emits {:rpc/type :full/:patch :rpc/data ...}.
   First emission is always :full; subsequent ones are :patch with an edit vector."
  [source]
  (let [out  (s/stream 16)
        prev (atom ::none)
        done (s/consume
              (fn [v]
                (let [p   @prev
                      msg (if (= p ::none)
                            {:rpc/type :full  :rpc/data v}
                            {:rpc/type :patch :rpc/data (patch/diff p v)})]
                  (reset! prev v)
                  @(s/put! out msg)))
              source)]
    (dfr/on-realized done
                     (fn [_] (s/close! out))
                     (fn [_] (s/close! out)))
    out))

(defn manifold->sse
  "Wraps a Manifold stream (or single value) into an SSE output stream.
   Streams values as diffs after the first full emission.
   Sends a keepalive every 20 s. Closes cleanly when the source closes."
  ([flow] (manifold->sse flow {}))
  ([flow {:keys [keepalive-ms] :or {keepalive-ms 20000}}]
   (let [source    (diff-stream (ensure-stream flow))
         data      (s/stream 16)
         sse       (s/map sse-message data)
         out       (s/stream 16)
         _         (s/connect sse out)
         keepalive (s/periodically (long keepalive-ms) (constantly :keepalive))
         _         (s/connect keepalive data)
         done      (s/consume (fn [v]
                                (try
                                  @(s/put! data v)
                                  (catch Throwable t
                                    (log/error t "Failed to write SSE message")
                                    (s/close! out)
                                    (throw t))))
                              source)]
     (dfr/on-realized done
                      (fn [_] (s/close! out))
                      (fn [e]
                        (log/error e "SSE stream errored")
                        (when-not (s/closed? out)
                          @(s/put! out (sse-event :exception {:message (.getMessage ^Throwable e)})))))
     (s/on-closed out (fn []
                        (try (s/close! keepalive) (catch Throwable _))
                        (try (s/close! source)    (catch Throwable _))
                        (try (s/close! data)      (catch Throwable _))))
     out)))
