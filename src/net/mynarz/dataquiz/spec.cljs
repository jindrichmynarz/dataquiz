(ns net.mynarz.dataquiz.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test.check.generators :as gen]
            [expound.alpha :as e]
            [net.mynarz.az-kviz.spec :as az]
            [net.mynarz.dataquiz.question-spec :as question]
            [reitit.core :as reitit]))

(def game-id-length 8)

(defn game-id-gen
  "A generator of game IDs."
  []
  (gen/fmap (comp string/upper-case (partial apply str))
            (gen/vector gen/char-alpha game-id-length)))

(defn gen-one
  "Generate one value satisfying `spec`."
  [spec]
  (ffirst (s/exercise spec 1)))

(defn iri?
  "Test if `s` is a valid IRI."
  [s]
  (try (boolean (js/URL. s))
       (catch js/TypeError _ false)))

(defn validate
  [spec data]
  (when-not (s/valid? spec data)
    (e/expound-str spec data)))

(def validate-questions
  (partial validate ::question/questions))

(s/def ::player #{:player-1 :player-2})

(s/def ::is-playing ::player)

(s/def ::player-name string?)

(s/def ::player-1 ::player-name)

(s/def ::player-2 ::player-name)

(s/def ::id string?)

(s/def ::label string?)

(s/def ::question-set
  (s/keys :req-un [::id
                   ::label]))

(s/def ::question-set-id string?)

(s/def ::question-sets
  (s/coll-of ::question-set))

(s/def ::next-player ::player)

(s/def ::winner ::player)

(s/def ::loading? boolean?)

(s/def ::error-type keyword?)

(s/def ::error-message ::question/hiccup)

(s/def ::error
  (s/keys :req-un [::error-type]
          :opt-un [::error-message]))

(s/def ::route
  (partial instance? reitit/Match))

(s/def ::guess
  (s/or :string string?
        :number number?
        :items ::question/items))

(s/def ::answer-revealed? boolean?)

(s/def ::game-id
  (s/with-gen (s/and string?
                     (s/conformer seq)
                     (s/coll-of (s/and char? (partial re-matches #"^[A-Z]"))
                                :count game-id-length))
              game-id-gen))

(s/def ::game-url
  (s/and string? iri?))

(s/def ::db
  (s/keys :req-un [::game-id
                   ::game-url
                   ::player-1
                   ::player-2
                   ::question-sets]
          :opt-un [::answer-revealed?
                   ::az/board-state
                   ::error
                   ::guess
                   ::is-playing
                   ::loading?
                   ::next-player
                   ::question-set-id
                   ::question/question
                   ::question/questions
                   ::route
                   ::winner]))
