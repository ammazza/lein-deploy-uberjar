(defproject org.ammazza/lein-deploy-uberjar "2.1.1-SNAPSHOT"
  :description "Deploy uberjars to Maven repositories, including private repositories hosted on S3."
  :url "https://github.com/ammazza/lein-deploy-uberjar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[org.sonatype.aether/aether-api "1.13.1"]]
  :repositories {"clojars" {:url "https://clojars.org/repo" :sign-releases false}}
  :eval-in-leiningen true)
