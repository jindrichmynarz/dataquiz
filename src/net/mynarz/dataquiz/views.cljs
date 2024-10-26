(ns net.mynarz.dataquiz.views
  (:require [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [net.mynarz.az-kviz.view :as az]
            [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.question-views :as question-views]
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

(def error-modal
  (let [dispatch-modal #(rf/dispatch [::events/dispatch-error-modal])]
    (fn [tr]
      (let [{:keys [error-type error-message]} @(rf/subscribe [::subs/error])]
        (when error-type
          [rc/modal-panel
           :backdrop-on-click dispatch-modal
           :child [rc/v-box
                   :children [[rc/h-box
                               :align :center
                               :children [[:h2 (tr [(keyword "modals" (name error-type))])]
                                          [:i.zmdi.zmdi-close
                                           {:on-click dispatch-modal
                                            :title (tr [:close])}]]
                               :justify :between]
                              (when error-message
                                [:pre error-message])]]])))))

(defn board
  []
  (let [state @(rf/subscribe [::subs/board])
        board-side @(rf/subscribe [::subs/board-side])]
    [az/board (assoc board-config :side board-side) state]))

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
  [tr]
  (let [data @(rf/subscribe [::subs/question])
        answer-revealed? @(rf/subscribe [::subs/answer-revealed?])
        [event icon title] (cond answer-revealed?
                                 [[::events/next-question]
                                  [:i.zmdi.zmdi-play]
                                  (tr [:question-box/next-question])]

                                 (#{:open :percent-range :sort} (:type data))
                                 [[::events/answer-question]
                                  [:i.zmdi.zmdi-check]
                                  (tr [:question-box/guess])]

                                 :else
                                 [[::events/answer-question false]
                                  [:i.zmdi.zmdi-skip-next]
                                  (tr [:question-box/skip-question])])]
    (when data
      [:div#question-box
        (question-views/question tr data answer-revealed?)
        [:button#next
         {:on-click #(rf/dispatch event)
          :title title}
         icon]])))

(defn controls
  [tr]
  [rc/v-box
   :class "controls"
   :children [[player-name]
              [timer]
              [question-box tr]]])

(defn navigation-button
  [text href]
  [rc/hyperlink-href
   :class "button"
   :href (rfe/href href)
   :label text])

(defn lets-play
  [tr]
  (navigation-button (tr [:play]) :play))

(defn lets-play-again
  [tr]
  (navigation-button (tr [:play-again]) :enter))

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
       [rc/modal-panel
        :child [:h2.loading loading-message]])))

(defn footer
  [tr]
  [:footer
    [:p (tr [:footer/made-by]) " " [:a {:href "https://mynarz.net/#jindrich"} "Jindřich Mynarz"]]
    [:p [:a
         {:href "https://github.com/jindrichmynarz/dataquiz"}
         [:i.zmdi.zmdi-code]
         (tr [:footer/source-code])]]])

(defn creator-view
  [{creator-name :name
    creator-url :url}]
  (with-meta
    (if creator-url
      [:a {:href creator-url} creator-name]
      creator-name)
    {:key creator-name}))

(defn credits
  [tr]
  (when-let [creators (->> [::subs/creators]
                           rf/subscribe
                           deref
                           (map creator-view)
                           (interpose (str " " (tr [:credits/and]) " ")))]
    [:p.credits (tr [:credits/questions-created-by]) " " creators "."]))

(defn questions-select-tab
  [tr]
  (let [choices @(rf/subscribe [::subs/question-sets])]
    (fn [tr]
      (let [questions-url @(rf/subscribe [::subs/question-set-id])
            loading? @(rf/subscribe [::subs/questions-loading?])
            loaded? @(rf/subscribe [::subs/questions-loaded?])]
        [rc/v-box
         :children [[rc/h-box
                     :children [[rc/single-dropdown
                                 :choices choices
                                 :class "questions-picker"
                                 :disabled? loading?
                                 :model questions-url
                                 :on-change (fn [url]
                                              (rf/dispatch [::events/download-questions url]))
                                 :placeholder (tr [:questions-picker/pick-questions])]
                                (when loaded?
                                  [rc/box
                                   :child [:button
                                           {:on-click #(rf/dispatch [::events/reset-question-set questions-url])
                                            :title (tr [:questions-picker/forget-questions-played])}
                                           [:i.zmdi.zmdi-refresh]]])]]
                    (when loaded?
                      [credits tr])]
         :max-width "50%"]))))

(defn questions-upload-tab
  []
  [rc/box
   :child [:input#questions-upload
           {:accept ".edn"
            :on-change #(rf/dispatch [::events/read-questions-from-file %])
            :type "file"}]])

(defn questions-picker
  [tr]
  (let [selected-tab (r/atom ::select-tab)]
    (fn [tr]
      [rc/v-box
       :align :center
       :children [[rc/horizontal-tabs
                   :class "questions-picker-tabs"
                   :model selected-tab
                   :tabs [{:id ::select-tab
                           :label (tr [:questions-picker/pick-questions])}
                          {:id ::input-tab
                           :label (tr [:questions-picker/load-questions])}]
                   :on-change #(reset! selected-tab %)]
                  (case @selected-tab
                    ::select-tab [questions-select-tab tr]
                    ::input-tab [questions-upload-tab])]
       :class "enter"
       :min-width "50%"])))

(defn lets-enter
  [tr]
  [rc/box :child (navigation-button (tr [:play]) :enter)])

(defn board-size-selector
  [tr]
  (let [board-side (rf/subscribe [::subs/board-side])]
    [rc/h-box
     :align :center
     :children [[rc/box :child [:span (tr [:advanced-options/board-size]) ":"]]
                [rc/horizontal-bar-tabs
                 :model @board-side
                 :on-change #(rf/dispatch [::events/change-board-side %])
                 :tabs [{:id 7
                         :label (tr [:advanced-options/original-size])}
                        {:id 4
                         :label (tr [:advanced-options/small-size])}]]]
     :gap ".5em"]))

(defn advanced-options-items
  [tr shown?]
  [rc/v-box
   :attr {:class-name (when @shown? "shown")
          :id "advanced-options-items"}
   :children [[board-size-selector tr]]])

(def advanced-options
  (let [shown? (r/atom false)]
    (fn [tr]
      [rc/v-box
       :align :center
       :attr {:id "advanced-options"}
       :class (when @shown? "shown")
       :children [[rc/box :child [:label
                                  {:on-click #(swap! shown? not)}
                                  (if @shown?
                                    [:i.zmdi.zmdi-hc-fw.zmdi-chevron-down]
                                    [:i.zmdi.zmdi-hc-fw.zmdi-chevron-right])
                                  (tr [:advanced-options/label])]]
                  [advanced-options-items tr shown?]]])))

(defn lang-switch-input
  []
  (let [language-switch-checked @(rf/subscribe [::subs/language-switch-checked])]
    [:input
     {:class-name (string/join " " (cond-> ["offscreen"]
                                      language-switch-checked (conj "checked")))
      :id "lang-toggle"
      :on-change #(rf/dispatch [::events/toggle-language])
      :type "checkbox"}]))

(defn lang-switch
  [tr]
  [:p#lang-switch [:span "CS"]
                  [lang-switch-input]
                  [:label.switch {:for "lang-toggle"
                                  :title (tr [:switch-lang])}]
                  [:span "EN"]])

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
  [tr]
  (let [winner @(rf/subscribe [::subs/winner])]
    (when winner
      [rc/v-box
       :align :center
       :children [[winners-cup (get-in board-config [:tile-config :colours (:id winner)])]
                  [:h2 (tr [:winner-heading] [(:name winner)])]]
       :justify :start])))

(defn pick-questions
  [tr]
  (let [loaded? @(rf/subscribe [::subs/questions-loaded?])]
    [rc/v-box
     :align :center
     :children [[loading-modal]
                [lang-switch tr]
                [error-modal tr]
                [title]
                [questions-picker tr]
                (when loaded? [lets-enter tr])
                [footer tr]]
     :gap "2em"
     :justify :start]))

(defn enter
  [tr]
  [rc/v-box
   :align :center
   :class "enter"
   :children [[lang-switch tr]
              [title]
              [enter-player-name :player-1]
              [enter-player-name :player-2]
              [advanced-options tr]
              [rc/gap :size "1em"]
              [lets-play tr]
              [footer tr]]
   :justify :start])

(defn play
  [tr]
  [:div#play
   [error-modal tr]
   [board]
   [controls tr]])

(defn verdict
  [tr]
  [rc/h-box
   :align :center
   :children [[rc/box
               :class "board-won"
               :child [board]
               :max-width "50%"]
              [rc/v-box
               :align :center
               :children [[winner-box tr]
                          [lets-play-again tr]]
               :gap "4em"]]
   :justify :center])

(defn ui
  []
  (let [view @(rf/subscribe [::subs/view])
        tr @(rf/subscribe [::subs/tr])]
    [(or view enter) tr]))
