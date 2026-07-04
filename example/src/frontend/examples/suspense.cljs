(ns frontend.examples.suspense
  (:require ["solid-js" :refer [createResource createSignal]]
            [solidclj.docs.ui :as ui]))

(defn- fetch-email
  "Stands in for a real request — resolves after 1.2s."
  [id]
  (js/Promise. (fn [resolve _]
                 (js/setTimeout #(resolve (str "user-" id "@example.com")) 1200))))

(defn example []
  (let [[user-id set-user-id] (createSignal 1)
        [email]               (createResource user-id fetch-email)]
    [:div {:class "space-y-3"}
     ;; the fallback shows while the resource loads the first time;
     ;; later loads keep the stale value until the new one resolves
     [:suspense {:fallback [:p {:class "text-gray-400"} "Loading…"]}
      (fn [] [:p {:class "font-mono"} (email)])]
     [ui/button {:on-click #(set-user-id (inc (user-id)))} "Load next user"]]))
