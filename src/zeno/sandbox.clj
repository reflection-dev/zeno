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

(defn- ca-wrap
  "Prepend a CA-trust prelude to argv. msb network-binds secrets by terminating
   TLS with its own per-sandbox CA at /.msb/tls/ca.pem, which the image's public
   SSL_CERT_FILE/GIT_SSL_CAINFO bundle does not trust. Concatenate the two into
   one bundle and repoint the standard TLS env vars at it, so both network-bound
   (msb CA) and direct-egress (public roots) HTTPS verify. No-op when either the
   msb CA or the base bundle is absent."
  [argv]
  (into ["/bin/sh" "-c"
         (str "if [ -f /.msb/tls/ca.pem ] && [ -n \"$SSL_CERT_FILE\" ]; then "
              "cat \"$SSL_CERT_FILE\" /.msb/tls/ca.pem > /tmp/zeno-ca.crt 2>/dev/null && "
              "export SSL_CERT_FILE=/tmp/zeno-ca.crt GIT_SSL_CAINFO=/tmp/zeno-ca.crt "
              "REQUESTS_CA_BUNDLE=/tmp/zeno-ca.crt CURL_CA_BUNDLE=/tmp/zeno-ca.crt; fi; "
              "exec \"$@\"")
         "zeno-ca"]
        argv))

(defn run
  "Launch one ephemeral sandbox, run argv (optionally feeding :in on stdin),
   block until it exits (or --timeout), and return {:exit :out :err}.

     :image :volume :workdir :timeout :cpus :memory :name :argv
     :env        {K V}         plain, guest-visible env (-e K=V)
     :mounts     [{:src :dst :ro}]  extra host mounts (besides :volume)
     :net-bound  [{:env :host}] network-bound secret refs (--secret ENV@HOST);
                 the VALUE is read from THIS launcher's env at start, never
                 inlined into the guest/config, and released only toward :host
     :secret-env {ENV VAL}      secret VALUES exposed to msb via this launcher's
                 env so --secret can read them — NOT passed into the guest (-e)
     :egress     {:default \"deny\" :allow [host|\"public\" ...]}  egress allowlist
     :secret-scope  action on a secret sent to a disallowed host (default block)
     :in         string written to the msb command's stdin (then EOF)"
  [{:keys [image volume workdir env mounts net-bound secret-env egress
           secret-scope timeout cpus memory name argv in]
    :or   {workdir "/work"}}]
  (let [nm     (or name (str "agent-" (System/currentTimeMillis)))
        expand (fn [p] (str/replace (str p) #"^~" (System/getProperty "user.home")))
        args   (concat [msb "run" "--replace" "-q" "-n" nm]
                       (when volume ["-v" (str volume ":" workdir)])
                       (mapcat (fn [{:keys [src dst ro]}]
                                 ["-v" (str (expand src) ":" dst (when ro ":ro"))])
                               mounts)
                       ["-w" workdir]
                       (env-args env)
                       (mapcat (fn [{:keys [env host]}] ["--secret" (str env "@" host)])
                               net-bound)
                       (when egress
                         (if-let [prof (:net egress)]
                           ["--net" prof]
                           (concat ["--net-default-egress" (or (:default egress) "deny")]
                                   (mapcat (fn [h] ["--net-rule" (str "allow@" h)])
                                           (:allow egress)))))
                       (when secret-scope ["--secret-scope" secret-scope])
                       (when timeout ["--timeout" timeout])
                       (when cpus ["-c" (str cpus)])
                       (when memory ["-m" (str memory)])
                       [image "--"]
                       (ca-wrap argv))
        proc-env (when (seq secret-env)
                   (merge (into {} (System/getenv)) secret-env))]
    (apply sh/sh (concat args
                         (when proc-env [:env proc-env])
                         (when in [:in in])))))

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
     sandbox keys — :image :volume :name :timeout :env :mounts :net-bound
                    :secret-env :egress :secret-scope  (see `run`)
     :model  omp model — routed by whatever provider creds land in the guest env
             (default \"anthropic/claude-opus-4-8\")
     :task   the prompt string"
  [{:keys [model task env] :or {model "anthropic/claude-opus-4-8"} :as opts}]
  (let [r (run (merge {:timeout "5m"}
                      (select-keys opts [:image :volume :name :timeout :mounts
                                         :net-bound :secret-env :egress :secret-scope])
                      {:env  (assoc env "OMP_TASK" (str task) "OMP_MODEL" model)
                       :argv omp-argv}))]
    (assoc r :answer (omp-answer (:out r)))))
