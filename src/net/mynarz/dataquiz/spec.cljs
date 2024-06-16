(ns net.mynarz.dataquiz.spec
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as e]
            [net.mynarz.az-kviz.spec :as az]
            [net.mynarz.dataquiz.questions-spec :as questions]
            [reitit.core :as reitit]))

(defn gen-one
  "Generate one value satisfying `spec`."
  [spec]
  (ffirst (s/exercise spec 1)))

(s/def ::player #{:player-1 :player-2})

(s/def ::is-playing ::player)

(s/def ::player-name string?)

(s/def ::player-1 ::player-name)

(s/def ::player-2 ::player-name)

(s/def ::next-player ::player)

(s/def ::winner ::player)

(s/def ::loading? boolean?)

(s/def ::loading-error str)

(s/def ::error ::questions/hiccup)

(s/def ::route
  (partial instance? reitit/Match))

(s/def ::guess
  (s/or :string string?
        :number number?
        :items ::questions/items))

(s/def ::answer-revealed? boolean?)

(s/def ::db
  (s/keys :req-un [::player-1
                   ::player-2]
          :opt-un [::answer-revealed?
                   ::az/board-state
                   ::error
                   ::guess
                   ::is-playing
                   ::loading?
                   ::loading-error
                   ::next-player
                   ::questions/question
                   ::questions/questions
                   ::route
                   ::winner]))

(defn validate
  [spec data]
  (when-not (s/valid? spec data)
    (e/expound-str spec data)))

(def validate-questions
  (partial validate ::questions/questions))
