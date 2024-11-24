(ns net.mynarz.dataquiz.routes
  (:require [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.spec :as spec]
            [net.mynarz.dataquiz.views :as views]
            [re-frame.core :as rf]
            [reitit.coercion.spec :as rcs]
            [reitit.frontend :as reitit]))

(def router
  (reitit/router
    ["/"
     [""
      {:name :pick-questions
       :view #'views/pick-questions}]
     ["join-game"
      {:name :join-game
       :view #'views/join-game}]
     ["enter/:game-id"
      {:name :enter
       :parameters {:path {:game-id ::spec/game-id}}
       :view #'views/enter}]
     ["lobby/:game-id"
      {:name :waiting-lobby
       :parameters {:path {:game-id ::spec/game-id}}
       :view #'views/waiting-lobby}]
     ["play"
      {:name :play
       :view #'views/play
       :controllers [{:start #(rf/dispatch [::events/start-game])}]}]
     ["verdict"
      {:name :verdict
       :view #'views/verdict}]]
    {:data {:coercion rcs/coercion}}))
