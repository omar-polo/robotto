(ns com.omarpolo.robotto.keyboard)

(defn inline-url [text url]
  {:text text, :url url})

(defn inline-login [text login-url]
  {:text text, :login_url login-url})

(defn inline-callback [text data]
  {:text text, :callback_data data})
