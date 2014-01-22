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
realtime-chart.core.build_charts(options, data_sources);
```

## Development
```sh
lein cljsbuild auto # rebuild when source is changed
```

## TODO
* working prototype
* cljs tests
* make it into a library so other cljs can use it

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
