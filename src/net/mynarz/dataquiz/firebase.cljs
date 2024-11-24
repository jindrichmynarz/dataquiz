(ns net.mynarz.dataquiz.firebase
  (:require [clojure.string :as string]
            ["firebase/app" :as app]
            ["firebase/database" :as database]))

(defn ->path
  [path]
  (string/join "/" path))

; (defn database-ref
;   [path]
;   (.ref firebase/firestore (->path path)))

(def config
  #js {:apiKey "AIzaSyC6QaWZPJaO15xztlOCspjUo4DaBB6zzd8"
       :authDomain "dataquiz-4184e.firebaseapp.com"
       :projectId "dataquiz-4184e"
       :storageBucket "dataquiz-4184e.appspot.com"
       :messagingSenderId "521770991018"
       :appId "1:521770991018:web:8aa76cfa4fa04fa9857fd3"})

(defonce db
  (cond-> (-> config app/initializeApp database/getDatabase)
    (= js/window.location.hostname "localhost") (database/connectDatabaseEmulator "127.0.0.1" 9000)))
