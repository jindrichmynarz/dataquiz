(ns net.mynarz.dataquiz.events
  (:require [ajax.edn :as edn]
            [clj-fuzzy.jaro-winkler :refer [jaro-winkler]]
            [clojure.spec.alpha :as s]
            [net.mynarz.az-kviz.logic :as az]
            [net.mynarz.dataquiz.coeffects :as cofx]
            [net.mynarz.dataquiz.effects :as fx]
            [net.mynarz.dataquiz.normalize :refer [abbreviate normalize-answer]]
            [re-frame.core :as rf]
            [re-pressed.core :as rp]
            [reitit.frontend.controllers :as rfc]))

(def status->question-filter
  {:default (complement #{:yesno})
   :missed #{:yesno}})

(def toggle-players
  {:player-1 :player-2
   :player-2 :player-1})

(def enter-key
  {:keyCode 13})

(defn unset-question
   [db]
   (dissoc db :question :guess :next-player :answer-revealed?))

(defmulti guess-matches-answer? :type)

(defmethod guess-matches-answer? :open
  [{:keys [answer]} guess & {:keys [threshold]
                             :or {threshold 0.94}}]
  (> (jaro-winkler (normalize-answer guess)
                   (normalize-answer answer))
     threshold))

(defmethod guess-matches-answer? :percent-range
  [{:keys [numeric-answer threshold]
    :or {threshold 5}}
   guess]
  (<= (abs (- numeric-answer guess)) threshold))

(defmethod guess-matches-answer? :sort
  [{:keys [items]}
   guess]
  (= guess items))

(rf/reg-event-fx
  ::initialize
  (fn [{:keys [db]} _]
    {:db {:loading? true
          :player-1 "Hráč 1"
          :player-2 "Hráč 2"}
     :fx [[:dispatch [::rp/set-keydown-rules {:always-listen-keys [enter-key]
                                              :event-keys [[[::submit]
                                                            [enter-key]]]}]]]}))

(rf/reg-event-fx
  ::submit
  (fn [{{{{route-name :name} :data} :route
         :keys [answer-revealed? question]} :db} _]
    (when-let [event-name (cond (and (= route-name :play)
                                     question
                                     (not answer-revealed?))
                                ::answer-question

                                (and (= route-name :play)
                                     answer-revealed?)
                                ::next-question)]
      {:fx [[:dispatch [event-name]]]})))

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
        unset-question
        (dissoc :winner)
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
             unset-question)
     :fx [[:dispatch [::has-player-won?]]]}))

(rf/reg-event-db
  ::make-a-guess
  (fn [db [_ guess]]
    (assoc db :guess guess)))

(rf/reg-event-fx
  ::answer-question
  (fn [{{:keys [guess is-playing question]
         :as db} :db}
       [_ correct?]]
    (let [other-player (toggle-players is-playing)
          active-tile-id (->> db
                              :board-state
                              (filter (comp #{:active} :status))
                              first
                              :id)
          correct? (or correct?
                       (and guess (guess-matches-answer? question guess)))
          [new-tile-state next-player] (if correct?
                                         [is-playing other-player]
                                         (if (= (:type question) :yesno)
                                           [other-player is-playing]
                                           [:missed other-player]))]
      {:db (-> db
               (assoc-in [:board-state active-tile-id :status] new-tile-state)
               (assoc :next-player next-player)
               (assoc :answer-revealed? true))
       :fx [[::fx/cancel-timeout (get-in db [:timeout ::pick-question])]]})))

(defmulti prepare-question :type)

(defmethod prepare-question :multiple
  [question]
  (update question :choices shuffle))

(defmethod prepare-question :default
  [question]
  question)

(rf/reg-event-fx
  ::pick-question
  [(rf/inject-cofx ::cofx/timeout {:id ::pick-question
                                   :event [::answer-question]
                                   :ms 45000})]
  (fn [{::cofx/keys [timeout]
        :keys [db]}
       [_ status tile-id]]
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
                 (assoc-in [:timeout timeout-key] timeout-id)
                 (cond->
                   (= (:type question) :open)
                   (update-in [:board-state tile-id :text] abbreviate (:answer question))

                   (= (:type question) :sort)
                   (assoc :guess (-> question :items shuffle))))}
        {:fx [[:dispatch [::no-more-questions]]]}))))

(rf/reg-event-fx
  ::click-tile
  (fn [{:keys [db]} [_ tile-id]]
    (let [status-path [:board-state tile-id :status]
          status (get-in db status-path)]
      (if (and (#{:default :missed} status) (nil? (:question db)))
        {:db (assoc-in db status-path :active)
         :fx [[:dispatch [::pick-question status tile-id]]]}
        {:db db}))))

(rf/reg-event-db
  ::drag-start
  (fn [db [_ index]]
    (assoc-in db [:guess index :status] :dragging)))

(rf/reg-event-db
  ::drag-end
  (fn [db [_ index]]
    (update-in db [:guess index] #(dissoc % :status))))

(defn insert-after
  "Insert `item` after `index` in `items`."
  [item index items]
  (let [[before after] (->> items
                            (remove #{item})
                            (split-at index))]
    (vec (concat before [(dissoc item :status)] after))))

(rf/reg-event-db
  ::drag-drop
  (fn [{items :guess
        :as db}
       [_ index]]
    (let [dragged-item (first (filter (comp #{:dragging} :status) items))]
      (update db :guess (partial insert-after dragged-item index)))))
