(ns net.mynarz.dataquiz.coeffects
  (:require [re-frame.core :as rf]))

(rf/reg-cofx
  ::origin
  (fn [cofx _]
    (assoc cofx ::origin js/window.location.origin)))

(rf/reg-cofx
  ::timeout
  (fn [cofx {:keys [id event ms]}]
    (assoc-in cofx
              [::timeout id]
              (js/setTimeout #(rf/dispatch event) ms))))
