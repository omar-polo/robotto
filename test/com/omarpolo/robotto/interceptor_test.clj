(ns com.omarpolo.robotto.interceptor-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [com.omarpolo.robotto.interceptor :refer :all]))

(def gen-integer-fn
  (gen/elements
   [identity
    (constantly 0)
    (constantly 1)
    inc
    dec]))

(def gen-interceptor
  (gen/let [enter gen-integer-fn
            leave gen-integer-fn]
    {:enter enter
     :leave leave}))

(defspec chain-associative
  1000
  (prop/for-all [a gen-interceptor
                 b gen-interceptor
                 c gen-interceptor
                 x gen/small-integer]
      (let [x (mod x 100)
            i1 (chain a (chain b c))
            i2 (chain (chain a b) c)]
        (= (run i1 x) (run i2 x)))))

(defn constantly-error [_]
  (throw (ex-info "error!" {})))

(defspec chain-errors
  1000
  (prop/for-all [i (gen/list gen-interceptor)
                 x gen/small-integer]
      (let [t (ex-info "another error" {})
            i (chain {:error (constantly t)}
                     i
                     {:enter constantly-error})
            x (mod x 100)]
        (identical? t (run i x)))))

(defspec chain-error-not-caught
  1000
  (prop/for-all [i (gen/list gen-interceptor)
                 x gen/small-integer]
      (let [i (chain {:enter constantly-error} i)
            x (mod x 100)]
        (instance? Throwable (run i x)))))

(defn make-reduced [i]
  (update i :enter #(comp reduced %)))

;; This fails because the implementation of the early exit isn't
;; correct.  We need some rearrangment in the implementation (adding
;; also async in the meantime) to reach this.
(defspec chain-early-exit
  1000
  (prop/for-all [a gen-interceptor
                 b gen-interceptor
                 x gen/small-integer]
      (let [x (mod x 100)
            i1 (chain a)
            i2 (chain (make-reduced a) b)]
        (= (run i1 x) (run i2 x)))))

(comment
  (run-tests)
)
