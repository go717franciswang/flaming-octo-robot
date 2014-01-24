(ns realtime-chart.chart
  (:require [domina :as dom]
            [domina.xpath :as xpath]
            [goog.net.XhrIo :as xhr])
  (:use [jayq.core :only [$]]))

(defn add-rows [rows]
  ;(.log js/console (str "data for chart" rows))
  (let [data (js/google.visualization.DataTable.)]
    (.addColumn data "datetime" "Time")
    (.addColumn data "number" "Free Memory")
    (.addRows data (clj->js (seq rows)))
    data))

(defn chart-options [title]
  (clj->js {:title title
            :curveType "function"
            :width 800 
            :height 300}))

(defn get-chart [selector]
  #_(.log js/console (str "container" selector))
  (js/google.visualization.LineChart. (.get ($ selector) 0)))

(.load js/google "visualization" "1" (clj->js {:packages ["corechart"]}))
#_(.setOnLoadCallback js/google #(my-get "test.json"))


(defn draw-chart [charts-data]
  (let [visible-chart-id (:visible charts-data)
        visible-chart (get-in charts-data [:charts visible-chart-id])
        chart (get-chart (:container-selector charts-data))
        options (chart-options (:title visible-chart))
        data (add-rows (get (:raw-data visible-chart) "free mem"))]
    (.draw chart data options)))

