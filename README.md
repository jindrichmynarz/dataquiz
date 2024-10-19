# dataquiz

Data-driven quizzes.

## Questions

Quiz questions are provided as data to dataquiz. They are written in the [EDN format](https://github.com/edn-format/edn). Each set of questions is a single [map](https://github.com/edn-format/edn?tab=readme-ov-file#maps) with the `:questions` key and optional metadata keys. The value of the `:questions` key is a [set](https://github.com/edn-format/edn?tab=readme-ov-file#sets) of questions, each of which is a map. Each question must specify its `:type` and provide the `:text` of the question. The text can be either a [string](https://github.com/edn-format/edn?tab=readme-ov-file#strings) or HTML written in the [Hiccup](https://github.com/weavejester/hiccup/blob/master/doc/syntax.md) syntax. This allows to include images, video, or sound in the questions. A question may provide the optional `:note` key with either a string or a Hiccup value. This is intended for additional notes or explanations that are shown after the question is answered. Each of the supported question types has additional required or optional keys described below.

The format of the questions is defined formally in [this specification](https://github.com/jindrichmynarz/dataquiz/blob/develop/src/net/mynarz/dataquiz/question_spec.cljs) using [clojure.spec](https://clojure.org/guides/spec).

Example questions about feminism (in Czech) are available [here](https://github.com/jindrichmynarz/femquiz/blob/gh-pages/femquiz.edn).

### Question types

dataquiz supports several types of questions specified by the `:type` key.

#### Multiple choice

The multiple choice question (`:type :multiple`) provides several choices for answers via the `:choices` key. The value of this key is a [vector](https://github.com/edn-format/edn?tab=readme-ov-file#vectors) of choices. At least two choices are required. Each choice is a map with the `:text` key giving a string value of the choice. At least one choice must have the `:correct?` key set to `true`.

Here is an example multiple choice question:

```clj
{
  :type :multiple
  :text [:<>
          [:audio {:autoplay "autoplay" :src "https://cdn.freesound.org/previews/155/155115_199526-lq.mp3"}]
          [:p "What is the name of the animal making this sound?"]]
  :choices [
    {:text "Ferret" :correct? true}
    {:text "Makak rhesus"}
    {:text "Chipmunk"}
    {:text "Your drunken uncle"}
  ]
  :note [:p [:a {:href "https://freesound.org/s/155115/"} "Ferret by J.Zazvurek"] "-- License: Attribution 4.0"]
}
```

#### Yes/no question

Yes/no question (`:type :yesno`) is answered with either "yes" or "no", one of which is correct. The correct answer is given by the `:correct?` key with the `true` value in case the answer is "yes", and the `false` value in case the answer is "no". The question's text can be phrased as a question or as a statement the veracity of which should be determined.

Here is an example yes/no question:

```clj
{
  :type :yesno
  :text "The Roman Catholic Church allows women to serve as priestesses."
  :correct? false
}
```

#### Open question

The answer to an open question (`:type :open`) is text given as value of `:answer`.

Here is an example open question:

```clj
{
  :type :open
  :text "How do you call a male-only discussion panel?"
  :answer "Manel"
}
```

When the question is shown, initial letters of each word of the answer are displayed as a hint. Character case of the answer does not matter and minor typos can be tolerated, depending on the length of the answer (computed by the [Jaro-Winkler distance](https://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance)).

#### Sorting question

Sorting question (`:type :sort`) requires to sort the given items in a specific order. The `:items` key must provide a [vector](https://github.com/edn-format/edn?tab=readme-ov-file#vectors) with as least two items. Each item is a [map](https://github.com/edn-format/edn?tab=readme-ov-file#maps) with a `:text` description and an optional `:sort-value` giving the number according to which the items are ordered (e.g., year). How are the items ordered is considered the correct order.

Here is an example sorting question:

```clj
{
  :type :sort
  :text "Sort the models of Barbie chronologically according to the years when Mattel started selling them."
  :items [
    {:text "Astronaut Barbie" :sort-value 1965}
    {:text "Busy Barbie" :sort-value 1972}
    {:text "CEO Barbie" :sort-value 1985}
    {:text "Curvy Barbie" :sort-value 2016}
  ]
}
```

Items of a sorting questions are shuffled randomly before they are shown. Answering a sorting question correctly requires to sort the items into their original order. A single transposition of items is tolerated. Sorting values are shown after the question is answered.

#### Percentage range

The percentage range question (`:type :percent-range`) asks to guess a percentage (i.e. a number between 0 and 100). The correct percentage is provided as a number via the `:percentage` key. An optional numeric `:threshold` can be provided. If no threshold is given, answers within 5 % from the correct answer are considered correct.

Here is an example percentage range question:

```clj
{
  :type :percent-range
  :text [:p "How many percent of Indian women are married (illegally) before their 18" [:sup "th"] "birthday?"]
  :percentage 23
}
```

### Metadata

Apart from questions, you can include metadata describing the questions. Here is the currently supported metadata:

| Key        | Description                                                                                 |
| ---------- | ------------------------------------------------------------------------------------------- |
| `:creator` | Zero or more creators represented in nested maps, each with one `:name` and optional `:url` |

## Development

This application is built by [Shadow CLJS](https://shadow-cljs.github.io/docs/UsersGuide.html). You can run the development build via `npm run watch`. Tests can be run via `npm run test`.

It is implemented in [ClojureScript](https://clojurescript.org) as a [re-frame](https://day8.github.io/re-frame/re-frame/) application.
