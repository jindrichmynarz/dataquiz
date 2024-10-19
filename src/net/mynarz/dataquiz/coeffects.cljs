(ns net.mynarz.dataquiz.coeffects
  (:require [clojure.string :as string]
            [re-frame.core :as rf]))

(defn split-by-commas
  "Split string `s` by commas."
  [s]
  (string/split s #","))

(defn load-question-ids
  "Load question IDs from string `s` into a set of integers."
  [s]
  (if s
    (->> s
         split-by-commas
         (map parse-long)
         set)
    #{}))

(rf/reg-cofx
  ::local-storage-language
  (fn [cofx]
    (assoc cofx ::local-storage-language (.getItem js/localStorage "language"))))

(rf/reg-cofx
  ::navigator-language
  (fn [cofx _]
    (when-let [language js/navigator.language]
      (assoc cofx ::navigator-language (keyword (subs language 0 2))))))

(rf/reg-cofx
  ::questions-seen
  (fn [{{:keys [question-set-id]} :db
        :as cofx}]
    (if question-set-id
      (assoc cofx
             ::questions-seen
             (->> question-set-id
                  (.getItem js/localStorage)
                  js->clj
                  load-question-ids))
      cofx)))

(rf/reg-cofx
  ::timeout
  (fn [cofx {:keys [id event ms]}]
    (assoc-in cofx
              [::timeout id]
              (js/setTimeout #(rf/dispatch event) ms))))
