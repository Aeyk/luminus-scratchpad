(defproject luminus-scratchpad "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[buddy/buddy-auth "2.2.0"]
                 [buddy/buddy-core "1.9.0"]
                 [buddy/buddy-hashers "1.7.0"]
                 [buddy/buddy-sign "3.3.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.10.0"]
                 [cljs-ajax "0.8.1"]
                 [clojure.java-time "0.3.2"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.google.javascript/closure-compiler-unshaded "v20200504" :scope "provided"]
                 [conman "0.9.1"]
                 [cprop "0.1.17"]
                 [day8.re-frame/http-fx "0.2.2"]
                 [expound "0.8.7"]
                 [funcool/struct "1.4.0"]
                 [luminus-http-kit "0.1.9"]
                 [luminus-migrations "0.7.1"]
                 [luminus-transit "0.1.2"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.5"]
                 [metosin/muuntaja "0.6.7"]
                 [metosin/reitit "0.5.10"]
                 [metosin/ring-http-response "0.9.1"]
                 [mount "0.1.16"]
                 [nrepl "0.8.3"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773" :scope "provided"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/google-closure-library "0.0-20191016-6ae1f72f" :scope "provided"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.postgresql/postgresql "42.2.18"]
                 [com.impossibl.pgjdbc-ng/pgjdbc-ng "0.8.6"]
                 [org.webjars.npm/bulma "0.9.1"]
                 [org.webjars.npm/material-icons "0.3.1"]
                 [org.webjars/webjars-locator "0.40"]
                 [re-frame "1.1.2"]
                 [reagent "1.0.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.8.2"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.31"]
                 [thheller/shadow-cljs "2.11.5" :scope "provided"]
                 [cljs-http "0.1.46"]
                 [com.draines/postal "2.0.4"]
                 [io.djy/mantra "0.6.1"]
                 [music-theory "0.3.1"]
                 [quil "3.1.0"]
                 [keybind "2.2.0"]
                 [thi.ng/geom "0.0.908"]
                 [com.taoensso/sente        "1.16.0"]
                 [com.taoensso/timbre       "4.10.0"]
                 [binaryage/dirac "1.7.2"]
                 [devcards "0.2.6"]
                 ]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot luminus-scratchpad.core

  :plugins [[lein-shadow "0.2.0"]
            [lein-kibit "0.1.2"]
            [lein-cljfmt "0.7.0"]
            [cider/cider-nrepl "0.25.5"]] 
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]
  :shadow-cljs
  {:nrepl
   {:port 7002
    :nrepl-middleware [cider.piggieback/wrap-cljs-repl
                       #_shadow.cljs.devtools.server.nrepl/middleware
                       dirac.nrepl/middleware]
    :init (do
            (require 'dirac.agent)
            (user/cljs-repl)
            (dirac.agent/boot!))}}
   :builds
   {:app
    {:target :browser
     :output-dir "target/cljsbuild/public/js"
     :asset-path "/js"
     :modules {:main {:init-fn luminus-scratchpad.core/start}
               :app {:entries [luminus-scratchpad.app]}}
     :compiler-options {:devcards :true}
     :devtools
     {:watch-dir "resources/public"
      :preloads [re-frisk.preload
                                               dirac.runtime.preload]}
     :dev
     {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}}
    :test
    {:target :node-test
     :output-to "target/test/test.js"
     :autorun true}}
  
  :npm-deps []
  :npm-dev-deps [[xmlhttprequest "1.8.0"]]

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["shadow" "release" "app"]]
             
             :aot :all
             :uberjar-name "luminus-scratchpad.jar"
             :source-paths ["env/prod/clj"  "env/prod/cljs" ]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn"
                             "-Xmx3G"]
                  :dependencies [[binaryage/devtools "1.0.2"]
                                 [cider/piggieback "0.5.2"]
                                 [pjstadig/humane-test-output "0.10.0"]
                                 [prone "2020-01-17"]
                                 [re-frisk "1.3.5"]
                                 [ring/ring-devel "1.8.2"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]] 
                  :source-paths ["env/dev/clj"  "env/dev/cljs" "test/cljs" ]
                  :resource-paths ["env/dev/resources"]
                  :repl-options
                  {:init-ns user
                   :repl-middleware
                   [shadow.cljs.devtools.server.nrepl/middleware]
                   :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"] 
                  
                  
                  }
   :profiles/dev {}
   :profiles/test {}})
