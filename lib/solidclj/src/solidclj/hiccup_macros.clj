(ns solidclj.hiccup-macros)

;;; Compile-time hiccup tree transform ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; The runtime walker (`as-element`) receives *values*, not source forms, so
;;; it cannot auto-wrap `(if ...)`, `(when ...)`, `(let [...] ...)` etc. in
;;; the `(fn [])` thunks that SolidJS needs to track reactive dependencies.
;;;
;;; This namespace provides a single macro `h` that walks the hiccup literal
;;; at compile time and inserts those thunks for you.  Plain `(fn [])` forms
;;; are left alone, so you can mix both styles freely — use `h` for the outer
;;; shape and add explicit thunks where you want fine-grained reactive scopes.
;;;
;;; Rule: in child positions of a hiccup vector (after the tag and optional
;;; props map), any list-form that does NOT start with `fn` or `fn*` is
;;; wrapped as `(fn [] <form>)`.  Nested hiccup vectors are recursed into.
;;; Everything else (literals, symbols, atoms) passes through unchanged —
;;; the runtime bridge in `solidclj.hiccup` handles atoms and other values.

(declare transform-vec)

(defn- fn-form? [form]
  (and (seq? form)
       ('#{fn fn*} (first form))))

(defn- transform-child [form]
  (cond
    (vector? form)  (transform-vec form)   ;; nested hiccup — recurse
    (fn-form? form) form                   ;; explicit thunk — keep as-is
    (seq? form)     (list 'fn [] form)     ;; any other list — wrap
    :else           form))                 ;; literal / symbol / atom — pass through

(defn- transform-vec [[tag & rest]]
  (let [[props children] (if (and (seq rest) (map? (first rest)))
                           [(first rest) (next rest)]
                           [nil rest])]
    (into [tag]
          (concat
           (when props [props])
           (map transform-child children)))))

(defmacro h
  "Walk a hiccup literal at compile time, auto-wrapping list-form
  expressions in child positions as `(fn [])` reactive thunks.

  Returns hiccup data — use it anywhere you'd return a hiccup vector:

      ;; instead of:
      [:div (fn [] (if (signal) [:span (signal)] [:span \"off\"]))]

      ;; write:
      (h [:div (if (signal) [:span (signal)] [:span \"off\"])])

  Plain `(fn [] ...)` forms are left unchanged, so you can mix both
  styles when you need fine-grained reactive scopes:

      (h [:div
          (if (show?)         ;; auto-wrapped — reruns the whole branch
            (fn [] [:p (detail)])  ;; explicit — independent reactive scope
            [:p \"hidden\"])])"
  [form]
  (if (vector? form)
    (transform-vec form)
    form))
