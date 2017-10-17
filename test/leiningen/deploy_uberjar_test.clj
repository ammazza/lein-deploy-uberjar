(ns leiningen.deploy-uberjar-test
  "Test the  utility functions  (not the deployment  process) included  in the
  lein-deploy-uberjar plugin."
  (:require [clojure.test :refer :all]
            [leiningen.deploy-uberjar :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Use the reader macro to import private functions that need to be tested, by
;; binding them to local names. Makes such references private, too.

;; (def ^{:private true} XXX
;;   #'leiningen.deploy-uberjar/XXX)

(def ^{:private true} add-version-suffix
  #'leiningen.deploy-uberjar/add-version-suffix)

(def ^{:private true} update-artifact-keys
  #'leiningen.deploy-uberjar/update-artifact-keys)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test function add-version-suffix. Artifact keys are vectors, containing the
;; version string as second element.

;; TODO: implement the following checks (and the relative tests) in add-version-suffix:
;; - If suffix is empty string, use default.
;; - If key not vector or vector too short, report error.

(deftest add-version-suffix-test

  (testing "Add default suffix to version string"
    (let [input  ["dummy/dummy-lib" "0.0.1-SNAPSHOT" :extension "jar"]
          output (add-version-suffix input)
          check  ["dummy/dummy-lib" "0.0.1-SNAPSHOT-standalone" :extension "jar"]]
      (is (= output check))))

  (testing "Add user-defined suffix with leading '-' to version string"
    (let [input  ["dummy/dummy-lib" "0.0.1-SNAPSHOT" :extension "jar"]
          output (add-version-suffix input "-mysuff")
          check  ["dummy/dummy-lib" "0.0.1-SNAPSHOT-mysuff" :extension "jar"]]
      (is (= output check))))

  (testing "Add user-defined suffix without leading '-' to version string"
    (let [input  ["dummy/dummy-lib" "0.0.1-SNAPSHOT" :extension "jar"]
          output (add-version-suffix input "mysuff")
          check  ["dummy/dummy-lib" "0.0.1-SNAPSHOT-mysuff" :extension "jar"]]
      (is (= output check)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A  map  describing  artifacts  as  the one  used  by  aether/deploy.   This
;; information  is manipulated  by the  plugin  to ensure  snapshot files  are
;; published with the '-standalone' qualifier and without timestamping.
;;
;; The relevant  functions work on  the :files field. Field  :artifacts (which
;; just replicates the keys in :files) is then derived from :files.

(def ^{:private true} files
  {:artifacts
   '(["dummy/dummy-lib" "0.0.1-SNAPSHOT" :extension "jar"]
     ["dummy/dummy-lib" "0.0.1-SNAPSHOT" :extension "pom"]),
   :files {["dummy/dummy-lib" "0.0.1-SNAPSHOT" :extension "jar"]
           "/home/user/dummy/target/uberjar/dummy-lib-0.0.1-SNAPSHOT-standalone.jar",
           ["dummy/dummy-lib" "0.0.1-SNAPSHOT" :extension "pom"]
           "/home/user/dummy/pom.xml"},
   :transfer-listener :stdout,
   :repository [["snapshots" {:url "s3p://my-s3-bucket/snapshots", :no-auth true}]]})

;; Input ('original') and output ('modified') test maps. Output uses default suffix.

(def ^{:private true} original
  (:files files))

(def ^{:private true} modified
  {["dummy/dummy-lib" "0.0.1-SNAPSHOT-standalone" :extension "jar"]
   "/home/user/dummy/target/uberjar/dummy-lib-0.0.1-SNAPSHOT-standalone.jar",
   ["dummy/dummy-lib" "0.0.1-SNAPSHOT-standalone" :extension "pom"]
   "/home/user/dummy/pom.xml"})

;; Output map  ('modified2'), using a  user-defined suffix. The fat  jar name,
;; clearly, still contains  the usual 'standalone', this  process only affacts
;; the artifact identifiers (keys).

(def ^{:private true} modified2
  {["dummy/dummy-lib" "0.0.1-SNAPSHOT-mysuf" :extension "jar"]
   "/home/user/dummy/target/uberjar/dummy-lib-0.0.1-SNAPSHOT-standalone.jar",
   ["dummy/dummy-lib" "0.0.1-SNAPSHOT-mysuf" :extension "pom"]
   "/home/user/dummy/pom.xml"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test update-artifact-keys.

(deftest update-artifact-keys-test

  (testing "Update keys from artifact map, using the default modifier."
    (is (= (update-artifact-keys original) modified)))

  (testing "Update keys from artifact map, using a user-defined modifier."
    ;; Modifier:  function  taking a  single  key  (vector) and  updating  the
    ;; version string (2nd element).
    (let [mod #(assoc % 1 (str (nth % 1) "-mysuff"))]
      (is (= (update-artifact-keys original) modified)))))
