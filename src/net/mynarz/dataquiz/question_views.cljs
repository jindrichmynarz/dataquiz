(ns net.mynarz.dataquiz.question-views
  (:require [goog.string :as gstring]
            [goog.string.format]
            [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.subscriptions :as subs]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(defn explanation-view
  [explanation]
  [rc/h-box
   :align :center
   :class "explanation"
   :children [[rc/box
               :child [:i.zmdi.zmdi-info-outline]]
              (if (string? explanation)
                [:div explanation]
                [explanation])]
   :gap ".5em"])

(defn- mark-answer
  [revealed? correct?]
  (when revealed?
    (if (and revealed? correct?)
      [:i.zmdi.zmdi-check]
      [:i.zmdi.zmdi-close])))

(defmulti question :type)

(defmethod question :yesno
  [{:keys [correct? text explanation]}
   answer-revealed?]
  (letfn [(answer [guess]
            #(rf/dispatch [::events/answer-question (= guess correct?)]))]
    [rc/v-box
     :children [[:p.question text]
                [:ul.choices
                 [:li [:button
                       (if answer-revealed?
                         {:disabled true}
                         {:on-click (answer true)})
                       "Ano"
                       (mark-answer answer-revealed? (true? correct?))]]
                 [:li [:button
                       (if answer-revealed?
                         {:disabled true}
                         {:on-click (answer false)})
                       "Ne"
                       (mark-answer answer-revealed? (false? correct?))]]]
                (when (and answer-revealed? explanation)
                  [explanation-view explanation])]]))

(defmethod question :multiple
  [{:keys [text choices explanation]}
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
              (when (and answer-revealed? explanation)
                [explanation-view explanation])]])

(defmethod question :open
  [{:keys [text answer]}
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
                  [explanation-view (gstring/format "Správná odpověď je %s." answer)])]]))

(defn calculate-offset
  [[left right] percentage]
  (let [range-knob-radius 5]
    (->> (/ percentage 100)
         (* (- right left (* range-knob-radius 2)))
         (+ left range-knob-radius)
         js/Math.round
         (gstring/format "%dpx"))))

(defn slider
  [guess answer-revealed?]
  (let [sides (atom nil)
        set-sides! (fn [node]
                     (when node
                       (let [bounding-rect (.getBoundingClientRect node)]
                         (reset! sides [(.-left bounding-rect) (.-right bounding-rect)]))))]
    (fn [guess answer-revealed?]
      [rc/popover-anchor-wrapper
       :attr {:ref set-sides!}
       :class "guess"
       :position :below-center
       :showing? true
       :anchor [rc/slider
                :disabled? answer-revealed?
                :max 100
                :min 0
                :model (or guess 50)
                :on-change #(rf/dispatch [::events/make-a-guess %])
                :parts {:wrapper {:style {:width "100%"}}}
                :width "100%"]
       :popover [rc/popover-content-wrapper
                 :body (gstring/format "%d %%" guess)
                 :close-button? false
                 :on-cancel nil
                 :style {:position "fixed"
                         :left (calculate-offset @sides guess)}]])))

(defmethod question :percent-range
  [{:keys [text numeric-answer]}
   answer-revealed?]
  (let [guess @(rf/subscribe [::subs/guess])]
    [rc/v-box
     :children [[:div.question text]
                [slider guess answer-revealed?]
                (when answer-revealed?
                  [explanation-view (gstring/format "Správná odpověď je %d %%." numeric-answer)])]]))
