(ns zeno.oci
  "Zeno core — build an agent's immutable environment from a Clojure spec, in
  Clojure. No nix expressions, no dockerTools, no JSON->nix: nix is used only to
  realise package store paths and their closure; the OCI image (a docker-save
  archive) is assembled here and loaded into msb. Packages are either bare
  nixpkgs attr names (\"git\") or full flake installables (\"github:…#…omp\").

    {:name \"zeno-agent\" :tag \"base\" :system \"aarch64-linux\"
     :packages [\"github:numtide/llm-agents.nix#packages.aarch64-linux.omp\"
                \"git\" \"openssh\" \"coreutils\" \"bashInteractive\" \"cacert\"]
     :env {\"LANG\" \"C.UTF-8\"} :cmd [\"/bin/sh\"] :workdir \"/work\"}"
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import (java.security MessageDigest)))

(def ^:private msb
  (let [home (or (System/getenv "MSB_HOME")
                 (str (System/getProperty "user.home") "/.microsandbox"))]
    (str home "/bin/msb")))

(defn- sh! [& args]
  (let [{:keys [exit out err]} (apply sh/sh args)]
    (if (zero? exit)
      (str/trim out)
      (throw (ex-info (str "command failed: " (str/join " " (take 3 args)))
                      {:err err :exit exit})))))

(defn- oci-arch [system]
  (cond (str/starts-with? system "aarch64") "arm64"
        (str/starts-with? system "x86_64")  "amd64"
        :else "arm64"))

(defn- realise
  "Store path of a package's main output for `system`. A package is either a bare
   nixpkgs attr name (\"git\") or a full flake installable containing '#'
   (\"github:numtide/llm-agents.nix#packages.aarch64-linux.omp\")."
  [system pkg]
  (let [installable (if (str/includes? pkg "#")
                      pkg
                      (str "nixpkgs#legacyPackages." system "." pkg "^out"))]
    (last (str/split-lines
            (sh! "nix" "build" installable "--no-link" "--print-out-paths")))))

(defn- sha256-file [f]
  (let [md (MessageDigest/getInstance "SHA-256") buf (byte-array 65536)]
    (with-open [in (io/input-stream f)]
      (loop [] (let [n (.read in buf)] (when (pos? n) (.update md buf 0 n) (recur)))))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md)))))

(defn build!
  "Assemble a docker-save OCI archive from the spec; return the tar path."
  [{:keys [name tag system packages env cmd workdir]
    :or   {tag "base" system "aarch64-linux" workdir "/work" cmd ["/bin/sh"] env {}}}]
  (let [paths    (into {} (map (fn [p] [p (realise system p)])) packages)
        bashp    (or (get paths "bashInteractive") (realise system "bashInteractive"))
        cacertp  (get paths "cacert")
        clo      (str/split-lines (apply sh! "nix-store" "-qR" (vals paths)))
        w        (str (System/getProperty "java.io.tmpdir") "/zeno-img-" (System/currentTimeMillis))
        farm     (io/file w "farm")
        wd       (subs workdir 1)
        farm-dirs ["bin" "tmp" "etc" "root" wd]]
    (doseq [d farm-dirs] (.mkdirs (io/file farm d)))
    (sh! "ln" "-sf" (str bashp "/bin/bash") (str farm "/bin/sh"))
    (spit (io/file farm "etc/passwd") "root:x:0:0:root:/root:/bin/sh\n")
    (spit (io/file farm "etc/group")  "root:x:0:\n")
    (let [layer (str w "/layer.tar")]
      (apply sh! (concat ["tar" "-C" "/" "-cf" layer] (map #(subs % 1) clo)))
      (apply sh! (concat ["tar" "-C" (str farm) "-rf" layer] farm-dirs))
      (let [diff (sha256-file layer)
            ldir (io/file w diff)]
        (.mkdirs ldir)
        (.renameTo (io/file layer) (io/file ldir "layer.tar"))
        (spit (io/file ldir "VERSION") "1.0")
        (spit (io/file ldir "json") (json/write-str {:id diff}))
        (let [tls  (when cacertp
                     (let [crt (str cacertp "/etc/ssl/certs/ca-bundle.crt")]
                       [(str "SSL_CERT_FILE=" crt) (str "GIT_SSL_CAINFO=" crt)]))
              envs (concat [(str "PATH=" (str/join ":" (concat (map #(str % "/bin") (vals paths)) ["/bin"])))]
                           tls (map (fn [[k v]] (str k "=" v)) env))
              cfg-file (io/file w "config.json")]
          (spit cfg-file (json/write-str
                           {:architecture (oci-arch system) :os "linux"
                            :config {:Env (vec envs) :Cmd (vec cmd) :WorkingDir workdir}
                            :rootfs {:type "layers" :diff_ids [(str "sha256:" diff)]}}))
          (let [cfg (sha256-file cfg-file)]
            (.renameTo cfg-file (io/file w (str cfg ".json")))
            (spit (io/file w "manifest.json")
                  (json/write-str [{:Config (str cfg ".json")
                                    :RepoTags [(str name ":" tag)]
                                    :Layers [(str diff "/layer.tar")]}]))
            (let [img (str w "/image.tar")]
              (sh! "tar" "-C" w "-cf" img "manifest.json" (str cfg ".json") diff)
              img)))))))

(defn load!
  "Load a docker-save tar into msb under name:tag; return the image ref."
  [tar ref]
  (sh! msb "load" "-i" tar "-t" ref)
  ref)

(defn build-and-load!
  "Build the agent image from a Clojure spec and load it into msb; return the
   image ref (name:tag) ready for `zeno.sandbox/run`."
  [{:keys [name tag] :or {tag "base"} :as spec}]
  (load! (build! spec) (str name ":" tag)))
