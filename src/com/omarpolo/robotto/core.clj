(ns omarpolo.robotto.core
  (:require [com.omarpolo.robotto.interceptor :as interceptor]
            [org.httpkit.client :as client]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [go chan >! <! >!! <!! put! alts!!]]))

(defn- update-whole
  "Like update, but pass the whole map insteaf of only `(k m)` to the
  function."
  [m k func]
  (assoc m k (func m)))

(defn- parse-json [str]
  (try
    (if str
      (json/read-str str :key-fn keyword))
    (catch Throwable e
      (println "cannot parse" str "due to" e)
      nil)))

(defn- to-json [x]
  (json/write-str x))



(defn- method-url [{url :req-url} method-name]
  (str url "/" method-name))

(defn- make-request
  ([ctx req] (make-request ctx req identity))
  ([ctx {:keys [name params]} callback]
   (try
     (client/post (method-url ctx name)
                  {:headers {"Content-Type" "application/json"
                             "Accept" "application/json"}
                   :body (to-json params)}
                  (fn [{:keys [body error] :as resp}]
                    (let [body (parse-json body)]
                      (if (:ok body)
                        (callback {:response (:result body)})
                        (callback {:error {:body (or error body)
                                           ::http-response resp}})))))
     (catch Throwable e
       (callback {:error e})))))

(defn- update-type [update]
  (cond
    (:message update)
    ::message

    (:callback_query update)
    ::callback-query

    :else
    ::unknown))

(defn- extract-command
  "Extract the command name from the message."
  [{:keys [offset length]} {:keys [text]}]
  (subs text (inc offset) (+ offset length)))

(defn- is-command? [{entities :entities, :as message}]
  (loop [entities entities]
    (let [entity (first entities)
          t (:type entity)]
      (cond
        (empty? entities) nil
        (= "bot_command" t) (keyword (extract-command entity message))
        :else (recur (rest entities))))))

(defn- chain-for-update [{:keys [error command text msg]} {:keys [message], :as update}]
  (case (update-type update)
    ::message (if-let [cmd (is-command? message)]
                {:chain (cmd command)
                 :data  message}
                (or (first (for [k '(:text :new_chat_members)
                                 :when (k message)]
                             {:chain (k msg)
                              :data message}))
                    {:chain error
                     :data {:msg "unknown message type"
                            :type ::unknown-message
                            :data message}}))

    ::callback-query {:chain error
                      :data {:msg "callbacks queries aren't here yet"}}

    ::unknown {:chain error
               :data {:msg "unknown update type"
                      :type ::unknown-update
                      :data update}}))

(defn- notify
  "Notify an error by running the error chain."
  [{error :error, :as ctx} err]
  (interceptor/run error err)
  ctx)

(defn- consume-updates
  "Runs the action for the given updates, then returns a new context."
  [{error :error, :as ctx} updates]
  (doseq [update updates]
    (let [{:keys [chain data]} (chain-for-update ctx update)]
      (if (empty? chain)
        (interceptor/run error {:msg "missing action for update"
                                :type ::missing-action
                                :data update})
        (interceptor/run chain data))))

  ;; return a new context with the :update-offset updated
  (let [{u :update-offset} ctx]
    (if (empty? updates)
      ctx
      (assoc ctx :update-offset
             (inc (reduce #(max %1 (:update_id %2))
                          u updates))))))

(defn get-updates [{:keys [update-offset timeout], :as ctx}]
  (let [{:keys [data error]} (<!! (make-request ctx 'getUpdates
                                                {:offset update-offset
                                                 :timeout timeout
                                                 :allowed_updates ["message" "callback_query"]}))]
    (cond
      data  (consume-updates ctx (:result data))
      error (notify ctx {:msg "error during update fetching"
                         :type ::transport-error
                         :data error}))))



(defn get-me
  "Return a channel that yields the info about the bot."
  [ctx]
  (make-request ctx 'getMe))

(defn get-chat
  "Returns a channel that yields the chat info."
  [ctx id]
  (make-request ctx 'getChat {:chat_id id}))

(defn send-message
  "Send a `text` message to chat id `cid`."
  ([ctx cid text] (send-message ctx cid text {}))
  ([ctx cid text opts]
   (make-request ctx 'sendMessage (merge opts {:text text
                                               :chat_id cid
                                               :parse_mode "HTML"}))))

(defn reply-message
  "Utility function on top of send-message.  if `reply` is `true`, (by
  default is `false`) then the message will be a reply to the given
  message, otherwise it'll simply be sent to the same chat."
  ([ctx message text] (reply-message ctx message text false))
  ([ctx {{cid :id} :chat, mid :message_id} text reply]
   (send-message ctx cid text (if reply
                                {:reply_to_message_id mid}))))



(def base-config
  "The default configuration."
  {:token nil
   :base-url "https://api.telegram.org/bot"
   :timeout 5})

(defn new-ctx
  "Build a new context."
  []
  base-config)

(defn set-token [ctx token]
  (assoc ctx :token token))

(defn set-base-url [ctx url]
  (assoc ctx :base-url url))

(defn set-timeout [ctx timeout]
  (assoc ctx :timeout timeout))

(defn on-new-chat-members [ctx cb]
  (update-in ctx [:msg :new_chat_members] interceptor/chain cb))

(defn on-command [ctx command cb]
  (update-in ctx [:command command] interceptor/chain cb))

(defn on-text [ctx cb]
  (update-in ctx [:msg :text] interceptor/chain cb))

(defn on-error [ctx err]
  (update ctx :error interceptor/chain err))

(defn build-ctx
  "Builds the context."
  [{:keys [base-url token], :as ctx}]
  (-> ctx
      (assoc :req-url (str base-url token))
      (assoc :update-offset 0)
      (update-whole :me #(<!! (get-me %)))))
