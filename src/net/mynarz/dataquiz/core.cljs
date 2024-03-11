(ns net.mynarz.dataquiz.core
  (:require [kee-frame.core :as kf]
            [net.mynarz.az-kviz.logic :as az]
            [net.mynarz.dataquiz.controllers] ; Must load controllers
            [net.mynarz.dataquiz.events :as events]
            [net.mynarz.dataquiz.spec :as spec]
            [net.mynarz.dataquiz.subscriptions] ; Must load subscriptions, so that they are not elided by the compiler.
            [net.mynarz.dataquiz.views :as views]
            ["react" :as react]
            ;[reagent.core :as reagent]
            ;[reagent.dom.client :as rdc]
            [re-frame.core :as rf])
  (:import [goog History]
           [goog.history EventType]))

(defn dev-setup
  []
  (when goog.DEBUG
    (enable-console-print!) ;; so that println writes to `console.log`
    (println "dev mode")))

; Plain re-frame setup:
;
; (defonce root
;   ;; Init only on use, this ns is loaded for SSR build also
;   (delay (rdc/create-root (js/document.getElementById "app"))))

; (defn run
;   []
;   (dev-setup)
;   (when reagent/is-client
;     ;; Enable StrictMode to warn about e.g. findDOMNode
;     (rdc/render @root [:> react/StrictMode {} [views/ui]])))
;   (rf/dispatch [::events/load-questions])

(defn run
  []
  (dev-setup)
  (kf/start! {:routes         [["/" :enter]
                               ["/play" :play]
                               ["/verdict" :verdict]]
              :app-db-spec    ::spec/db
              :initial-db     {:loading? true
                               :player-1 "Hráč 1"
                               :player-2 "Hráč 2"}
              :root-component [views/ui]})
  (rf/dispatch [::events/load-questions]))

;; The `:dev/after-load` metadata causes this function to be called
;; after shadow-cljs hot-reloads code. We force a UI update by clearing
;; the Reframe subscription cache.
(defn ^:dev/after-load on-reload
  []
  (rf/clear-subscription-cache!)
  (run))

(defn ^:export main
  []
  (run))
