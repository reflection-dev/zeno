(ns zeno.loop-test
  (:require [clojure.test :refer [deftest is]]
            [zeno.loop :as zloop]))

(deftest folds-steps-in-order
  (is (= {:x [:a :b]}
         (zloop/tick [["a" (fn [c] (update c :x (fnil conj []) :a))]
                      ["b" (fn [c] (update c :x (fnil conj []) :b))]]
                     {}))))

(deftest crashing-step-is-isolated
  ;; a throwing step is logged and skipped; the context it was handed passes
  ;; through, and later steps still run — one bad step never kills the loop.
  (is (= {:x [:a :c]}
         (zloop/tick [["a"    (fn [c] (update c :x (fnil conj []) :a))]
                      ["boom" (fn [_] (throw (ex-info "boom" {})))]
                      ["c"    (fn [c] (update c :x (fnil conj []) :c))]]
                     {}))))
