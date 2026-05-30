(ns solidclj.hiccup.macro-test
  (:require-macros [solidclj.hiccup-macros :refer [h]])
  (:require [cljs.test :refer-macros [deftest is testing]]
            [solidclj.hiccup :refer [render]]
            [solidclj.hiccup.test-setup :refer [fresh-root]]
            ["solid-js" :refer [createSignal]]))

;;; Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-mount [hiccup f]
  (let [root    (fresh-root)
        dispose (render hiccup root)]
    (try (f root)
         (finally (dispose)))))

;;; Compile-time structure tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; `h` returns hiccup data (vectors), so we can inspect the transformed
;;; structure directly — a wrapped list-form becomes an opaque fn value.

(deftest list-form-child-becomes-fn
  (testing "if in child position is wrapped"
    (let [result (h [:div (if true "yes" "no")])]
      (is (vector? result))
      (is (fn? (second result)))))

  (testing "when in child position is wrapped"
    (let [result (h [:div (when true [:span "x"])])]
      (is (fn? (second result)))))

  (testing "arbitrary call in child position is wrapped"
    (let [result (h [:div (str "a" "b")])]
      (is (fn? (second result))))))

(deftest fn-form-is-not-double-wrapped
  (let [thunk  (fn [] [:span "x"])
        result (h [:div thunk])]
    (testing "bare symbol passes through unchanged"
      ;; The symbol is not a seq — runtime handles it.
      (is (= thunk (second result)))))

  (testing "inline (fn [] ...) is kept as-is"
    (let [result (h [:div (fn [] [:span "x"])])]
      (is (fn? (second result)))
      ;; Call it — the fn should return hiccup, not a double-wrapped fn.
      (is (vector? ((second result)))))))

(deftest props-map-is-not-treated-as-child
  (let [result (h [:div {:class "row"} (if true "a" "b")])]
    (testing "tag is unchanged"
      (is (= :div (first result))))
    (testing "props map is unchanged"
      (is (= {:class "row"} (second result))))
    (testing "child is wrapped"
      (is (fn? (nth result 2))))))

(deftest nested-vector-is-recursed-into
  (let [result (h [:div [:span (if true "yes" "no")]])]
    (testing "outer tag unchanged"
      (is (= :div (first result))))
    (let [inner (second result)]
      (testing "inner vector preserved"
        (is (vector? inner))
        (is (= :span (first inner))))
      (testing "child of inner vector is wrapped"
        (is (fn? (second inner)))))))

(deftest literal-children-are-unchanged
  (let [result (h [:div "text" 42 :keyword nil])]
    (testing "strings, numbers, keywords, nil pass through"
      (is (= "text"    (nth result 1)))
      (is (= 42        (nth result 2)))
      (is (= :keyword  (nth result 3)))
      (is (= nil       (nth result 4))))))

;;; Reactivity tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Mount the auto-wrapped hiccup into a Solid root and verify that the DOM
;;; updates when a signal changes — the key promise of the macro.

(deftest if-branch-updates-on-signal-change
  (let [[show? set-show] (createSignal true)]
    (with-mount
      (h [:div (if (show?) [:span "on"] [:span "off"])])
      (fn [root]
        (testing "initial signal true renders first branch"
          (is (= "<div><span>on</span></div>" (.-innerHTML root))))
        (testing "signal flip renders second branch"
          (set-show false)
          (is (= "<div><span>off</span></div>" (.-innerHTML root))))
        (testing "signal flip back restores first branch"
          (set-show true)
          (is (= "<div><span>on</span></div>" (.-innerHTML root))))))))

(deftest when-appears-and-disappears-on-signal-change
  (let [[visible? set-visible] (createSignal false)]
    (with-mount
      (h [:div (when (visible?) [:p "hello"])])
      (fn [root]
        (testing "initial false renders nothing"
          (is (= "<div></div>" (.-innerHTML root))))
        (testing "signal true makes content appear"
          (set-visible true)
          (is (= "<div><p>hello</p></div>" (.-innerHTML root))))
        (testing "signal false makes content disappear"
          (set-visible false)
          (is (= "<div></div>" (.-innerHTML root))))))))

(deftest let-binding-inside-auto-wrap
  ;; (let [...] ...) in child position is a list-form → gets wrapped.
  ;; Signal reads inside the let are tracked in that reactive scope.
  (let [[value set-value] (createSignal "initial")]
    (with-mount
      (h [:div (let [v (value)] [:span v])])
      (fn [root]
        (testing "initial value renders"
          (is (= "<div><span>initial</span></div>" (.-innerHTML root))))
        (testing "signal change re-runs the let and updates DOM"
          (set-value "updated")
          (is (= "<div><span>updated</span></div>" (.-innerHTML root))))))))

(deftest explicit-fn-and-auto-wrap-coexist
  ;; Fine-grained explicit (fn []) alongside auto-wrapped expressions —
  ;; the two styles should compose without interfering.
  (let [[a set-a] (createSignal "A")
        [b set-b] (createSignal "B")]
    (with-mount
      (h [:div
          (str "prefix-" (a))         ;; auto-wrapped — reads signal a
          (fn [] [:em (b)])])          ;; explicit thunk — reads signal b
      (fn [root]
        (testing "initial render"
          (is (= "<div>prefix-A<em>B</em></div>" (.-innerHTML root))))
        (testing "changing a updates auto-wrapped child"
          (set-a "A2")
          (is (= "<div>prefix-A2<em>B</em></div>" (.-innerHTML root))))
        (testing "changing b updates explicit thunk child"
          (set-b "B2")
          (is (= "<div>prefix-A2<em>B2</em></div>" (.-innerHTML root))))))))

(deftest nested-hiccup-reactive-in-inner-span
  ;; The macro recurses into nested vectors, so an (if ...) inside a
  ;; nested [:span ...] is also auto-wrapped and reactive.
  (let [[flag set-flag] (createSignal true)]
    (with-mount
      (h [:div [:span (if (flag) "yes" "no")]])
      (fn [root]
        (testing "initial render uses first branch"
          (is (= "<div><span>yes</span></div>" (.-innerHTML root))))
        (testing "flag flip updates inner span"
          (set-flag false)
          (is (= "<div><span>no</span></div>" (.-innerHTML root))))))))
