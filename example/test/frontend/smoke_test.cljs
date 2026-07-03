(ns frontend.smoke-test
  "Renders every guide page under happy-dom — catches broken hiccup,
  bad requires, and runtime errors in the examples."
  (:require [cljs.test :refer-macros [deftest is testing]]
            [frontend.test-setup :refer [fresh-root]]
            [solidclj.api :as s]
            [frontend.app :as app]
            [frontend.pages :as pages]))

(deftest every-page-renders
  (doseq [{:keys [pages]} pages/sections
          page            pages]
    (testing (name (:id page))
      (let [root    (fresh-root)
            dispose (s/render [app/page-view page] root)]
        (try
          (is (pos? (.. root -innerHTML -length))
              (str (:id page) " should render non-empty HTML"))
          (is (.includes (.-textContent root) (:title page))
              (str (:id page) " should include its title"))
          (finally (dispose)))))))

(deftest example-source-is-syntax-highlighted
  (let [page    (->> pages/sections (mapcat :pages) (some #(when (= :components (:id %)) %)))
        root    (fresh-root)
        dispose (s/render [app/page-view page] root)]
    (try
      (is (.includes (.-innerHTML root) "hljs-")
          "code block should contain highlight.js token spans")
      (finally (dispose)))))

(deftest app-shell-renders-sidebar-and-home
  (let [root    (fresh-root)
        dispose (s/render [app/app] root)]
    (try
      (let [html (.-innerHTML root)]
        (is (.includes html "solidclj") "brand in sidebar")
        (is (.includes html "Why solidclj?") "home page selected by default")
        (is (.includes html "Flash DOM updates") "flash toggle present"))
      (finally (dispose)))))
