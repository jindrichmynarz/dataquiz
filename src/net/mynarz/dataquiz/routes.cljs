(ns net.mynarz.dataquiz.routes
  (:require [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.views :as views]
            [re-frame.core :as rf]
            [reitit.coercion.schema :as rsc]
            [reitit.frontend :as reitit]))

(def router
  (reitit/router
    ["/"
     [""
      {:name :pick-questions
       :view #'views/pick-questions}]
     ["enter"
      {:name :enter
       :view #'views/enter}]
     ["play"
      {:name :play
       :view #'views/play
       :controllers [{:start #(rf/dispatch [::events/start-game])}]}]
     ["verdict"
      {:name :verdict
       :view #'views/verdict}]]
    {:data {:coercion rsc/coercion}}))
