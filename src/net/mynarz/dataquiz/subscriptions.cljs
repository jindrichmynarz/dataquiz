(ns net.mynarz.dataquiz.subscriptions
  (:require [net.mynarz.az-kviz.spec :as az]
            [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  ::error
  (fn [{:keys [error]}]
    error))

(reg-sub
  ::loading-message
  (fn [{:keys [loading?]}]
    (when loading?
      (rand-nth ["Začínám druhou směnu..."
                 "Načítám otázky..."]))))

(reg-sub
  ::board
  (fn [{:keys [board-state]}]
    board-state))

(reg-sub
  ::player-name
  (fn [db [_ player]]
    (get db player)))

(reg-sub
  ::current-player
  (fn [{:keys [is-playing] :as db}]
    [(name is-playing)
     (db is-playing)]))

(reg-sub
  ::question
  (fn [{:keys [question]}]
    question))

(reg-sub
  ::winner
  (fn [{:keys [winner] :as db}]
    (get db winner)))
