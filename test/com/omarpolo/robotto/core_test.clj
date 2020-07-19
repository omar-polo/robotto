(ns com.omarpolo.robotto.core-test
  (:require [clojure.test :refer :all]
            [com.omarpolo.robotto.core :refer :all]))

(deftest robotto-tests
  (testing "`process-updates` should return `ctx` on success"
    (let [ctx {:update-offset 0, :timeout 5}]
      (with-redefs [make-request (fn [& _] (atom {:response []}))]
        (is (= ctx (process-updates ctx))))))

  (testing "`process-updates` should return `ctx` on error"
    (let [ctx {:update-offset 0, :timeout 5}]
      (with-redefs [make-request (fn [& _] (atom {:error "something bad happened!"}))]
        (is (= ctx (process-updates ctx)))))))

(comment
  (run-all-tests)
  (run-tests)
)
