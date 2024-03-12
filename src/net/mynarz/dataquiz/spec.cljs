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

(s/def ::answer boolean?)

(s/def ::correct? true?)

(s/def ::choice
  (s/keys :req-un [::text]
          :opt-un [::correct?]))

(s/def ::choices
  (s/and
    (s/coll-of ::choice
               :min-count 2
               :distinct true)
    (partial some :correct?)))

(defmulti question :type)

(defmethod question :yesno [_]
  (s/keys :req-un [::text
                   ::answer]))

(defmethod question :multiple [_]
  (s/keys :req-un [::text
                   ::choices]))

(s/def ::question
  (s/multi-spec question :type))

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

(s/def ::db
  (s/keys :req-un [::player-1
                   ::player-2]
          :opt-un [::az/board-state
                   ::error
                   ::is-playing
                   ::loading?
                   ::next-player
                   ::question
                   ::questions
                   ::route
                   ::winner]))
