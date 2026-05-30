(ns solidclj.hiccup.smoke-test
  (:require [cljs.test :refer-macros [deftest is]]
            [solidclj.hiccup.test-setup]))

(deftest happy-dom-installed
  (is (some? js/document))
  (is (some? (.createElement js/document "div"))))
