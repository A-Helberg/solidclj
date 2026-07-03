(ns solidrpc.stream
  "Missionary-native SSE flow. Handles :full/:patch diff reconstruction client-side.
   Browser EventSource manages reconnection automatically."
  (:require
   [missionary.core :as m]
   [solidrpc.patch :as patch]
   [solidrpc.transit :as transit]))

(defn- diff-xf
  "Stateful transducer: reconstructs full values from :full/:patch SSE events.
   Emits the full reconstructed value on each step."
  []
  (fn [rf]
    (let [prev (atom ::none)]
      (fn
        ([] (rf))
        ([r] (rf r))
        ([r [type data]]
         (let [v (if (= type :full)
                   data
                   (let [p @prev]
                     (when-not (= p ::none)
                       (patch/apply-patch p data))))]
           (if (some? v)
             (do (reset! prev v) (rf r v))
             r)))))))

(defn sse-flow
  "Returns a Missionary flow backed by an SSE connection.
   Handles :full/:patch diff reconstruction. Browser EventSource handles reconnection.
   The flow terminates when cancelled (e.g. via onCleanup)."
  [fn-name args]
  (let [qs  (transit/write {:fn-name fn-name :args args})
        url (str "/api/query?q=" (js/encodeURIComponent qs))]
    (->> (m/observe
          (fn [emit!]
            (let [es (js/EventSource. url)]
              (.addEventListener es "full"
                                 (fn [e]
                                   (try
                                     (emit! [:full (transit/read (.-data e))])
                                     (catch :default ex
                                       (js/console.error "[solidrpc.stream] transit decode failed (full)" (str ex))))))
              (.addEventListener es "patch"
                                 (fn [e]
                                   (try
                                     (emit! [:patch (transit/read (.-data e))])
                                     (catch :default ex
                                       (js/console.error "[solidrpc.stream] transit decode failed (patch)" (str ex))))))
              (.addEventListener es "exception"
                                 (fn [e]
                                   (js/console.error "[solidrpc.stream] server exception"
                                                     (try (transit/read (.-data e))
                                                          (catch :default _ (.-data e))))))
              (fn [] (.close es)))))
         (m/eduction (diff-xf)))))
