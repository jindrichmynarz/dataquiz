(ns net.mynarz.dataquiz.i18n
  (:require [taoensso.tempura :as tempura]))

(def dictionary
  {:cs {:advanced-options {:board-size "Velikost hracího pole"
                           :label "Pokročilé nastavení"
                           :original-size "Originální"
                           :small-size "Malá"}
        :close "Zavřít"
        :credits {:and "a"
                  :questions-created-by "Otázky vytvořili"}
        :footer {:made-by "Vyrobil"
                 :source-code "Zdrojový kód"}
        :loading-questions "Načítám otázky..."
        :modals {:load-questions-error "Chyba při načítání otázek!"
                 :matching-player-names "Hráči musí mít odlišná jména!"
                 :no-more-questions-error "Otázky došly"
                 :parse-questions-error "Chybný formát otázek"}
        :play "Hrát"
        :play-again "Hrát znovu"
        :player-n "Hráč %1"
        :question {:open {:correct-answer "Správná odpověď je %1."}
                   :percent-range {:correct-answer "Správná odpověď je %1 %."}
                   :yesno {:yes "Ano"
                           :no "Ne"}}
        :question-box {:guess "Hádej"
                       :next-question "Další otázka"
                       :skip-question "Nevím, dál!"}
        :questions-picker {:forget-questions-played "Zapomenout již hrané otázky"
                           :load-questions "Nahrát otázky"
                           :make-your-own [:span "Vytvoř si " [:a {:href "https://github.com/jindrichmynarz/dataquiz?tab=readme-ov-file#questions"} "vlastní otázky"] "."]
                           :pick-questions "Vyber otázky"}
        :switch-lang "Přepni jazyk"
        :winner-heading "Vítězí %1!"}
   :en {:advanced-options {:board-size "Game board's size"
                           :label "Advanced options"
                           :original-size "Original"
                           :small-size "Small"}
        :close "Close"
        :credits {:and "and"
                  :questions-created-by "The questions were created by"}
        :footer {:made-by "Made by"
                 :source-code "Source code"}
        :loading-questions "Loading questions..."
        :modals {:load-questions-error "Error loading questions!"
                 :matching-player-names "The players must have different names!"
                 :no-more-questions-error "There are no more questions."
                 :parse-questions-error "Wrong format of the questions"}
        :play "Play"
        :play-again "Play again"
        :player-n "Player %1"
        :question {:open {:correct-answer "The correct answer is %1."}
                   :percent-range {:correct-answer "The correct answer is %1 %."}
                   :yesno {:yes "Yes"
                           :no "No"}}
        :question-box {:guess "Make a guess"
                       :next-question "Next question"
                       :skip-question "Skip question"}
        :questions-picker {:forget-questions-played "Forget the already played questions"
                           :load-questions "Load questions"
                           :make-your-own [:span "Make your " [:a {:href "https://github.com/jindrichmynarz/dataquiz?tab=readme-ov-file#questions"} "own questions"] "."]
                           :pick-questions "Pick questions"}
        :switch-lang "Switch the language"
        :winner-heading "%1 wins!"}})

(def tr
  (partial tempura/tr {:dict dictionary}))
