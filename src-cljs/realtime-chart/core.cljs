(ns realtime-chart.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]
            [jayq.core :as jq
             :refer [$ append ajax inner html $deferred when done resolve pipe on]]
            [goog.net.XhrIo :as xhr]
            [clojure.browser.repl :as repl]
            [realtime-chart.chart :as c])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

;(.setOnLoadCallback js/google (fn [] (repl/connect "http://localhost:9000/repl")))
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
                     (put! rc [:new-data source-id data])))
        query (fn q []
                (xhr/send url receiver "GET")
                (js/setTimeout q interval))]
    (query)
    rc))

(defn transition-chan [interval start-source-id source-count]
  (let [rc (chan)
        transition (fn t [source-id]
                     (put! rc [:transition source-id source-count])
                     (let [next-source-id (mod (inc source-id) source-count)]
                       (js/setTimeout #(t next-source-id)  interval)))]
  (when (and interval (> source-count 1))
      (transition start-source-id))
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

(defn wait-for [target input-chan]
  (loop []
    (go (let [msg (<! input-chan)]
          (if (= msg target)
            msg
            (recur))))))

(defn transition-charts [old-charts-data new-charts-data fading?]
  (when (not @fading?)
    (if (not= (:visible old-charts-data) (:visible new-charts-data))
      (do
        (reset! fading? true)
        (c/fade-out-chart old-charts-data
                          (fn []
                            (c/draw-chart new-charts-data)
                            (c/fade-in-chart new-charts-data #(reset! fading? false)))))
      (when (not= old-charts-data new-charts-data)
        (c/draw-chart new-charts-data)))))

; charts-data is structured as follow
; {:visible 0                           ; id of the chart that should be visible
;  :container-selector #mydiv
;  :gchart-options {...}                ; default options for google chart
;  :display 10000                       ; default milli-seconds of chart data we should keep (rolling window size)
;  :interval 5000                       : transition to next chart after this many milliseconds
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
                     (data-chan source-id url (:query-interval options)))
        transition-chan (transition-chan (:interval charts-data) 0 (count data-sources))
        all-chans (conj data-chans transition-chan)
        fading? (atom false)]
    (go
      (loop [charts-data charts-data]
        (let [[[msg-name source-id data] rc] (alts! all-chans)
              new-charts-data (if (= msg-name :new-data)
                                (update-charts-data charts-data source-id data)
                                (assoc charts-data :visible source-id))]
          (transition-charts charts-data new-charts-data fading?)
          (recur new-charts-data))))))

(defn ^:export build-charts-from-js [options data-sources]
  (build-charts (js->clj options) (js->clj data-sources)))

(defn run []
  (let [options {:query-interval 1000
                 :container-selector "#mydiv"
                 :gchart-options {:curveType "function"
                                  :width 800
                                  :height 300}
                 :display 60000}
        data-sources [{:gchart-options {:title "Memory Usage (60 sec window)"}
                       :url "freemem.php"
                       :columns ["free mem"]}]]
    (build-charts options data-sources))
  (let [options {:query-interval 1000
                 :container-selector "#mydiv2"
                 :gchart-options {:curveType "function"
                                  :width 800
                                  :height 300}
                 :display 30000}
        data-sources [{:gchart-options {:title "Memory Usage (30 sec window)"}
                       :url "freemem.php"
                       :columns ["free mem", "cached"]}]]
    (build-charts options data-sources))
  (let [options {:query-interval 1000
                 :container-selector "#mydiv3"
                 :gchart-options {:curveType "function"
                                  :width 800
                                  :height 300}
                 :interval 5000
                 :display 30000}
        data-sources [{:gchart-options {:title "Memory Usage (60 sec window)"}
                       :url "freemem.php"
                       :columns ["free mem"]
                       :display 60000}
                      {:gchart-options {:title "Memory Usage (30 sec window)"}
                       :url "freemem.php"
                       :columns ["free mem", "cached"]}]]
    (build-charts options data-sources)))

(.setOnLoadCallback js/google run)
