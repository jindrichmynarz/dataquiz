(ns net.mynarz.dataquiz.spec
  (:require [clojure.spec.alpha :as s]
            [net.mynarz.az-kviz.spec :as az]
            [reitit.core :as reitit]))

(s/def ::hiccup
  (s/or :string string?
        :element (s/cat :tag keyword?
                        :attrs (s/? map?)
                        :content (s/* ::hiccup))))

(s/def ::player #{:player-1 :player-2})

(s/def ::is-playing ::player)

(s/def ::player-name string?)

(s/def ::player-1 ::player-name)

(s/def ::player-2 ::player-name)

(s/def ::text ::hiccup)

(s/def ::correct? boolean?)

(s/def ::answer string?)

(s/def ::numeric-answer number?)

(s/def ::choice
  (s/keys :req-un [::text]
          :opt-un [::correct?]))

(s/def ::choices
  (s/and
    (s/coll-of ::choice
               :min-count 2
               :distinct true)
    (partial some :correct?)))

(s/def ::explanation ::hiccup)

(s/def ::question-base
  (s/keys :req-un [::text]
          :opt-un [::explanation]))

(defmulti question :type)

(defmethod question :yesno [_]
  (s/keys :req-un [::correct?]))

(defmethod question :multiple [_]
  (s/keys :req-un [::choices]))

(defmethod question :open [_]
  (s/keys :req-un [::answer]))

(s/def ::threshold
  (s/and number? pos?))

(defmethod question :percent-range [_]
  (s/keys :req-un [::numeric-answer]
          :opt-un [::threshold]))

(s/def ::sort-value
  number?)

(s/def ::item
  (s/keys :req-un [::text]
          :opt-un [::sort-value]))

(s/def ::items
  (s/coll-of ::item
             :kind vector?
             :min-count 2
             :distinct true))

(defmethod question :sort [_]
  (s/keys :req-un [::items]))

(s/def ::question
  (s/and
    ::question-base
    (s/multi-spec question :type)))

(s/def ::questions
  (s/coll-of ::question
             :kind set?
             :min-count 1))

(s/def ::next-player ::player)

(s/def ::winner ::player)

(s/def ::loading? boolean?)

(s/def ::error ::hiccup)

(s/def ::route
  (partial instance? reitit/Match))

(s/def ::guess
  (s/or :string string?
        :number number?
        :items ::items))

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
                   ::next-player
                   ::question
                   ::questions
                   ::route
                   ::winner]))
