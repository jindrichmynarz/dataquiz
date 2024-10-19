(ns net.mynarz.dataquiz.effects
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

(rf/reg-fx
  ::cancel-timeout
  (fn [timeout-id]
    (js/clearTimeout timeout-id)))

(rf/reg-fx
  ::delete-local-storage
  (fn [local-store-key]
    (.removeItem js/localStorage local-store-key)))

(rf/reg-fx
  ::set-local-storage-language
  (fn [language]
    (.setItem js/localStorage "language" language)))

(rf/reg-fx
  ::set-questions-seen
  (fn [[question-set-id questions-seen]]
    (->> questions-seen
         (string/join \,)
         (.setItem js/localStorage question-set-id))))

(rf/reg-fx
  ::navigate-to
  (fn [route-id]
    (rfe/navigate route-id)))
