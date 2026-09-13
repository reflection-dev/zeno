(ns zeno.main
  "Entry point — load and run the config at $ZENO_HOME (default ~/.zeno), the way
   emacs loads ~/.emacs.d/init.el.

   The launcher composes the classpath (zeno core + the config project's :paths
   and machine :deps, as a local/root). This just evaluates init.clj and
   publishes the config dir as the `zeno.home` system property so init code can
   resolve its own files regardless of the working directory."
  (:require [clojure.java.io :as io]))

(defn config-home
  "The config directory: $ZENO_HOME, else ~/.zeno."
  []
  (or (not-empty (System/getenv "ZENO_HOME"))
      (str (System/getProperty "user.home") "/.zeno")))

(defn -main [& _]
  (let [home (config-home)
        init (io/file home "init.clj")]
    (System/setProperty "zeno.home" home)
    (when-not (.exists init)
      (binding [*out* *err*]
        (println (str "zeno: no config at " (.getPath init)
                      "\n  set ZENO_HOME or create " home "/init.clj (like ~/.emacs.d/init.el)")))
      (System/exit 1))
    (println "zeno: loading config from" home)
    (load-file (.getPath init))))
