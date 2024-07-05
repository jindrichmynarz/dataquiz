(ns net.mynarz.dataquiz.events
  (:require [ajax.edn :as edn]
            [clj-fuzzy.jaro-winkler :refer [jaro-winkler]]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.string :as gstring]
            [net.mynarz.az-kviz.logic :as az]
            [net.mynarz.dataquiz.coeffects :as cofx]
            [net.mynarz.dataquiz.effects :as fx]
            [net.mynarz.dataquiz.normalize :as normalize]
            [net.mynarz.dataquiz.spec :refer [validate-questions]]
            [re-frame.core :as rf]
            [re-pressed.core :as rp]
            [jtk-dvlp.re-frame.readfile-fx]
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
  (> (jaro-winkler (normalize/normalize-answer guess)
                   (normalize/normalize-answer answer))
     threshold))

(defmethod guess-matches-answer? :percent-range
  [{:keys [percentage threshold]
    :or {threshold 5}}
   guess]
  (<= (abs (- percentage guess)) threshold))

(defmethod guess-matches-answer? :sort
  [{:keys [items]}
   guess]
  (or (= guess items)
      ; Allow 1 transposition
      (<= (->> (map = guess items)
               (filter false?)
               count)
          2)))

(rf/reg-event-fx
  ::initialize
  (fn [{:keys [db]} _]
    {:db {:player-1 "Hráč 1"
          :player-2 "Hráč 2"
          :question-sets [{:id "questions/femquiz.edn" :label "Fem-quiz"}]}
     :fx [[:dispatch [::rp/set-keydown-rules {:always-listen-keys [enter-key]
                                              :event-keys [[[::submit]
                                                            [enter-key]]]}]]]}))

(rf/reg-event-fx
  ::submit
  (fn [{{{{route-name :name} :data} :route
         :keys [answer-revealed? question]} :db} _]
    (when-let [effect (cond (= route-name :enter)
                            [::fx/navigate-to :play]

                            (and (= route-name :play)
                                 question
                                 (not answer-revealed?))
                            [:dispatch [::answer-question]]

                            (and (= route-name :play)
                                 answer-revealed?)
                            [:dispatch [::next-question]])]
      {:fx [effect]})))

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
    (-> db
      (dissoc :questions)
      (assoc :error {:error-type :load-questions-error}))))

(rf/reg-event-db
  ::dispatch-error-modal
  (fn [db _]
    (dissoc db :error :loading?)))

(rf/reg-event-db
  ::load-questions
  (fn [db [_ questions]]
    (let [sanitized-questions (normalize/sanitize-hiccup questions)]
      (println (gstring/format "Máme %d otázek." (count sanitized-questions)))
      (-> db
          (assoc :questions sanitized-questions)
          (dissoc :loading?)))))

(rf/reg-event-fx
  ::download-questions
  (fn [{:keys [db]} [_ url]]
    {:db (assoc db :loading? true)
     :fx [[:http-xhrio {:method :get
                        :on-failure [::load-questions-error]
                        :on-success [::load-questions]
                        :response-format (edn/edn-response-format)
                        :uri url}]]}))

(rf/reg-event-fx
  ::read-questions-from-edn
  (fn [{:keys [db]} [_ [{edn :content}]]]
    (let [questions (cljs.reader/read-string edn)
          error (validate-questions questions)]
      (if (nil? error)
        {:fx [[:dispatch [::load-questions questions]]]}
        {:db (-> db
                 (dissoc :questions)
                 (assoc :error {:error-type :parse-questions-error
                                :error-message error}))}))))

(rf/reg-event-fx
  ::read-questions-from-file
  (fn [{:keys [db]} [_ file]]
    {:db (assoc db :loading? true)
     :fx [[:readfile {:files [file]
                      :on-success [::read-questions-from-edn]
                      :on-error [::load-questions-error]}]]}))

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
    (assoc db :error {:error-type :no-more-questions-error})))

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
               (assoc-in [:board-state active-tile-id :text] (str (inc active-tile-id)))
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
                   (assoc-in [:board-state tile-id :text] (normalize/abbreviate (:answer question)))

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
  ::drag-enter
  (fn [db [_ index]]
    (update-in db
               [:guess index :status]
               (fn [status]
                 (if-not (= status :dragging)
                   :drag-over
                   status)))))

(rf/reg-event-db
  ::drag-leave
  (fn [db [_ index]]
    (update-in db
               [:guess index]
               (fn [{:keys [status] :as item}]
                 (if (= status :drag-over)
                   (dissoc item :status)
                   item)))))

(rf/reg-event-db
  ::drag-end
  (fn [db [_ index]]
    (update-in db [:guess index] #(dissoc % :status))))

(defn insert-before
  "Insert `item` before `index` in `items`."
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
      (update db :guess (partial insert-before dragged-item index)))))
