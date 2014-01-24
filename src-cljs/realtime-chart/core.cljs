(ns realtime-chart.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]
            [jayq.core :as jq
             :refer [$ append ajax inner html $deferred when done resolve pipe on]]
            [goog.net.XhrIo :as xhr]
            [clojure.browser.repl :as repl]
            [realtime-chart.chart :as c])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

#_(.setOnLoadCallback js/google (fn [] (repl/connect "http://localhost:9000/repl")))
(.setOnLoadCallback js/google (fn [] (repl/connect "http://betalabs:9000/repl")))

(defn data-chan [source-id url interval]
  (let [rc (chan)
        receiver (fn [e]
                   (let [response (.-target e)
                         text (.getResponseText response)
                         data (map 
                                (fn [[t v]]
                                  [(js/Date. (js/parseInt t)) v])
                                (js->clj (JSON/parse text)))]
                     ;(.log js/console (str "sending data:" data))
                     (put! rc [:new-data source-id data])))
        query (fn q []
                ;(.log js/console (str "querying:" url))
                (xhr/send url receiver "GET")
                (js/setTimeout q interval))]
    (query)
    rc))

(defn get-oldest-timestamp [default-display display chart-data]
  (let [display (or display default-display)
        latest-time (-> chart-data last first)
        latest-timestamp (.getTime latest-time)
        oldest-timestamp (- latest-timestamp display)]
    oldest-timestamp))

(defn filter-old-data [chart-data oldest-timestamp]
  (let [outdated (seq (take-while #(< (.getTime %) oldest-timestamp) (keys chart-data)))]
    (apply dissoc chart-data outdated)))

(defn update-charts-data [charts-data source-id raw-data]
  (let [chart (get-in charts-data [:charts source-id])
        chart-data (get chart :raw-data (sorted-map))
        new-chart-data (into chart-data raw-data)
        oldest-timestamp (get-oldest-timestamp (:display charts-data) (:display chart) new-chart-data)
        latest-chart-data (filter-old-data new-chart-data oldest-timestamp)]
    (assoc-in charts-data [:charts source-id :raw-data] latest-chart-data)))

; charts-data is structured as follow
; {:visible 0                           ; id of the chart that should be visible
;  :container-selector #mydiv
;  :gchart-options {...}                ; default options for google chart
;  :display 10000                       ; default milli-seconds of chart data we should keep (rolling window size)
;  :charts [{:title                     ; chart title
;            :url                       ; GET request this url to get chart data
;            :gchart-options {...}      ; options for google chart
;            :display 20000             ; milli-seconds of chart data we should keep
;            :raw-data {t1: {:legend1 v1 :legend2 v2 ..}
;                       t2: {:legend1 v1 :legend2 v2 ..}
;                       ..}}
;           {..}..]}

(defn build-charts [options data-sources]
  (let [charts-data (conj options
                          [:visible 0]
                          [:charts data-sources])
        data-chans (for [source-id (range (count data-sources))
                         :let [data-source (get data-sources source-id)
                               url (:url data-source)]]
                     (data-chan source-id url (:query-interval options)))]
    (go
      (loop [charts-data charts-data]
        (let [[[msg-name source-id data] rc] (alts! data-chans)
              new-charts-data (update-charts-data charts-data source-id data)]
          ;(.log js/console (str "data: " new-charts-data))
          ;(.log js/console (str "received msg" [msg-name source-id data]))
          (when (not= charts-data new-charts-data)
            (c/draw-chart new-charts-data))
          (recur new-charts-data))))))

(defn run []
  (let [options {:query-interval 1000
                 :container-selector "#mydiv"
                 :gchart-options {:curveType "function"
                                  :width 800
                                  :height 300}
                 :display 60000}
        data-sources [{:gchart-options {:title "Memory Usage"}
                       :url "freemem.php"
                       :columns ["free mem"]}]]
    (build-charts options data-sources))
  (let [options {:query-interval 1000
                 :container-selector "#mydiv2"
                 :gchart-options {:curveType "function"
                                  :width 800
                                  :height 300}
                 :display 60000}
        data-sources [{:gchart-options {:title "Memory Usage"}
                       :url "freemem.php"
                       :columns ["free mem", "cached"]}]]
    (build-charts options data-sources)))

(.setOnLoadCallback js/google run)
