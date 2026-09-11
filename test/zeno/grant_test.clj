(ns zeno.grant-test
  (:require [clojure.test :refer [deftest is testing]]
            [zeno.grant :as grant]))

(defn- ctx []
  (grant/build {:vocab    {"add" (fn [a b] (+ a b))}
                :docs     {"add" "(add a b) - sum"}
                :ctx-info {:role "t"}}))

(deftest granted-vocab-is-callable
  (is (= 5 (grant/eval-str (ctx) "(add 2 3)")))
  (is (= 3 (grant/eval-str (ctx) "(+ 1 2)")) "pure core stays available"))

(deftest deny-by-default
  (testing "fs, shell, and Java interop are unreachable unless granted"
    (doseq [code ["(slurp \"/etc/hostname\")"
                  "(spit \"/tmp/zeno-x\" \"y\")"
                  "(System/getProperty \"user.home\")"
                  "(clojure.java.shell/sh \"id\")"]]
      (is (thrown? Exception (grant/eval-str (ctx) code))
          (str "should be denied: " code)))))

(deftest context-and-tools-exposed
  (is (= {:role "t"} (grant/eval-str (ctx) "(context)")))
  (is (some #{"(add a b) - sum"} (grant/eval-str (ctx) "(tools)"))
      "(tools) lists the granted verb's doc"))
