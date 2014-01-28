(defproject realtime-chart "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [domina "1.0.2"]
                 [jayq "2.5.0"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]
  :plugins [[lein-cljsbuild "1.0.1"]
            [com.cemerick/clojurescript.test "0.2.2"]]

  :aliases {"cleantest" ["do" "clean," "cljsbuild" "test"]}

  :cljsbuild { 
    :builds [{:source-paths ["src-cljs" "test/cljs"]
              :compiler {:output-to "resources/public/js/realtime-chart.js"
                         :optimizations :whitespace
                         ;:optimizations :advanced
                         :externs ["externs/jquery-1.9.js"
                                   "externs/google_visualization_api.js"
                                   "externs/google_loader_api.js"]
                         :pretty-print true}}]
    :test-commands {"unit-tests" ["phantomjs" :runner
                                  "window.literal_js_was_evaluated=true"
                                  "resources/public/js/jquery.min.js"
                                  "resources/public/js/jsapi"
                                  "resources/public/js/realtime-chart.js"
                                  ]}})
