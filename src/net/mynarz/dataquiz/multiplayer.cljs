(ns net.mynarz.dataquiz.multiplayer
  (:require [net.mynarz.dataquiz.spec :as spec]))

(defn game-id
  []
  (spec/gen-one ::spec/game-id))
