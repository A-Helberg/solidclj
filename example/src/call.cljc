(ns call
  (:require
   [transit :as transit]
   #?(:cljs [promesa.core :as p])
   #?(:cljs ["solid-js" :refer [createSignal createEffect onCleanup]])))

;; ---------------------------------------------------------------------------
;; SSE stream
;; ---------------------------------------------------------------------------

#?(:cljs
   (defn stream
     "Returns {:subscribe (fn [on-value on-error] -> cancel-fn)} for an SSE stream."
     [fn-name args]
     (let [qs  (transit/write {:fn-name fn-name :args args})
           url (str "/api/query?" qs)]
       {:subscribe
        (fn [on-value on-error]
          (let [cancelled?   (atom false)
                attempt*     (atom 0)
                token*       (atom 0)
                src*         (atom nil)
                retry-timer* (atom nil)
                clear-timer! (fn [a]
                               (when-let [t @a]
                                 (js/clearTimeout t)
                                 (reset! a nil)))
                close!       (fn []
                               (when-let [s @src*]
                                 (.close s)
                                 (reset! src* nil)))]
            (letfn [(backoff-ms [n]
                      (let [floor 2000 cap 60000
                            raw   (min cap (* floor (js/Math.pow 2 (dec n))))
                            jit   (* 0.15 raw (- (* 2 (rand)) 1))]
                        (max floor (+ raw jit))))
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
                        (let [my-token (swap! token* inc)]
                          (when-let [old @src*] (.close old) (reset! src* nil))
                          (try
                            (let [s (js/EventSource. url)]
                              (reset! src* s)
                              (set! (.-onopen s)
                                    (fn [_]
                                      (when (and (= my-token @token*) (not @cancelled?))
                                        (reset! attempt* 0))))
                              (set! (.-onmessage s)
                                    (fn [e]
                                      (when (and (= my-token @token*) (not @cancelled?))
                                        (try
                                          (on-value (transit/read (.-data e)))
                                          (catch :default ex
                                            (on-error (ex-info "transit decode failed" {:error ex})))))))
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
                (close!)))))})))

;; ---------------------------------------------------------------------------
;; Query — returns a SolidJS signal getter backed by an SSE stream.
;;
;; Must be called inside a SolidJS reactive owner (component body, createRoot,
;; or createEffect). The subscription is cancelled automatically via onCleanup
;; when the owner is disposed.
;;
;; Reactive args: pass signal getters as args — createEffect tracks their reads
;; and re-subscribes the stream whenever they change. e.g.:
;;   (call/query `my-fn some-signal)   ;; re-subscribes when some-signal changes
;; ---------------------------------------------------------------------------

#?(:cljs
   (defn- ping? [v]
     (or (= v :keepalive)
         (and (map? v)
              (#{:keepalive :ping}
               (or (:testapp.sse/event v) (:event v) (:type v)))))))

#?(:cljs
   (defn query
     [fn-id & args]
     (assert (symbol? fn-id) "query requires a symbol")
     (let [[state set-state] (createSignal nil)]
       (createEffect
        (fn []
          ;; Reading signal args here makes this effect re-run when they change.
          (let [argsv (mapv (fn [a] (if (fn? a) (a) a)) args)
                {:keys [subscribe]} (stream fn-id argsv)
                cancel (subscribe
                        (fn [v] (when-not (ping? v) (set-state v)))
                        (fn [e] (js/console.error "[query] error" (or (ex-data e) (str e)))))]
            (onCleanup cancel))))
       state)))

;; ---------------------------------------------------------------------------
;; Command — plain HTTP POST, returns a Promise.
;; ---------------------------------------------------------------------------

#?(:cljs
   (defn command
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
                                     (p/rejected (ex-info "command failed" data))))))))))))
