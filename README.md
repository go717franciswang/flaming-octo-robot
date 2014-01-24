# realtime-chart

Cljs chart plugin that continuously query multiple sources, and display each source using a simple Google line chart

## Usage
#### include relevant libs
```html
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script src="https://www.google.com/jsapi"></script>
<script src="realtime-chart.js"></script>
```

#### set up server data sources


#### call in cljs
```clj
(let [options {:container "mydiv"
               :width 600
               :height 200
               :transition-interval 20
               :query-interval 60
               :default-display {:range 30 
                                 :unit :minutes}}
      data-sources [{:title "Stock A last 30 minutes"
                     :url "get_price.json?stock=A"}
                    {:title "Stock B last 30 minutes"
                     :url "get_price.json?stock=B"}
                    {:title "Stock C last 30 minutes"
                     :url "get_price.json?stock=C"}
                    {:title "Unrealized profit last 30 days"
                     :url "get_profit.json"
                     :display {:range 30
                               :unit :days}}]]
  (realtime-chart.core/build-charts (clj->js options) 
                                    (clj->js data-sources)))
```
#### or call in js
```js
realtime_chart.core.build_charts(options, data_sources);
```

## Development
```sh
lein cljsbuild auto # rebuild when source is changed

# brepl will listen for connection on port 9000
rlwrap -r -m -q '\\"' -b "(){}[],^%3@\\\";:'" lein trampoline cljsbuild repl-listen
```
connect to brepl with the following code
```clj
(.setOnLoadCallback js/google (fn [] (repl/connect "http://localhost:9000/repl")))
```

## TODO
* if we were to represent the state of chart data as state of modal in http://rigsomelight.com/2013/07/18/clojurescript-core-async-todos.html, the state can contain data from all sources, and only one source will be visible at a given moment. furthermore, one async channel can trigger the moving visibility from one source to another, and another channel that requests data from server
* working prototype
* cljs tests
* make it into a library so other cljs can use it

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
