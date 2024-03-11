(ns net.mynarz.dataquiz.question-views
  (:require [net.mynarz.dataquiz.events :as events]
            [re-com.core :as rc]
            [re-frame.core :as rf]))

(defn- mark-answer
  [revealed? correct?]
  (when revealed?
    (if (and revealed? correct?)
      [:i.zmdi.zmdi-check]
      [:i.zmdi.zmdi-close])))

(defmulti question :type)

(defmethod question :yesno
  [{expected-answer :answer
    revealed? :revealed?
    text :text}]
  (letfn [(answer [actual-answer]
            #(rf/dispatch [::events/answer-question (= actual-answer expected-answer)]))]
    [rc/v-box
     :children [[:p.question text]
                [:ul.choices
                 [:li [:button
                       (if revealed?
                         {:disabled true}
                         {:on-click (answer true)})
                       "Ano"
                       (mark-answer revealed? (true? expected-answer))]]
                 [:li [:button
                       (if revealed?
                         {:disabled true}
                         {:on-click (answer false)})
                       "Ne"
                       (mark-answer revealed? (false? expected-answer))]]]]]))

(defmethod question :multiple
  [{:keys [text choices revealed?]}]
  [rc/v-box
   :children [[:div.question text]
              [:ul.choices
               (for [{choice :text correct? :correct?} choices]
                 ^{:key choice} [:li [:button
                                      (if revealed?
                                        {:disabled true}
                                        {:on-click #(rf/dispatch [::events/answer-question correct?])})
                                      choice (mark-answer revealed? correct?)]])]]])
