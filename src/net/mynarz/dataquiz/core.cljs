(ns net.mynarz.dataquiz.core
  (:require [day8.re-frame.http-fx]
            [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.interceptors :refer [spec-interceptor]]
            [net.mynarz.dataquiz.routes :refer [router]]
            [net.mynarz.dataquiz.spec :as spec]
            ; Must load subscriptions, so that they are not elided by the compiler.
            [net.mynarz.dataquiz.subscriptions]
            [net.mynarz.dataquiz.views :as views]
            ["react" :as react]
            [reagent.core :as reagent]
            [reagent.dom.client :as rdc]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]))

(defn dev-setup
  []
  (when goog.DEBUG
    (enable-console-print!) ; so that println writes to `console.log`
    (rf/reg-global-interceptor (spec-interceptor ::spec/db)) ; Validate the application database after every event.
    (println "dev mode")))

(defonce root
  ;; Init only on use, this ns is loaded for SSR build also
  (delay (rdc/create-root (js/document.getElementById "app"))))

(defn run
  []
  (dev-setup)
  (when reagent/is-client
    ;; Enable StrictMode to warn about e.g. findDOMNode
    (rdc/render @root [:> react/StrictMode {} [views/ui]])))

;; The `:dev/after-load` metadata causes this function to be called
;; after shadow-cljs hot-reloads code. We force a UI update by clearing
;; the Reframe subscription cache.
(defn ^:dev/after-load on-reload
  []
  (rf/clear-subscription-cache!)
  (run))

(defn ^:export main
  []
  (rfe/start! router
              (fn [new-route]
                (when new-route
                  (rf/dispatch [::events/change-route new-route])))
              {:use-fragment true})
  (rf/dispatch-sync [::events/initialize-db])
  (rf/dispatch [::events/download-questions])
  (run))
