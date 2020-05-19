(ns com.omarpolo.robotto.utils)

(defn private-message?
  "`true` if the given message belongs to a private chat."
  [{{t :type} :chat}]
  (= t "private"))

(defn group-message?
  "`true` if the given message belongs to a group chat."
  [{{t :type} :chat}]
  (= t "group"))

(defn supergroup-message?
  "`true` if the given message belongs to a super-group chat."
  [{{t :type} :chat}]
  (= t "supergroup"))

(defn channel-message?
  "`true` if the given message belongs to a channel."
  [{{t :type} :chat}]
  (= t "channel"))
