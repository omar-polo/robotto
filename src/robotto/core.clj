(ns robotto.core
  (:require [org.httpkit.client :as client]
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
  "Returns a channel that yields the response."
  ([ctx method-name] (make-request ctx method-name {}))
  ([ctx method-name data]
   ;; one slot is needed so we don't deadlock in the catch.
   (let [ch (chan 1)]
     (try
       (client/post (method-url ctx method-name)
                    {:headrs {"Content-Type" "application/json"
                              "Accept" "application/json"}
                     :body (to-json data)}
                    (fn [{:keys [body error]}]
                      (>!! ch {:data (-> body parse-json :data :result)
                               :error error})))
       (catch Throwable e
         (>!! ch {:data nil
                  :error e})))
     ch)))

(defn- run-stack
  "Given the data and a stack of interceptors, run it"
  [ctx stack data]
  ;; TODO: we're implicitly running the :pre task, :post and :error
  ;; would be nice too.
  ;; TODO: support async stuff
  (loop [ctx (assoc ctx
                    :data data
                    :next stack)]
    (when-let [[curr next] (:next ctx)]
      (recur (curr (assoc ctx :next next))))))

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

(defn- stack-for-update [{:keys [error command text msg]} {:keys [message], :as update}]
  (case (update-type update)
    ::message (if-let [cmd (is-command? message)]
                {:stack (cmd command)
                 :data  message}
                (or (first (for [k '(:text :new_chat_members)
                                 :when (k message)]
                             {:stack (k msg)
                              :data message}))
                    {:stack error
                     :data {:msg "unknown message type"
                            :type ::unknown-message
                            :data message}}))

    ::callback-query {:stack error
                      :data {:msg "callbacks queries aren't here yet"}}

    ::unknown {:stack error
               :data {:msg "unknown update type"
                      :type ::unknown-update
                      :data update}}))

(defn- notify
  "Notify an error by running the error stack."
  [{error :error, :as ctx} err]
  (run-stack ctx error err)
  ctx)

(defn- consume-updates
  "Runs the action for the given updates, then returns a new context."
  [{error :error, :as ctx} updates]
  (doseq [update updates]
    (let [[stack data] (stack-for-update ctx update)]
      (if (empty? stack)
        (run-stack ctx error {:msg "missing action for update"
                              :type ::missing-action
                              :data update})
        (run-stack ctx stack data))))

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
  (update-in ctx [:msg :new_chat_members] conj cb))

(defn on-command [ctx command cb]
  (update-in ctx [:command command] conj cb))

(defn on-text [ctx cb]
  (update-in ctx [:msg :text] conj cb))

(defn on-error [ctx err]
  (update ctx :error conj err))

(defn build-ctx
  "Builds the context."
  [{:keys [base-url token], :as ctx}]
  (-> ctx
      (assoc :req-url (str base-url token))
      (assoc :update-offset 0)
      (update-whole :me #(<!! (get-me %)))))
