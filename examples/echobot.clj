(ns echobot
  "This is a minimal example of a bot.  It includes commands, text
  handling and inline buttons in less than 50 LOC."
  (:require [com.omarpolo.robotto.core :as robotto]
            [com.omarpolo.robotto.effect :as fx]
            [com.omarpolo.robotto.keyboard :as key])
  (:gen-class))

(defn on-start [{{{id :id} :chat} :message, :as ctx}]
  (fx/send-message ctx {:chat-id id
                        :text    "Hello there.  This is echobot!"
                        
                        :inline-keyboard
                        [[(key/inline-callback "do not click!" "btn-id")]]}))

(defn echo [{msg :message, :as ctx}]
  (fx/reply-message ctx {:message msg
                         :text    (:text msg)
                         :reply?  true}))

(defn callback [{cb :callback-query, :as ctx}]
  (fx/answer-callback-query ctx {:callback-query cb
                                 :text "am I a joke to you?"
                                 :show-alert? true}))

(defn bot-main []
  (let [ctx (-> (robotto/new-ctx)
                (robotto/set-token (System/getenv "TELEGRAM_TOKEN"))
                (robotto/on-command :start on-start)
                (robotto/on-callback-query callback)
                (robotto/on-text echo)
                (robotto/build-ctx))]
    (loop [ctx ctx]
      (recur (robotto/get-updates ctx)))))

(defn- main [& _]
  (bot-main))

(comment
  (bot-main)
)
