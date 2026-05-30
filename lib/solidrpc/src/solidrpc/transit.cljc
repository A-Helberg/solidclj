(ns solidrpc.transit
  (:refer-clojure :exclude [read write])
  (:require [cognitect.transit :as t])
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream])))

#?(:cljs (def ^:private writer (t/writer :json)))
#?(:cljs (def ^:private reader (t/reader :json)))

(defn write [data]
  #?(:clj  (let [out (ByteArrayOutputStream.)
                 w   (t/writer out :json)]
             (t/write w data)
             (.toString out "UTF-8"))
     :cljs (t/write writer data)))

(defn read [s]
  #?(:clj  (t/read (t/reader (if (string? s)
                                (ByteArrayInputStream. (.getBytes ^String s "UTF-8"))
                                s)
                              :json))
     :cljs (t/read reader s)))
