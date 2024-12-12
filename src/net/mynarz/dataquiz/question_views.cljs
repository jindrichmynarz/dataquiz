(ns net.mynarz.dataquiz.question-views
  (:require [goog.string :as gstring]
            [goog.string.format]
            [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.subscriptions :as subs]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(defn note-view
  [note]
  [rc/h-box
   :align :center
   :class "note"
   :children [[rc/box
               :child [:i.zmdi.zmdi-info-outline]]
              (if (vector? note)
                note
                [:div note])]
   :gap ".5em"])

(defn- mark-answer
  [revealed? correct?]
  (when revealed?
    (if (and revealed? correct?)
      [:i.zmdi.zmdi-check]
      [:i.zmdi.zmdi-close])))

(defmulti question (fn [_ question] (:type question)))

(defmethod question :yesno
  [tr
   {:keys [correct? text note]}
   answer-revealed?]
  (letfn [(answer [guess]
            #(rf/dispatch [::events/answer-question (= guess correct?)]))]
    [rc/v-box
     :children [[:div.question text]
                [:ul.choices
                 [:li [:button
                       (if answer-revealed?
                         {:disabled true}
                         {:on-click (answer true)})
                       (tr [:question.yesno/yes])
                       (mark-answer answer-revealed? (true? correct?))]]
                 [:li [:button
                       (if answer-revealed?
                         {:disabled true}
                         {:on-click (answer false)})
                       (tr [:question.yesno/no])
                       (mark-answer answer-revealed? (false? correct?))]]]
                (when (and answer-revealed? note)
                  [note-view note])]]))

(defmethod question :multiple
  [tr
   {:keys [text choices note]}
   answer-revealed?]
  [rc/v-box
   :children [[:div.question text]
              [:ul.choices
               (for [{choice :text correct? :correct?} choices]
                 ^{:key choice} [:li [:button
                                      (if answer-revealed?
                                        {:disabled true}
                                        {:on-click #(rf/dispatch [::events/answer-question correct?])})
                                      choice (mark-answer answer-revealed? correct?)]])]
              (when (and answer-revealed? note)
                [note-view note])]])

(defmethod question :open
  [tr
   {:keys [text answer]}
   answer-revealed?]
  (let [guess @(rf/subscribe [::subs/guess])]
    [rc/v-box
     :children [[:div.question text]
                [rc/input-text
                 :attr {:auto-focus true}
                 :change-on-blur? false
                 :class "guess"
                 :disabled? answer-revealed?
                 :model guess
                 :on-change #(rf/dispatch [::events/make-a-guess %])
                 :width "100%"]
                (when answer-revealed?
                  [note-view (tr [:question.open/correct-answer] [answer])])]]))

(defn slider
  [guess answer-revealed?]
  (let [guess-or-default (or guess 50)
        [current-player _] @(rf/subscribe [::subs/current-player])]
    [rc/h-box
     :align :center
     :class "slider"
     :children [[rc/slider
                 :class current-player
                 :disabled? answer-revealed?
                 :max 100
                 :min 0
                 :model guess-or-default
                 :on-change #(rf/dispatch [::events/make-a-guess %])
                 :parts {:wrapper {:style {:flex "1 1 auto"}}}
                 :width "100%"]
                [rc/box
                 :child [:span (gstring/format "%d %%" guess-or-default)]
                 :width "3em"]]]))

(defmethod question :percent-range
  [tr
   {:keys [text percentage]}
   answer-revealed?]
  (let [guess @(rf/subscribe [::subs/guess])]
    [rc/v-box
     :children [[:div.question text]
                [slider guess answer-revealed?]
                (when answer-revealed?
                  [note-view (tr [:question.percent-range/correct-answer] [percentage])])]]))

(defn sortable-list
  [items answer-revealed?]
  [:ol.sortable-list
   (for [[index {:keys [status sort-value text]}] (map-indexed vector items)]
     ^{:key index}
     [:li
      {:class [(when (= status :dragging) (name status))]
       :draggable (not answer-revealed?)
       :on-drag-start #(rf/dispatch [::events/drag-start index])
       :on-drag-over #(.preventDefault %) ; Otherwise :on-drop doesn't fire.
       :on-drop #(rf/dispatch [::events/drag-drop index])
       :on-drag-end #(rf/dispatch [::events/drag-end index])
       :style {:cursor (if answer-revealed? "initial" "grab")}}
      [rc/h-box
       :align :center
       :children [(if (vector? text)
                    text
                    [:div text])
                  (when (and answer-revealed? sort-value)
                    [:div.sort-value sort-value])]
       :justify :between]])])

(defmethod question :sort
  [tr
   {:keys [text]}
   answer-revealed?]
  (let [items @(rf/subscribe [::subs/guess])]
    [rc/v-box
     :children [[:div.question text]
                [sortable-list items answer-revealed?]]]))
