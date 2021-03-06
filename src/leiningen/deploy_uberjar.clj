(ns leiningen.deploy-uberjar
  "Build and deploy standalone jar to remote repository.
   This code blatantly ripped from lein's standard deploy task"
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [leiningen.core.user :as user]
            [clojure.java.io :as io]
            [leiningen.pom :as pom]
            [leiningen.jar :as jar]
            [leiningen.uberjar :as uberjar])
  (:import [org.sonatype.aether.deployment DeploymentException]))

(defn- abort-message [message]
  (cond (re-find #"Return code is 405" message)
        (str message "\n" "Ensure you are deploying over SSL.")
        (re-find #"Return code is 401" message)
        (str message "\n" "See `lein help deploy` for an explanation of how to"
             " specify credentials.")
        :else message))

(defn add-auth-interactively [[id settings]]
  (if (or (and (:username settings) (some settings [:password :passphrase
                                                    :private-key-file]))
          ;; No auth may be set  by wagon plugins (e.g. s3-wagon-private) that
          ;; require credentials of type not natively supported by Leiningen.
          (:no-auth settings)
          (.startsWith (:url settings) "file://"))
    [id settings]
    (do
      (println "No credentials found for" id)
      (println "See `lein help deploying` for how to configure credentials.")
      (print "Username: ") (flush)
      (let [username (read-line)
            password (.readPassword (System/console) "%s"
                                    (into-array ["Password: "]))]
        [id (assoc settings :username username :password password)]))))

(defn repo-for [project name]
  (let [[settings] (for [[id settings] (concat (:deploy-repositories project)
                                               (:repositories project)
                                               [[name {:url name}]])
                         :when (= id name)] settings)]
    (-> [name settings]
        (classpath/add-repo-auth)
        (add-auth-interactively))))

(defn sign [file]
  (let [exit (binding [*out* (java.io.StringWriter.)]
               (eval/sh (user/gpg-program) "--yes" "-ab" file))]
    (when-not (zero? exit)
      (main/abort "Could not sign" file))
    (str file ".asc")))

(defn signatures-for [jar-file pom-file coords]
  {(into coords [:extension "jar.asc"]) (sign jar-file)
   (into coords [:extension "pom.asc"]) (sign pom-file)})

(defn files-for [project repo]
  (let [coords [(symbol (:group project) (:name project)) (:version project)]
        jar-file (uberjar/uberjar project)
        pom-file (pom/pom project)]
    (merge {(into coords [:extension "jar"]) jar-file
            (into coords [:extension "pom"]) pom-file}
           (if (and (:sign-releases (second repo) true)
                    (not (.endsWith (:version project) "-SNAPSHOT")))
             (signatures-for jar-file pom-file coords)))))

(defn warn-missing-metadata [project]
  (doseq [key [:description :license :url]]
    (when (or (nil? (project key)) (re-find #"FIXME" (str (project key))))
      (main/info "WARNING: please set" key "in project.clj."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Given a single key  k from the aether/deploy artifacts map  and a string s,
;; append  s to  the original  version  string. The  key  is a  vector of  the
;; form:  [ART-GROUP/ART-ID  VERSION :extension  EXT].  The  suffix string  is
;; optional and defaults to "standalone".

(defn- add-version-suffix
  ;; The default suffix is 'standalone'.
  ([k] (add-version-suffix k "standalone"))
  ;; General version: specify a suffix s.
  ([k s]
   ;; If necessary prepend "-" to the suffix.
   (let [s (if (= (subs s 0 1) "-") s (str "-" s))]
   ;; Version is the 2nd element (index 1) in the key (vector).
     (assoc k 1 (str (nth k 1) s)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Given the aether/deploy artifacts map 'files' and a modifer function 'modf'
;; map the function on the keys and  return the artifacts map with the updated
;; keys.    Function  modf   is  optional;   its  default   behaviour  is   to
;; append "standalone"  to the versions; to  append a different string  to the
;; versions pass as modf:  #(add-version-suffix % "mystring").

(defn- update-artifact-keys
  ([files] (update-artifact-keys files add-version-suffix))
  ([files modf]
   (let [oldkeys (keys files)
         newkeys (into [] (map modf oldkeys))]
    (clojure.set/rename-keys files (zipmap oldkeys newkeys)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main entry point. This is the  function called when invoking from terminal:
;; 'lein deploy-uberjar'.

(defn deploy-uberjar
  "Build uberjar and deploy to remote repository.

  The target repository will be looked up in :repositories in project.clj:

  :repositories [[\"snapshots\" \"https://internal.repo/snapshots\"]
                 [\"releases\" \"https://internal.repo/releases\"]
                 [\"alternate\" \"https://other.server/repo\"]]

  If you don't provide a repository name to deploy to, either \"snapshots\" or
  \"releases\" will be used depending on your project's current version. See
  `lein help deploying` under \"Authentication\" for instructions on how to
  configure your credentials so you are not prompted on each deploy."
  ([project repository-name]
   (warn-missing-metadata project)
   (let [repo (repo-for project repository-name)
         files (update-artifact-keys (files-for project repo))]
     (try
       (main/debug "Deploying" files "to" repo)
       (aether/deploy-artifacts :artifacts (keys files)
                                :files files
                                :transfer-listener :stdout
                                :repository [repo])
       (catch DeploymentException e
         (when main/*debug* (.printStackTrace e))
         (main/abort (abort-message (.getMessage e)))))))
  ([project]
   (deploy-uberjar project (if (pom/snapshot? project)
                             "snapshots"
                             "releases"))))
