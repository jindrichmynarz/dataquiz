(ns net.mynarz.dataquiz.effects
  (:require [re-frame.core :as rf]))

(rf/reg-fx
  ::cancel-timeout
  (fn [timeout-id]
    (js/clearTimeout timeout-id)))
