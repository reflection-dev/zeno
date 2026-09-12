(ns zeno.sandbox
  "Zeno core — launch an ephemeral agent body in a microsandbox (msb backend).

  One sandbox = one agent run. Durable state lives in a named, persistent volume
  mounted at :workdir (a host dir now; a PVC under k8s later), so the body is
  disposable but its session/repo/work survive across runs. The image is
  immutable infra; the volume is the mutable per-agent home.

  `run` is the generic primitive (any argv, optional stdin). `omp` wraps the omp
  agent in print mode: the task is piped into omp inside the guest (omp reads its
  prompt from stdin), with headless flags, and the assistant's answer is parsed
  out of omp's JSON stream.

  Secret VALUES are resolved by the instance and passed in :env — the launcher
  only forwards what it is given; egress/network-bound-secret policy is msb
  config layered on later."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(def ^:private msb
  (let [home (or (System/getenv "MSB_HOME")
                 (str (System/getProperty "user.home") "/.microsandbox"))]
    (str home "/bin/msb")))

(defn ensure-volume!
  "Create a named persistent volume if absent (idempotent). Returns the name."
  [name]
  (sh/sh msb "volume" "create" name)
  name)

(defn- env-args [env]
  (mapcat (fn [[k v]] ["-e" (str (if (keyword? k) (clojure.core/name k) k) "=" v)]) env))

(defn run
  "Launch one ephemeral sandbox, run argv (optionally feeding :in on stdin),
   block until it exits (or --timeout), and return {:exit :out :err}.

     :image :volume :workdir :env :timeout :cpus :memory :name :argv
     :in    string written to the msb command's stdin (then EOF)"
  [{:keys [image volume workdir env timeout cpus memory name argv in]
    :or   {workdir "/work"}}]
  (let [nm   (or name (str "agent-" (System/currentTimeMillis)))
        args (concat [msb "run" "--replace" "-q" "-n" nm]
                     (when volume ["-v" (str volume ":" workdir)])
                     ["-w" workdir]
                     (env-args env)
                     (when timeout ["--timeout" timeout])
                     (when cpus ["-c" (str cpus)])
                     (when memory ["-m" memory])
                     [image "--"]
                     argv)]
    (apply sh/sh (concat args (when in [:in in])))))

(defn- omp-answer
  "Extract the assistant's text from omp's --mode=json (NDJSON) output."
  [out]
  (->> (str/split-lines (or out ""))
       (keep #(try (json/read-str % :key-fn keyword) (catch Exception _ nil)))
       (filter #(and (= "message_end" (:type %))
                     (= "assistant" (get-in % [:message :role]))))
       (mapcat #(get-in % [:message :content]))
       (filter #(= "text" (:type %)))
       (map :text)
       (str/join "")
       (#(when (seq %) %))))

;; omp reads its prompt from stdin, not from argv; feeding it via the host
;; command's stdin does not reliably reach the guest under msb, so the task is
;; piped into omp INSIDE the guest. Task and model travel as env (no shell
;; escaping of caller data into the command line).
(def ^:private omp-argv
  ["/bin/sh" "-c"
   (str "printf '%s' \"$OMP_TASK\" | "
        "omp -p --no-pty --no-tools --no-session --no-title --mode=json "
        "--model \"$OMP_MODEL\"")])

(defn omp
  "Run omp headless in a sandbox and return the answer. Returns the `run` result
   plus :answer (the assistant text).
     :image :volume :name :timeout :env  — sandbox (see `run`)
     :model  omp model — MUST be an anthropic model to route via a Manifest
             ANTHROPIC_BASE_URL/ANTHROPIC_API_KEY in :env (default
             \"anthropic/claude-opus-4-8\"; `auto` picks openrouter and ignores it)
     :task   the prompt string"
  [{:keys [model task env] :or {model "anthropic/claude-opus-4-8"} :as opts}]
  (let [r (run (merge {:timeout "5m"}
                      (select-keys opts [:image :volume :name :timeout])
                      {:env  (assoc env "OMP_TASK" (str task) "OMP_MODEL" model)
                       :argv omp-argv}))]
    (assoc r :answer (omp-answer (:out r)))))
