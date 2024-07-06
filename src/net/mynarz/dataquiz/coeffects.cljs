(ns net.mynarz.dataquiz.coeffects
  (:require [re-frame.core :as rf]))

(rf/reg-cofx
   ::local-store
   (fn [cofx local-store-key]
      (assoc-in cofx
                [::local-store local-store-key]
                (js->clj (.getItem js/localStorage local-store-key)))))

(rf/reg-cofx
  ::timeout
  (fn [cofx {:keys [id event ms]}]
    (assoc-in cofx
              [::timeout id]
              (js/setTimeout #(rf/dispatch event) ms))))
