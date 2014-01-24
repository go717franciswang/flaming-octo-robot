(ns realtime-chart.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]
            [jayq.core :as jq
             :refer [$ append ajax inner html $deferred when done resolve pipe on]]
            [goog.net.XhrIo :as xhr]
            [clojure.browser.repl :as repl]
            [realtime-chart.chart :as c])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(.setOnLoadCallback js/google (fn [] (repl/connect "http://localhost:9000/repl")))

(defn data-chan [source-id url interval]
  (let [rc (chan)
        receiver (fn [e]
                   (let [response (.-target e)
                         text (.getResponseText response)
                         data (map 
                                (fn [[legend tv]]
                                  (let [norm-tv (map 
                                                  (fn [[t v]]
                                                    [(js/Date. (js/parseInt t)) v])
                                                  tv)]
                                    [legend norm-tv])) 
                                (js->clj (JSON/parse text)))]
                     #_(.log js/console (str "sending data:" data))
                     (put! rc [:new-data source-id data])))
        query (fn q []
                #_(.log js/console (str "querying:" url))
                (xhr/send url receiver "GET")
                (js/setTimeout q interval))]
    (query)
    rc))

(defn update-charts-data [charts-data source-id raw-data]
  (let [chart-data (get-in charts-data [:charts source-id :raw-data] {})
        new-chart-data (reduce
                         (fn [new-chart-data [legend data]]
                           (let [original-data (get new-chart-data legend)]
                             (assoc new-chart-data legend (into original-data data))))
                         chart-data
                         raw-data)]
    (assoc-in charts-data [:charts source-id :raw-data] new-chart-data)))

; charts-data is structured as follow
; {:visible 0
;  :container-selector #mydiv
;  :charts [{:title
;            :url
;            :raw-data {:legend1 {t1 v1 t2 v2 ...}
;                       :legend2 {t3 v3 t4 v4 ...}}}
;           ...]}

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
          #_(.log js/console (str "data: " new-charts-data))
          #_(.log js/console (str "received msg" [msg-name source-id data]))
          (when (not= charts-data new-charts-data)
            (c/draw-chart new-charts-data))
          (recur new-charts-data))))))

(defn run []
  (let [options {:query-interval 1000
                 :container-selector "#mydiv"}
        data-sources [{:title "Free mem"
                       :url "freemem.php"}]]
    (build-charts options data-sources)))

(.setOnLoadCallback js/google run)
