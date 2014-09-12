(ns realtime-chart.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]
            [jayq.core :as jq
             :refer [$ append ajax inner html $deferred when done resolve pipe on]]
            [goog.net.XhrIo :as xhr]
            [clojure.browser.repl :as repl]
            [realtime-chart.chart :as c]
            [clojure.walk :refer [keywordize-keys]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

;(.setOnLoadCallback js/google (fn [] (repl/connect "http://localhost:9000/repl")))
;(.setOnLoadCallback js/google (fn [] (repl/connect "http://betalabs:9000/repl")))

(defn data-chan [source-id url interval]
  (let [rc (chan)
        receiver (fn [e]
                   (let [response (.-target e)
                         text (.getResponseText response)
                         data (doall (map 
                                (fn [[t v]]
                                  [(js/Date. (js/parseInt t)) v])
                                (js->clj (JSON/parse text))))]
                     (put! rc [:new-data source-id data])))
        query (fn q []
                (xhr/send url receiver "GET")
                (js/setTimeout q interval))]
    (query)
    rc))

(defn transition-chan [interval start-source-id source-count charts-data]
  (let [rc (chan)
        active-source-id (atom start-source-id)
        transition-to (fn t [operation]
                        (let [next-source-id (mod (operation @active-source-id) source-count)]
                          (reset! active-source-id next-source-id)
                          (put! rc [:transition @active-source-id source-count])))
        transition-next #(transition-to inc)
        transition-prev #(transition-to dec)
        ]

    ; 37 left arrow, 39 right arrow
    (c/add-keypress-listener-to-chart 37 transition-prev charts-data)
    (c/add-keypress-listener-to-chart 39 transition-prev charts-data)

    (when (and interval (> source-count 1))
      (js/setInterval transition-next interval))
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
  (if (not (empty? raw-data))
    (let [chart (get-in charts-data [:charts source-id])
          chart-data (get chart :raw-data (sorted-map))
          new-chart-data (into chart-data raw-data)
          oldest-timestamp (get-oldest-timestamp (:display charts-data) (:display chart) new-chart-data)
          latest-chart-data (filter-old-data new-chart-data oldest-timestamp)]
      (assoc-in charts-data [:charts source-id :raw-data] latest-chart-data))
    charts-data))

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
;  :containerSelector #mydiv
;  :gchartOptions {...}                ; default options for google chart
;  :display 10000                       ; default milli-seconds of chart data we should keep (rolling window size)
;  :interval 5000                       : transition to next chart after this many milliseconds
;  :charts [{:title                     ; chart title
;            :url                       ; GET request this url to get chart data
;            :gchartOptions {...}      ; options for google chart
;            :display 20000             ; milli-seconds of chart data we should keep
;            :raw-data {t1: {:legend1 v1 :legend2 v2 ..}
;                       t2: {:legend1 v1 :legend2 v2 ..}
;                       ..}}
;           {..}..]}

(defn build-charts [options data-sources]
  (let [charts-data (conj options
                          [:visible 0]
                          [:charts data-sources]
                          [:chart (c/get-chart (:containerSelector options))])
        data-chans (doall (for [source-id (range (count data-sources))
                         :let [data-source (get data-sources source-id)
                               url (:url data-source)]]
                     (data-chan source-id url (:queryInterval options))))
        transition-chan (transition-chan 
                          (:interval charts-data) 0 (count data-sources) charts-data)
        all-chans (conj data-chans transition-chan)
        fading? (atom false)]
    (go
      (loop [charts-data charts-data]
        (let [[[msg-name source-id data] rc] (alts! all-chans)
              new-charts-data (if (= msg-name :new-data)
                                (update-charts-data charts-data source-id data)
                                (assoc charts-data :visible source-id))]
          (transition-charts charts-data new-charts-data fading?)
          (recur (doall new-charts-data)))))))

(defn ^:export build-charts-from-js [options data-sources]
  (let [options (keywordize-keys (js->clj options))
        data-sources (keywordize-keys (js->clj data-sources))]
  (build-charts options data-sources)))

