(ns com.omarpolo.robotto.misc
  (:require [clojure.walk :as w]))

(defn deep-remove-nils [m]
  (letfn [(clean [[k v]]
            (when v
              [k v]))]
    (w/postwalk #(if (map? %)
                   (into {} (map clean %))
                   %)
                m)))

(comment
  (deep-remove-nils {:foo nil
                     :bar {:baz {:quux nil
                                 :c 'd}
                           :xxx 'xyz
                           :xyz nil}})
)
