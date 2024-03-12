(ns net.mynarz.dataquiz.views
  (:require [goog.string :as gstring]
            [goog.string.format]
            [net.mynarz.az-kviz.view :as az]
            [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.question-views :refer [explanation-view question]]
            [net.mynarz.dataquiz.subscriptions :as subs]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]))

(defn error-modal
  []
  (let [error @(rf/subscribe [::subs/error])]
    (when error
      (rc/modal-panel
        :child [:h2 error]))))

(defn board
  []
  (let [state @(rf/subscribe [::subs/board])]
    [az/board
     {:on-click (fn [id] (rf/dispatch [::events/click-tile id]))
      :tile-config {:colours {:player-2 "#1e60a2"}
                    :hex-shade 0.6}}
     state]))

(defn player-name
  []
  (let [[current-player player-name] @(rf/subscribe [::subs/current-player])]
    [rc/box :child [:h1#player-name {:class current-player} player-name]]))

(defn timer
  []
  (let [question @(rf/subscribe [::subs/question])
        answer-revealed? @(rf/subscribe [::subs/answer-revealed?])
        [current-player _] @(rf/subscribe [::subs/current-player])]
    [:div.timer
      (when (and question (not answer-revealed?))
        [:<>
          [:div.progress-bar {:class current-player}]
          [:div.progress-shade]])]))

(defn question-box
  []
  (let [data @(rf/subscribe [::subs/question])
        answer-revealed? @(rf/subscribe [::subs/answer-revealed?])
        [event icon title] (if answer-revealed?
                             [[::events/next-question]
                              [:i.zmdi.zmdi-play]
                              "Další otázka"]
                             [[::events/answer-question false]
                              [:i.zmdi.zmdi-skip-next]
                              "Nevím, dál!"])]
    (when data
      [:<>
        (question data answer-revealed?)
        [:button#next
         {:on-click #(rf/dispatch event)
          :title title}
         icon]])))

(defn controls
  []
  [rc/v-box
   :class "controls"
   :children [[player-name]
              [timer]
              [question-box]]])

(defn lets-play
  []
  [rc/hyperlink-href
   :class "button"
   :href (rfe/href :play)
   :label "Hrát"])

(defn enter-player-name
  [player]
  (let [player-name @(rf/subscribe [::subs/player-name player])]
    [rc/input-text
     :model player-name
     :on-change #(rf/dispatch [::events/change-player-name player %])]))

(defn title
  []
  [:h1 [:a {:href (rfe/href :enter)} "▲quiz"]])

(defn loading-modal
  []
  (let [loading-message @(rf/subscribe [::subs/loading-message])]
    (when loading-message
       (rc/modal-panel
         :child [:h2.loading loading-message]))))

(defn footer
  []
  [:footer
    [:p "Vyrobil " [:a {:href "https://mynarz.net/#jindrich"} "Jindřich Mynarz"] "."]
    [:p [:a
         {:href "https://github.com/jindrichmynarz/dataquiz"}
         [:i.zmdi.zmdi-code]
         "Zdrojový kód"]]])

(defn enter
  []
  [rc/v-box
   :align :center
   :class "enter"
   :children [[loading-modal]
              [error-modal]
              [title]
              [enter-player-name :player-1]
              [enter-player-name :player-2]
              [lets-play]
              [footer]]
   :justify :start])

(defn play
  []
  (rc/h-box
    :class "play"
    :children [[error-modal]
               [board]
               [controls]]))

(defn verdict
  []
  (let [winner @(rf/subscribe [::subs/winner])]
    (when winner
      [rc/v-box
       :align :center
       :children [[title]
                  [:h1 "🏆"]
                  [:h2 (gstring/format "Vítězem se stává: %s!" winner)]]
       :justify :start])))

(defn ui
  []
  (let [view @(rf/subscribe [::subs/view])]
    [(or view enter)]))
