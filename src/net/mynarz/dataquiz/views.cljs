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

(def board-config
  {:on-click (fn [id] (rf/dispatch [::events/click-tile id]))
   :tile-config {:colours {:player-1 "#e40000"
                           :player-2 "#1e60a2"}
                 :hex-shade 0.6}})

(defn error-modal
  []
  (let [error @(rf/subscribe [::subs/error])]
    (when error
      (rc/modal-panel
        :child [:h2 error]))))

(defn board
  []
  (let [state @(rf/subscribe [::subs/board])]
    [az/board board-config state]))

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
        [event icon title] (cond answer-revealed?
                                 [[::events/next-question]
                                  [:i.zmdi.zmdi-play]
                                  "Další otázka"]

                                 (#{:open :percent-range :sort} (:type data))
                                 [[::events/answer-question]
                                  [:i.zmdi.zmdi-check]
                                  "Hádej"]

                                 :else
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

(defn navigation-button
  [text href]
  [rc/hyperlink-href
   :class "button"
   :href (rfe/href href)
   :label text])

(defn lets-play
  []
  (navigation-button "Hrát" :play))

(defn lets-play-again
  []
  (navigation-button "Hrát znovu" :enter))

(defn enter-player-name
  [player]
  (let [player-name @(rf/subscribe [::subs/player-name player])]
    [rc/input-text
     :class "input-player-name"
     :model player-name
     :on-change #(rf/dispatch [::events/change-player-name player %])]))

(defn title
  []
  [:h1 [:a {:href (rfe/href :pick-questions)} "▲quiz"]])

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

(defn questions-picker
  []
  (let [questions-url (r/atom nil)]
    (fn []
      (let [loaded? @(rf/subscribe [::subs/questions-loaded?])
            loading? @(rf/subscribe [::subs/questions-loading?])
            choices [{:id "questions/femquiz.edn" :label "Fem-quiz"}]]
        [rc/box
         :child [rc/single-dropdown
                 :choices choices
                 :class "questions-picker"
                 :disabled? loading?
                 :model questions-url
                 :on-change (fn [url]
                              (reset! questions-url url)
                              (rf/dispatch [::events/download-questions url]))
                 :placeholder "Vyber otázky"]]))))

(defn lets-enter
  []
  (navigation-button "Hrát" :enter))

(defn pick-questions
  []
  (let [loaded? @(rf/subscribe [::subs/questions-loaded?])]
    [rc/v-box
     :align :center
     :children [[loading-modal]
                [error-modal]
                [title]
                [questions-picker]
                (when loaded? [lets-enter])
                [footer]]
     :gap "2em"
     :justify :center]))

(defn enter
  []
  [rc/v-box
   :align :center
   :class "enter"
   :children [[title]
              [enter-player-name :player-1]
              [enter-player-name :player-2]
              [lets-play]
              [footer]]
   :justify :start])

(defn winners-cup
  "Winner's cup coloured with the colour of the winning player."
  [colour]
  [:svg {:width 250 :height 250}
    [:defs
     [:linearGradient
       {:gradientUnits "userSpaceOnUse"
        :id "cup-gradient"
        :x1 0 :y1 0
        :x2 177.79153 :y2 96.346634}
       [:stop {:offset 0
               :style {:stop-color colour
                       :stop-opacity 0}}]
       [:stop {:offset 1
               :style {:stop-color colour
                       :stop-opacity 1}}]]]
    [:path {:d "m 23.094464,-2e-5 23.522065,57.01846 -12.801831,22.16903 45.725784,79.20482 8.89883,0 9.488616,23.019 15.351792,0 0,35.95961 0.0694,0 -33.773938,16.3058 0.50306,16.3233 90.688298,0 0.15613,-16.3233 -33.75659,-16.3058 0.0693,0 0,-35.95961 15.21301,0 9.50598,-23.019 9.03761,0 L 216.70041,79.18747 203.88122,56.9664 227.42063,-2e-5 l -56.42865,0 -91.451568,0 -56.446015,0 z m 175.981796,68.58867 6.12336,10.59882 -36.9657,64.04383 30.84234,-74.64265 z m -147.654708,0.0694 30.44338,73.81003 -36.532055,-63.28059 6.088675,-10.52944 z"
            :id "cup"
            :style {:fill "url(#cup-gradient)"}}]
    [:path {:d "M 34.526818,-51.55751 52.996969,21.61337 -3.3828659,-29.67722 47.907724,26.68531 -25.263158,8.21516 47.336482,28.79717 -25.263158,51.97574 49.223311,35.73862 -3.3828669,89.86811 50.746623,37.26193 34.526818,111.7484 57.688076,39.14876 78.270092,111.7484 59.79994,38.56021 116.17977,89.86811 64.889185,33.48828 138.04275,51.97574 65.443117,31.39373 138.04275,8.21516 63.573598,24.43496 116.17977,-29.67722 62.050286,22.91165 78.270092,-51.55751 55.091523,21.04213 34.526818,-51.55751 z"
            :id "shine"
            :style {:fill "#fff"}}]])

(defn winner-box
  []
  (let [winner @(rf/subscribe [::subs/winner])]
    (when winner
      [rc/v-box
       :align :center
       :children [[winners-cup (get-in board-config [:tile-config :colours (:id winner)])]
                  [:h2 (gstring/format "Vítězem se stává %s!" (:name winner))]]
       :justify :start])))

(defn verdict
  []
  (rc/h-box
    :align :center
    :children [[rc/box
                :class "board-won"
                :child [board]
                :max-width "50%"]
               [rc/v-box
                :align :center
                :children [[winner-box]
                           [lets-play-again]]
                :gap "4em"]]
    :justify :center))

(defn play
  []
  (rc/h-box
    :children [[error-modal]
               [board]
               [controls]]))

(defn ui
  []
  (let [view @(rf/subscribe [::subs/view])]
    [(or view enter)]))
