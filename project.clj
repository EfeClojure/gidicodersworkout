(defproject gidicodersworkout "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [selmer "0.9.1"]
                 [com.taoensso/timbre "4.1.1"]
                 [com.taoensso/tower "3.0.2"]
                 [markdown-clj "0.9.74"]
                 [environ "1.0.0"]
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [org.webjars/bootstrap "3.3.5"]
                 [org.webjars/jquery "2.1.4"]
                 [migratus "0.8.4"]
                 [conman "0.1.7"]
                 [org.immutant/web "2.1.0"]
                 
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [korma "0.4.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 
                 [clj-time "0.11.0"]
                 [clj-http "1.0.1"]
                 [digest "1.4.4"]
                 [org.clojure/data.json "0.2.6"]]

  :min-lein-version "2.0.0"
  :uberjar-name "gidicodersworkout.jar"
  :jvm-opts ["-server"]

  :main gidicodersworkout.core
  :migratus {:store :database}

  :plugins [[lein-environ "1.0.0"]
            [migratus-lein "0.1.7"]]
  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
             :aot :all}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev  {:dependencies [[ring/ring-mock "0.2.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.0"]
                                 [mvxcvi/puget "0.8.1"]]
                  
                  
                  :repl-options {:init-ns gidicodersworkout.core}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :port       3000
                        :nrepl-port 7000}}
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :profiles/dev {}
   :profiles/test {}})
