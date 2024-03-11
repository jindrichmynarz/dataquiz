(ns net.mynarz.dataquiz.events
  (:require [ajax.edn :as edn]
            [kee-frame.core :as kf]
            [net.mynarz.az-kviz.logic :as az]
            [net.mynarz.dataquiz.coeffects :as cofx]
            [net.mynarz.dataquiz.effects :as fx]
            [re-frame.core :as rf]))

(def status->question-filter
  {:default (complement #{:yesno})
   :missed #{:yesno}})

(def toggle-players
  {:player-1 :player-2
   :player-2 :player-1})

(kf/reg-event-db
  ::load-questions-error
  (fn [db]
    (assoc db :error [:<> "Chyba při načítání otázek!" [:i.zmdi.zmdi-alert-circle-o]])))

(kf/reg-chain
  ::load-questions
  (fn []
    {:http-xhrio {:method :get
                  :on-failure [::load-questions-error]
                  :response-format (edn/edn-response-format)
                  :uri "questions.edn"}})
  (fn [{:keys [db]} [questions]]
    {:db (-> db
             (assoc :questions questions)
             (dissoc :loading?))}))

(kf/reg-event-db
  ::change-player-name
  (fn [db [player player-name]]
    (assoc db player player-name)))

(kf/reg-event-db
  ::start-game
  (fn [db]
    (-> db
        (dissoc :question)
        (assoc :board-state (az/init-board-state)
               :is-playing (rand-nth [:player-1 :player-2])))))

(kf/reg-event-db
  ::no-more-questions
  (fn [db]
    (assoc db :error [:<> "Otázky došly!" [:i.zmdi.zmdi-block]])))

(kf/reg-event-fx
  ::has-player-won?
  (fn [{:keys [db]}]
    (let [winner (az/who-won (:board-state db))]
      (if winner
        {:db (assoc db :winner winner)
         :fx [[:navigate-to [:verdict]]]}
        {:db db}))))

(kf/reg-event-fx
  ::next-question
  (fn [{{:keys [next-player]
         :as db} :db}]
    {:db (-> db
             (assoc :is-playing next-player)
             (dissoc :question :next-player))
     :fx [[:dispatch [::has-player-won?]]]}))

(kf/reg-event-fx
  ::answer-question
  (fn [{{current-player :is-playing
         {question-type :type} :question
         :as db} :db}
       [correct?]]
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
               (assoc-in [:question :revealed?] true))
       :fx [[::fx/cancel-timeout (get-in db [:timeout ::pick-question])]]})))

(defmulti prepare-question :type)

(defmethod prepare-question :multiple [question]
  (update question :choices shuffle))

(defmethod prepare-question :default [question]
  question)

(kf/reg-event-fx
  ::pick-question
  [(rf/inject-cofx ::cofx/timeout {:id ::pick-question
                                   :event [::answer-question]
                                   :ms 45000})]
  (fn [{::cofx/keys [timeout]
        :keys [db]}
       [status]]
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
         :dispatch [::no-more-questions]}))))

(kf/reg-event-fx
  ::click-tile
  (fn [{:keys [db]} [tile-id]]
    (let [status-path [:board-state tile-id :status]
          status (get-in db status-path)]
      (if (and (#{:default :missed} status) (nil? (:question db)))
        {:db (assoc-in db status-path :active)
         :fx [[:dispatch [::pick-question status]]]}
        {:db db}))))
