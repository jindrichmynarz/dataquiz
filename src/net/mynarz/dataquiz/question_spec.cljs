(ns net.mynarz.dataquiz.question-spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::hiccup
  (s/or :string string?
        :element (s/cat :tag keyword?
                        :attrs (s/? map?)
                        :content (s/* ::hiccup))))

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

(s/def ::note ::hiccup)

(s/def ::question-base
  (s/keys :req-un [::text]
          :opt-un [::note]))

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
             :min-count 1
             :distinct true))
