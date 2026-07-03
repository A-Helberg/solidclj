(ns frontend.examples.seqs)

(def langs ["Clojure" "ClojureScript" "SolidJS"])

;; plain (for …) works too — it renders once, un-keyed, so add
;; ^{:key …} metadata to keep the dev-time checker happy
(defn example []
  [:ul {:class "list-disc pl-5"}
   (for [lang langs]
     ^{:key lang}
     [:li lang])])
