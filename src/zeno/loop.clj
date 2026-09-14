(ns zeno.loop
  "Zeno core — the supervised step loop.

  A loop is an ordered seq of [label step-fn]; each step takes the context map and
  returns it. Steps are plain fns so they can be redefined live in the image; a
  crashing step is isolated (logged, skipped) and the context passes through, so
  one bad step never kills the loop. Domain-agnostic: the instance supplies the
  actual steps.")

(defn- supervise [label f ctx]
  (try
    (f ctx)
    (catch Throwable t
      (println (str "!! zeno step " label " error: " (.getMessage t)))
      ctx)))

(defn tick
  "Run one pass of `steps` (seq of [label fn]) over `ctx`, returning the new ctx."
  [steps ctx]
  (reduce (fn [c [label f]] (supervise (name label) f c)) ctx steps))

;; --- background scheduler: recurring processes run in both the interactive and
;;     the --daemon modes. The engine owns the loop + daemon; the instance only
;;     registers what to run (like init.el scheduling onto emacs' run-with-timer).

(defonce ^:private processes (atom {}))
(defonce ^:private scheduler (atom nil))

(defn every
  "Register (or replace) a recurring process `name`: run 0-arg `f` every
   `interval-ms`, supervised. Picked up live if the scheduler is already running."
  [name interval-ms f]
  (swap! processes assoc name {:interval-ms interval-ms :run f :next-at 0})
  name)

(defn cancel
  "Deregister a recurring process."
  [name]
  (swap! processes dissoc name))

(defn- run-due! []
  (let [now (System/currentTimeMillis)]
    (doseq [[nm {:keys [interval-ms run next-at]}] @processes]
      (when (and (ifn? run) (number? interval-ms) (number? next-at) (>= now next-at))
        (try (run)
             (catch Throwable t
               (println (str "!! zeno process " nm " error: " (.getMessage t)))))
        ;; re-schedule only if still registered, so a concurrent `cancel` can't be
        ;; resurrected as a partial (:run-less) zombie that later crashes the loop
        (swap! processes (fn [ps]
                           (if (contains? ps nm)
                             (update ps nm assoc :next-at (+ (System/currentTimeMillis) interval-ms))
                             ps)))))))

(defn start!
  "Start the background scheduler (a daemon thread) that runs the `every`
   processes on their intervals. Idempotent. zeno.main calls this so an instance's
   processes run in both interactive and --daemon modes."
  ([] (start! 1000))
  ([resolution-ms]
   (or @scheduler
       (let [t (doto (Thread.
                       (fn [] (loop [] (run-due!) (Thread/sleep (long resolution-ms)) (recur)))
                       "zeno-loop")
                 (.setDaemon true)
                 (.start))]
         (reset! scheduler t)
         t))))
