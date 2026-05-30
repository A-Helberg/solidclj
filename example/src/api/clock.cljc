(ns api.clock
  (:require
   #?(:cljs [solidrpc.call :as solidrpc])
   #?(:clj [manifold.stream :as s])))

(defn ^:api time-flow
  "Emits the current server time every second as a string.
   Server: a Manifold periodic stream.
   Client: an SSE-backed Reagent ratom via call/query."
  []
  #?(:clj  (s/periodically (long 1000)
                           (fn [] (str (java.util.Date.))))
     :cljs (solidrpc/query `time-flow)))

(defn ^:api echo
  "Simple command that echoes its argument back."
  [msg]
  #?(:clj  {:echo msg :at (str (java.util.Date.))}
     :cljs (solidrpc/command `echo msg)))
