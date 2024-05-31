(ns net.mynarz.dataquiz.events-test
  (:require [net.mynarz.dataquiz.events :as events]
            [clojure.test :refer [are deftest testing]]))

(deftest insert-before
  (let [item {:text "Insert"}
        items [{} item {} {}]]
    (are [index expected] (= (events/insert-before item index items) expected)
         0 [item {} {} {}]
         1 [{} item {} {}]
         2 [{} {} item {}]
         3 [{} {} {} item])))
