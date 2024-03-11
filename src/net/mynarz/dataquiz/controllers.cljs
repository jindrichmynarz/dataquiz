(ns net.mynarz.dataquiz.controllers
  (:require [net.mynarz.dataquiz.events :as events]
            [kee-frame.core :as kf]))

(defn- route-active?
  [route]
  ; FIXME: This is awkward.
  (comp #{route} :name :data))

(kf/reg-controller
  :enter
  {:params (route-active? :enter)
   :start [::events/start-game]})

(kf/reg-controller
  :play
  {:params (constantly true)
   :start (fn [])})

(kf/reg-controller
  :verdict
  {:params (constantly true)
   :start (fn [])})
