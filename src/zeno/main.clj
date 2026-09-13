(ns zeno.main
  "Entry point — start zeno and load the config at $ZENO_HOME (default ~/.zeno),
   the way emacs loads ~/.emacs.d/init.el.

   zeno ALWAYS starts, even if the config is missing or throws: the error is
   reported and you still land in a working REPL (like emacs dropping you into
   the editor with a *Warnings* buffer instead of refusing to boot).

   Modes:
     (default)   interactive REPL, config loaded (errors tolerated)
     --daemon    no interactive REPL; start an nREPL server and stay up so an
                 editor/client can connect later (like `emacs --daemon`)."
  (:require [clojure.java.io :as io]
            [clojure.main :as main]
            [zeno.loop :as zloop]))

(defn config-home
  "The config directory: $ZENO_HOME, else ~/.zeno."
  []
  (or (not-empty (System/getenv "ZENO_HOME"))
      (str (System/getProperty "user.home") "/.zeno")))

(defn- load-config!
  "Load <home>/init.clj, tolerating a missing or throwing config so zeno always
   comes up. Publishes the config dir as the `zeno.home` system property."
  [home]
  (System/setProperty "zeno.home" home)
  (let [init (io/file home "init.clj")]
    (if-not (.exists init)
      (binding [*out* *err*]
        (println "zeno: no config at" (.getPath init) "— starting empty"))
      (try
        (println "zeno: loading config from" home)
        (load-file (.getPath init))
        (catch Throwable t
          (binding [*out* *err*]
            (println "zeno: config error — starting anyway:")
            (println "  " (or (ex-message t) (str t)))
            (when-let [c (ex-cause t)]
              (println "   caused by:" (or (ex-message c) (str c))))))))))

(defn- start-nrepl!
  "Start an nREPL server (port from $ZENO_NREPL_PORT, else ephemeral), write
   <home>/.nrepl-port for editors, and return the server — or nil if nREPL isn't
   on the classpath."
  [home]
  (try
    (require 'nrepl.server)
    (let [start (resolve 'nrepl.server/start-server)
          port  (some-> (not-empty (System/getenv "ZENO_NREPL_PORT")) Integer/parseInt)
          srv   (start :bind "127.0.0.1" :port (or port 0))
          p     (:port srv)]
      (spit (io/file home ".nrepl-port") (str p))
      (println (str "zeno: nREPL on 127.0.0.1:" p " (wrote " home "/.nrepl-port)"))
      srv)
    (catch Throwable t
      (binding [*out* *err*]
        (println "zeno: could not start nREPL:" (or (ex-message t) (str t))))
      nil)))

(defn -main [& args]
  (let [daemon? (boolean (some #{"--daemon" "-d"} args))
        home    (config-home)]
    (load-config! home)
    (zloop/start!)                          ; run instance-registered processes (both modes)
    (if daemon?
      (do (start-nrepl! home)
          (println "zeno: daemon up — connect via nREPL; Ctrl-C to stop")
          @(promise))                       ; stay alive
      (do (println "zeno: REPL — config at" home "(sys prop zeno.home). ^D to exit.")
          (main/repl :init #(in-ns 'user))))))
