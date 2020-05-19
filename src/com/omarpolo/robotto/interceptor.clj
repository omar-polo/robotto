(ns com.omarpolo.robotto.interceptor
  "An implementation of interceptors for robotto.

  Inspired by pedestal and loosely based on
  https://lispcast.com/a-model-of-interceptors"

  (:require [better-cond.core :as b]
            [manifold.deferred :as d]))

(defn normalize [i]
  (cond
    (nil? i) '()

    (= {} i) '()

    (seq? i) i

    (fn? i) (list {:enter i})

    (map? i) (list i)))

(defn chain
  ([] [])
  ([a] (normalize a))
  ([a b]
   (let [a (normalize a)
         b (normalize b)]
     (into a (reverse b))))
  ([a b & cs]
   (apply chain (chain a b) cs)))

;; TODO: rework so this run the next stack
;; TODO: use manifold to handle async stuff
(defn run-stackfn [keyfn]
  (fn [chain v]
    (b/cond
      (empty? chain)
      v

      :let [f (keyfn (first chain))]

      (nil? f)
      (recur (rest chain) v)

      :let [v' (try (f v)
                        (catch Throwable t
                          t))]

      (instance? Throwable v')
      v'

      (reduced? v')
      (unreduced v')

      :else
      (recur (rest chain) v'))))

(def run-enter (run-stackfn :enter))
(def run-leave (run-stackfn :leave))
(def run-error (run-stackfn :error))

(defn run-enter-then-leave [enter-chain leave-chain v]
  (let [v' (run-enter enter-chain v)]
    (if (instance? Throwable v')
      v'
      (run-leave leave-chain v'))))

(defn run [chain v]
  (let [rchain (reverse chain)
        v' (run-enter-then-leave rchain chain v)]
    (if (instance? Throwable v')
      (run-error rchain v')
      v')))
