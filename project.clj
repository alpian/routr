(defproject routr "0.1.0"
  :description "routr"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [org.lesscss/lesscss "1.3.3"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [postgresql "9.1-901.jdbc4"]]
  :plugins [[lein-ring "0.8.3"]]
  :ring {:handler routr.web/app}
  :source-paths ["src/main/clojure"]
  :resource-paths ["resources"]
  :main "routr.web"
  :min-lein-version "2.1.1"
  :profiles {:dev {:jvm-opts ["-Droutr.environment=development"]}})
