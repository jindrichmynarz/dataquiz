(ns net.mynarz.dataquiz.normalize-test
  (:require [net.mynarz.dataquiz.normalize :as normalize]
            [clojure.test :refer [are deftest testing]]))

(deftest sanitize-hiccup
  (are [hiccup normalized] (= (normalize/sanitize-hiccup hiccup) normalized)
       [:div [:script {:src "https://evil.com"}] "Foo"] [:div "Foo"]))
