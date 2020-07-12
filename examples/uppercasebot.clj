(ns uppercasebot
  "This is an example of a bot that provides answers to inline queries."
  (:require [com.omarpolo.robotto.core :as robotto]
            [com.omarpolo.robotto.effect :as fx]
            [com.omarpolo.robotto.inline-query-result :as iq])
  (:gen-class))

(defn on-start [{{{id :id} :chat} :message :as ctx}]
  (fx/send-message ctx {:chat-id id
                        :text "Hello there.  This is uppercasebot.  You can call me inline and I will send what you typed in uppercase.  Cool, hu?"}))

(defn handle-inline-query [{{text :query :as iq} :inline-query :as ctx}]
  (let [TEXT    (.toUpperCase text)
        results (if (= text "")
                  []
                  [(iq/result-article
                    {:id                    (.hashCode text)
                     :title                 TEXT
                     :input-message-content (iq/input-text-message-content {:text TEXT})})])]
    (fx/answer-inline-query ctx {:inline-query iq
                                 :results      results})))

(defn -main [& _]
  (let [ctx (-> (robotto/new-ctx)
                (robotto/set-token (System/getenv "TELEGRAM_TOKEN"))
                (robotto/on-command :start on-start)
                (robotto/on-inline-query handle-inline-query)
                (robotto/build-ctx))]
    (loop [ctx ctx]
      (recur (robotto/get-updates ctx)))))

(comment
  (-main)
)
