(ns com.omarpolo.robotto.effect)

(defn queue-http-request
  "Queue the HTTP request `req` onto the `ctx`."
  [ctx req]
  (update ctx ::reqs conj req))

(defn send-message
  "Send a message."
  [ctx {:keys [chat-id text parse-mode opts inline-keyboard] :or {opts {}}}]
  (queue-http-request ctx {:name   'sendMessage
                           :params (merge opts
                                          (cond-> {:text text
                                                   :chat_id chat-id}
                                            parse-mode      (assoc :parse_mode parse-mode)
                                            inline-keyboard (assoc-in [:reply_markup :inline_keyboard] inline-keyboard)))}))

(defn reply-message
  "Send a message in the same chat as the given message.  If `:reply-to`
  is true, the message sent will be a reply."
  [ctx {{{cid :id} :chat, mid :message_id} :message, :keys [text reply parse-mode opts]}]
  (send-message ctx {:chat-id cid, :text text, :parse-mode parse-mode
                     :opts    (cond-> opts
                                reply (assoc :reply_to_message_id mid))}))

(defn delete-message
  "Delete the message with the given id in the given chat.`"
  [ctx {:keys [message-id chat-id]}]
  (queue-http-request ctx {:name   'deleteMessage
                           :params {:chat_id    chat-id
                                    :message_id message-id}}))

(defn get-chat
  "Get info about the chat given its id."
  [ctx chat-id]
  (queue-http-request ctx {:name   'getChat
                           :params {:chat_id chat-id}}))

(defn answer-callback-query
  "Reply to a callback query. Specify either the whole callback-query
  map with `:callback-query` or only the id with
  `:callback-query-id`."
  [ctx {{cid :id} :callback-query, id :callback-query-id, :keys [text, show-alert? url cache-time]}]
  (queue-http-request ctx {:name   'answerCallbackQuery
                           :params (cond-> {:callback_query_id (or cid id)}
                                     text        (assoc :text text)
                                     show-alert? (assoc :show_alert show-alert?)
                                     url         (assoc :url url)
                                     cache-time  (assoc :cache_time cache-time))}))

