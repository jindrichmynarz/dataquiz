(ns net.mynarz.dataquiz.interceptors
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as e]
            [re-frame.interceptor :refer [->interceptor assoc-effect get-effect get-coeffect]]
            [re-frame.core :refer [console]]))

; Copied from kee-frame (<https://github.com/ingesolvoll/kee-frame/blob/8907f615db446c20ed9054acd4b55995b8c6393f/src/kee_frame/spec.cljc#L41-L58>)

(defn default-log-spec-error
  [new-db spec event]
  (console :group "*** Spec error when updating DB, rolling back event " event " ***")
  (e/expound spec new-db)
  (console :groupEnd "*****************************"))

(defn rollback
  [context new-db db-spec]
  (default-log-spec-error new-db db-spec (get-coeffect context :event))
  (assoc-effect context :db (get-coeffect context :db)))

(defn spec-interceptor
  "Validate application database according to the `db-spec`."
  [db-spec]
  (->interceptor
   :id :spec
   :after (fn [context]
            (let [new-db (get-effect context :db)]
              (if (and new-db (not (s/valid? db-spec new-db)))
                (rollback context new-db db-spec)
                context)))))
