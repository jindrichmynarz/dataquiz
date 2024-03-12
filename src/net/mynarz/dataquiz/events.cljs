(ns net.mynarz.dataquiz.events
  (:require [ajax.edn :as edn]
            [clojure.spec.alpha :as s]
            [net.mynarz.az-kviz.logic :as az]
            [net.mynarz.dataquiz.coeffects :as cofx]
            [net.mynarz.dataquiz.effects :as fx]
            [re-frame.core :as rf]
            [reitit.frontend.controllers :as rfc]))

(def status->question-filter
  {:default (complement #{:yesno})
   :missed #{:yesno}})

(def toggle-players
  {:player-1 :player-2
   :player-2 :player-1})

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    {:loading? true
     :player-1 "Hráč 1"
     :player-2 "Hráč 2"}))

(rf/reg-event-db
  ::change-route
  (fn [{old-route :route
        :as db}
       [_ new-route]]
    (->> (rfc/apply-controllers (:controllers old-route) new-route)
         (assoc new-route :controllers)
         (assoc db :route))))

(rf/reg-event-db
  ::load-questions-error
  (fn [db _]
    (assoc db :error [:<> "Chyba při načítání otázek!" [:i.zmdi.zmdi-alert-circle-o]])))

(rf/reg-event-db
  ::load-questions
  (fn [db [_ questions]]
    (-> db
        (assoc :questions questions)
        (dissoc :loading?))))

(rf/reg-event-fx
  ::download-questions
  (fn [_ _]
    {:fx [[:http-xhrio {:method :get
                        :on-failure [::load-questions-error]
                        :on-success [::load-questions]
                        :response-format (edn/edn-response-format)
                        :uri "questions.edn"}]]}))

(rf/reg-event-db
  ::change-player-name
  (fn [db [_ player player-name]]
    (assoc db player player-name)))

(rf/reg-event-db
  ::start-game
  (fn [db _]
    (-> db
        (dissoc :question :next-player :winner)
        (assoc :board-state (az/init-board-state)
               :is-playing (rand-nth [:player-1 :player-2])))))

(rf/reg-event-db
  ::no-more-questions
  (fn [db _]
    (assoc db :error [:<> "Otázky došly!" [:i.zmdi.zmdi-block]])))

(rf/reg-event-fx
  ::has-player-won?
  (fn [{:keys [db]} _]
    (let [winner (az/who-won (:board-state db))]
      (if winner
        {:db (assoc db :winner winner)
         :fx [[::fx/navigate-to :verdict]]}
        {:db db}))))

(rf/reg-event-fx
  ::next-question
  (fn [{{:keys [next-player]
         :as db} :db}
       _]
    {:db (-> db
             (assoc :is-playing next-player)
             (dissoc :question :next-player :answer-revealed?))
     :fx [[:dispatch [::has-player-won?]]]}))

(rf/reg-event-fx
  ::answer-question
  (fn [{{current-player :is-playing
         {question-type :type} :question
         :as db} :db}
       [_ correct?]]
    (let [other-player (toggle-players current-player)
          active-tile-id (->> db
                              :board-state
                              (filter (comp #{:active} :status))
                              first
                              :id)
          [new-tile-state next-player] (if correct?
                                         [current-player other-player]
                                         (if (= question-type :yesno)
                                           [other-player current-player]
                                           [:missed other-player]))]
      {:db (-> db
               (assoc-in [:board-state active-tile-id :status] new-tile-state)
               (assoc :next-player next-player)
               (assoc :answer-revealed? true))
       :fx [[::fx/cancel-timeout (get-in db [:timeout ::pick-question])]]})))

(defmulti prepare-question :type)

(defmethod prepare-question :multiple [question]
  (update question :choices shuffle))

(defmethod prepare-question :default [question]
  question)

(rf/reg-event-fx
  ::pick-question
  [(rf/inject-cofx ::cofx/timeout {:id ::pick-question
                                   :event [::answer-question]
                                   :ms 45000})]
  (fn [{::cofx/keys [timeout]
        :keys [db]}
       [_ status]]
    (let [[timeout-key timeout-id] (first timeout)
          question-filter (status->question-filter status)
          question (->> db
                        :questions
                        (filter (comp question-filter :type))
                        shuffle
                        first)]
      (if question
        {:db (-> db
                 (assoc :question (prepare-question question))
                 (update :questions #(disj % question))
                 (assoc-in [:timeout timeout-key] timeout-id))}
        {:db db
         :fx [[:dispatch [::no-more-questions]]]}))))

(rf/reg-event-fx
  ::click-tile
  (fn [{:keys [db]} [_ tile-id]]
    (let [status-path [:board-state tile-id :status]
          status (get-in db status-path)]
      (if (and (#{:default :missed} status) (nil? (:question db)))
        {:db (assoc-in db status-path :active)
         :fx [[:dispatch [::pick-question status]]]}
        {:db db}))))
