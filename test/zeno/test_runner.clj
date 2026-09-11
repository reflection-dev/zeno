(ns zeno.test-runner
  "Deterministic, offline test suite entrypoint: `clojure -M:test`."
  (:require [clojure.test :as t]
            [zeno.grant-test]
            [zeno.loop-test]))

(defn -main [& _]
  (let [{:keys [fail error]} (t/run-tests 'zeno.grant-test 'zeno.loop-test)]
    (shutdown-agents)
    (System/exit (if (zero? (+ (or fail 0) (or error 0))) 0 1))))
