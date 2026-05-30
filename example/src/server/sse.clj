(ns server.sse
  (:require
   [manifold.deferred :as dfr]
   [manifold.stream :as s]
   [taoensso.timbre :as log]
   [transit :as transit]))

(def headers
  {"Content-Type"     "text/event-stream; charset=utf-8"
   "Cache-Control"    "no-cache, no-transform, no-store"
   "X-Accel-Buffering" "no"})

(defn- sse-event [event payload]
  (str "event: " (name event) "\n"
       "data: " (transit/write payload) "\n\n"))

(defn- sse-message [m]
  (cond
    (= m :keepalive)
    (sse-event :keepalive 1)

    (and (map? m) (= :exception (:testapp.sse/event m)))
    (sse-event :exception (:testapp.sse/payload m))

    :else
    (str "data: " (transit/write m) "\n\n")))

(defn- ensure-stream [flow]
  (if (s/stream? flow)
    flow
    (let [single (s/stream 1)]
      @(s/put! single flow)
      (s/close! single)
      single)))

(defn manifold->sse
  "Wraps a manifold stream (or single value) into an SSE output stream.
   Sends a keepalive every 20 seconds. Closes cleanly when source closes."
  [flow]
  (let [data      (s/stream 16)
        sse       (s/map sse-message data)
        out       (s/stream 16)
        _         (s/connect sse out)
        source    (ensure-stream flow)
        keepalive (s/periodically (long 20000) (constantly :keepalive))
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
    out))
