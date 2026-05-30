(ns solidrpc.stream
  "Framework-agnostic EventSource wrapper with exponential backoff reconnect.
   Handles :full and :patch SSE events, reconstructing values from diffs client-side."
  (:require
   [solidrpc.patch :as patch]
   [solidrpc.transit :as transit]))

(defn- backoff-ms [attempt]
  (let [floor 2000
        cap   60000
        raw   (min cap (* floor (js/Math.pow 2 (dec attempt))))
        jit   (* 0.15 raw (- (* 2 (rand)) 1))]
    (max floor (+ raw jit))))

(defn open
  "Opens an SSE stream for the given fully-qualified fn symbol and args vector.
   Returns a cancel-fn that closes the connection and stops reconnect."
  [fn-name args on-value on-error]
  (let [qs           (transit/write {:fn-name fn-name :args args})
        url          (str "/api/query?q=" (js/encodeURIComponent qs))
        cancelled?   (atom false)
        attempt*     (atom 0)
        token*       (atom 0)
        src*         (atom nil)
        retry-timer* (atom nil)
        ;; Holds the last reconstructed value so patches can be applied.
        ;; Reset to ::none on each reconnect so the next :full resets state cleanly.
        prev*        (atom ::none)
        clear-timer! (fn [a]
                       (when-let [t @a]
                         (js/clearTimeout t)
                         (reset! a nil)))
        close!       (fn []
                       (when-let [s @src*]
                         (.close s)
                         (reset! src* nil)))]
    (letfn [(handle-full [data]
              (reset! prev* data)
              (on-value data))
            (handle-patch [edits]
              (let [p @prev*]
                (if (= p ::none)
                  ;; Received a patch before a full — shouldn't happen, but ignore safely.
                  (on-error (ex-info "patch received before full value" {}))
                  (let [next (patch/apply-patch p edits)]
                    (reset! prev* next)
                    (on-value next)))))
            (schedule-reconnect! []
              (when (and (not @cancelled?) (not @retry-timer*))
                (let [n (swap! attempt* inc)
                      t (js/setTimeout
                         (fn []
                           (reset! retry-timer* nil)
                           (when-not @cancelled? (connect!)))
                         (backoff-ms n))]
                  (reset! retry-timer* t))))
            (connect! []
              (when-not @cancelled?
                (clear-timer! retry-timer*)
                (reset! prev* ::none)
                (let [my-token (swap! token* inc)]
                  (when-let [old @src*] (.close old) (reset! src* nil))
                  (try
                    (let [s (js/EventSource. url)]
                      (reset! src* s)
                      (set! (.-onopen s)
                            (fn [_]
                              (when (and (= my-token @token*) (not @cancelled?))
                                (reset! attempt* 0))))
                      ;; Plain `message` events are not used — all data comes via named events.
                      (.addEventListener s "full"
                                         (fn [e]
                                           (when (and (= my-token @token*) (not @cancelled?))
                                             (try
                                               (handle-full (transit/read (.-data e)))
                                               (catch :default ex
                                                 (on-error (ex-info "transit decode failed (full)" {:error ex})))))))
                      (.addEventListener s "patch"
                                         (fn [e]
                                           (when (and (= my-token @token*) (not @cancelled?))
                                             (try
                                               (handle-patch (transit/read (.-data e)))
                                               (catch :default ex
                                                 (on-error (ex-info "transit decode failed (patch)" {:error ex})))))))
                      (.addEventListener s "exception"
                                         (fn [e]
                                           (when (and (= my-token @token*) (not @cancelled?))
                                             (try
                                               (on-error (ex-info "SSE exception"
                                                                   {:payload (transit/read (.-data e))}))
                                               (catch :default ex
                                                 (on-error (ex-info "SSE exception parse" {:error ex})))))))
                      (set! (.-onerror s)
                            (fn [_]
                              (when (and (= my-token @token*) (not @cancelled?))
                                (when (identical? @src* s)
                                  (.close s)
                                  (reset! src* nil))
                                (schedule-reconnect!)))))
                    (catch :default ex
                      (on-error (ex-info "EventSource creation failed" {:error ex}))
                      (schedule-reconnect!))))))]
      (connect!)
      (fn cancel []
        (reset! cancelled? true)
        (clear-timer! retry-timer*)
        (close!)))))

(defn stream
  "Returns a subscribable {:subscribe (fn [on-value on-error] -> cancel-fn)}."
  [fn-name args]
  {:subscribe (fn [on-value on-error]
                (open fn-name args on-value on-error))})
