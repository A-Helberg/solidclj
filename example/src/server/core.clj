(ns server.core
  (:require
   [aleph.http :as http]
   [manifold.stream :as s]
   [reitit.ring :as ring]
   [taoensso.timbre :as log]
   [transit :as transit]
   [server.sse :as sse])
  (:import [java.net URLDecoder]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- resolve-api-fn
  "Resolves a fully-qualified symbol to a Var, or nil on failure."
  [sym]
  (try
    (requiring-resolve sym)
    (catch Throwable t
      (log/error t "Failed to resolve" {:sym sym})
      nil)))

(defn- ensure-stream [x]
  (if (s/stream? x)
    x
    (let [out (s/stream 1)]
      @(s/put! out x)
      (s/close! out)
      out)))

;; ---------------------------------------------------------------------------
;; Query handler — responds with an SSE stream
;; ---------------------------------------------------------------------------

(defn handle-query [req]
  (try
    (let [qs                  (some-> (:query-string req) (URLDecoder/decode "UTF-8"))
          {:keys [fn-name
                  args]}      (transit/read qs)
          sym                 (symbol fn-name)
          v                   (resolve-api-fn sym)]
      (cond
        (nil? v)
        {:status  404
         :headers {"content-type" "application/json"}
         :body    (str "{\"error\":\"not found: " fn-name "\"}")}

        (not (:api (meta v)))
        {:status  403
         :headers {"content-type" "application/json"}
         :body    (str "{\"error\":\"not an api fn: " fn-name "\"}")}

        :else
        (let [result (apply v args)
              data   (sse/manifold->sse (ensure-stream result))]
          {:status 200 :headers sse/headers :body data})))

    (catch Throwable t
      (log/error t "Query handler failed")
      {:status  400
       :headers {"content-type" "application/json"}
       :body    (str "{\"error\":\"" (.getMessage t) "\"")})))

;; ---------------------------------------------------------------------------
;; Command handler — plain HTTP POST, returns transit JSON
;; ---------------------------------------------------------------------------

(defn handle-command [req]
  (try
    (let [body               (some-> req :body slurp)
          {:keys [fn-name
                  args]}     (transit/read body)
          sym                (symbol fn-name)
          v                  (resolve-api-fn sym)]
      (cond
        (nil? v)
        {:status  404
         :headers {"content-type" "application/transit+json"}
         :body    (transit/write {:type :command/exception :ok false :fn-name fn-name
                                  :exception {:message (str "not found: " fn-name)}})}

        (not (:api (meta v)))
        {:status  403
         :headers {"content-type" "application/transit+json"}
         :body    (transit/write {:type :command/exception :ok false :fn-name fn-name
                                  :exception {:message (str "not an api fn: " fn-name)}})}

        :else
        (let [result (apply v args)]
          {:status  200
           :headers {"content-type" "application/transit+json"}
           :body    (transit/write {:type :command/result :ok true
                                    :fn-name fn-name :result result})})))

    (catch Throwable t
      (log/error t "Command handler failed")
      {:status  500
       :headers {"content-type" "application/transit+json"}
       :body    (transit/write {:type :command/exception :ok false :fn-name "<unknown>"
                                :exception {:message (.getMessage t)}})})))

;; ---------------------------------------------------------------------------
;; Router & server
;; ---------------------------------------------------------------------------

(defn index-handler [_req]
  {:status  200
   :headers {"content-type"  "text/html; charset=utf-8"
             "cache-control" "no-store, no-cache, must-revalidate, max-age=0"}
   :body    (slurp (clojure.java.io/resource "public/index.html"))})

(def handler
  (ring/ring-handler
   (ring/router
    [["/api/query"   {:get  handle-query}]
     ["/api/command" {:post handle-command}]

     ;; Static assets — each handler strips its own prefix before looking up
     ;; the classpath resource, matching how reitit's resource handler works.
     ["/js/*"    {:get (ring/create-resource-handler {:root "public/js"})}]
     ["/css/*"   {:get (ring/create-resource-handler {:root "public/css"})}]])

   ;; SPA fallback — serve index.html for all unmatched GET requests.
   index-handler))

(defonce server* (atom nil))

(defn start! []
  (let [s (http/start-server handler {:port 3000})]
    (reset! server* s)
    (log/info "Server started on http://localhost:3000")))

(defn stop! []
  (when-let [s @server*]
    (.close s)
    (reset! server* nil)
    (log/info "Server stopped")))

(defn run [_opts]
  (start!)
  @(promise))
