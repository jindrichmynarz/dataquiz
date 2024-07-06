(ns net.mynarz.dataquiz.effects
  (:require [re-frame.core :as rf]
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
  ::navigate-to
  (fn [route-id]
    (rfe/navigate route-id)))
