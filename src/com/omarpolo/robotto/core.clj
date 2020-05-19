(ns com.omarpolo.robotto.core
  (:require [com.omarpolo.robotto.interceptor :as interceptor]
            [com.omarpolo.robotto.effect :as effect]
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

(defn- chain-for-update [{{:keys [error command text msg callback-query]} ::chain}
                         {:keys [message callback_query], :as update}]
  (case (update-type update)
    ::message (if-let [cmd (is-command? message)]
                {:chain (cmd command)
                 :ctx   {:message message}}
                (or (first (for [k     '(:text :new_chat_members)
                                 :when (k message)]
                             {:chain (k msg)
                              :ctx   {:message message}}))
                    {:chain error
                     :ctx   {:error {:msg     "unknown message type"
                                     :type    ::unknown-message
                                     :message message}}}))

    ::callback-query {:chain callback-query
                      :ctx   {:callback-query callback_query}}

    ::unknown {:chain error
               :ctx   {:error {:msg  "unknown update type"
                               :type ::unknown-update
                               :data update}}}))

(defn- notify
  "Notify an error by running the error chain."
  [{{error :error} ::chain :as ctx} err]
  (interceptor/run error (merge ctx err)))

(defn- realize-requests
  "Run all the requests."
  [{reqs ::effect/reqs, :as ctx}]
  (doseq [req reqs]
    (let [{err :error} @(make-request ctx req)]
      (when err
        (println "during" req "got error" err)))))

(defn- consume-updates
  "Runs the action for the given updates, then returns a new context."
  [{error :error, :as ctx} updates]
  (doseq [update updates]
    (let [{chain :chain update-ctx :ctx} (chain-for-update ctx update)]
      (realize-requests
       (if (empty? chain)
         (interceptor/run error (merge {:error {:msg  "missing action for update"
                                                :type ::missing-action
                                                :data update}}
                                       ctx))
         (interceptor/run chain (merge update-ctx ctx))))))

  ;; return a new context with the :update-offset updated
  (let [{u :update-offset} ctx]
    (if (empty? updates)
      ctx
      (assoc ctx :update-offset
             (inc (reduce #(max %1 (:update_id %2))
                          u updates))))))

(defn get-updates
  "Retrieve updates from telegram and process them, yielding back a new contex."
  [{:keys [update-offset timeout], ch ::bus, :as ctx}]
  (let [{:keys [response error]} @(make-request ctx {:name   'getUpdates
                                                     :params {:offset          update-offset
                                                              :timeout         timeout
                                                              ;; :allowed_updates ["message" "callback_query"]
                                                              :allowed_updates []}})]
    (cond
      response (consume-updates ctx response)
      error    (notify ctx {:error {:msg  "error during update fetching"
                                    :type ::transport-error
                                    :data error}}))))



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

(defn on-new-chat-members [ctx i]
  (update-in ctx [::chain :msg :new_chat_members] interceptor/chain i))

(defn on-command [ctx command i]
  (update-in ctx [::chain :command command] interceptor/chain i))

(defn on-text [ctx i]
  (update-in ctx [::chain :msg :text] interceptor/chain i))

(defn on-error [ctx i]
  (update-in ctx [::chain :error] interceptor/chain i))

(defn on-callback-query [ctx i]
  (update-in ctx [::chain :callback-query] interceptor/chain i))

(defn build-ctx
  "Builds the context."
  [{:keys [base-url token], :as ctx}]
  (-> ctx
      (assoc :req-url (str base-url token))
      (assoc :update-offset 0)
      (update-whole :me #(<!! (get-me %)))))
