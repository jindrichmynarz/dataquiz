(ns net.mynarz.dataquiz.question-views
  (:require [net.mynarz.dataquiz.events :as events]
            [re-com.core :as rc]
            [re-frame.core :as rf]))

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
  [{:keys [answer text explanation]}
   answer-revealed?]
  (letfn [(give-answer [actual-answer]
            #(rf/dispatch [::events/answer-question (= actual-answer answer)]))]
    [rc/v-box
     :children [[:p.question text]
                [:ul.choices
                 [:li [:button
                       (if answer-revealed?
                         {:disabled true}
                         {:on-click (give-answer true)})
                       "Ano"
                       (mark-answer answer-revealed? (true? answer))]]
                 [:li [:button
                       (if answer-revealed?
                         {:disabled true}
                         {:on-click (give-answer false)})
                       "Ne"
                       (mark-answer answer-revealed? (false? answer))]]]
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
