(ns api.clock
  (:require
   #?(:cljs [solidrpc.call.solidjs :as solidrpc])
   #?(:clj [manifold.stream :as s])))

(defn time-flow
  "Emits the current server time every second as a string.
   Server: a Manifold periodic stream.
   Client: an SSE-backed Reagent ratom via call/query."
  []
  #?(:clj  (s/periodically (long 1000)
                           (fn [] (str (java.util.Date.))))
     :cljs (solidrpc/query `time-flow)))

(defn slow-time-flow
  "Like time-flow but waits 3 seconds before emitting the first value."
  []
  #?(:clj  (let [stream (s/stream)]
             (future
               (Thread/sleep 3000)
               (s/connect (s/periodically (long 1000)
                                          (fn [] (str (java.util.Date.))))
                          stream))
             stream)
     :cljs (solidrpc/query `slow-time-flow)))

(defn echo
  "Simple command that echoes its argument back."
  [msg]
  #?(:clj  {:echo msg :at (str (java.util.Date.))}
     :cljs (solidrpc/command `echo msg)))
