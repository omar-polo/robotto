(ns com.omarpolo.robotto.inline-query-result)

(defn input-text-message-content
  [{:keys [text parse-mode disable-web-page-preview?]}]
  {:message_text             text
   :parse_mode               parse-mode
   :disable_web_page_preview disable-web-page-preview?})

(defn result-article [{:keys [id title input-message-content
                              reply-markup url hide-url? description
                              thumb-url thumb-width thumb-height]}]
  {:type                  "article"
   :id                    id
   :title                 title
   :input_message_content input-message-content
   :reply_markup          reply-markup
   :url                   url
   :hide_url              hide-url?
   :description           description
   :thumb_url             thumb-url
   :thumb_width           thumb-width
   :thumb_height          thumb-height})

(defn result-photo [{:keys [id photo-url thumb-url photo-width
                            photo-height title description caption
                            parse-mode reply-markup input-message-content]}]
  {:type                  "photo"
   :id                    id
   :photo_url             photo-url
   :thumb_url             thumb-url
   :photo_width           photo-width
   :photo_height          photo-height
   :title                 title
   :description           description
   :caption               caption
   :parse_mode            parse-mode
   :reply_markup          reply-markup
   :input_message_content input-message-content})
