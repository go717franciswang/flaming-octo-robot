# realtime-chart

Cljs chart plugin that continuously query multiple sources, and display each source using a simple Google line chart

## Usage

```clj
(let [options {:container "mydiv"
               :width 600
               :height 200
               :transition-interval 20
               :query-interval 60}
      data-sources ["stockA.json"
                    "stockB.json"
                    "stockC.json"]]
  (realtime-chart.core/setup-charts options data-sources))
```

## TODO
* working prototype
* cljs tests


## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
