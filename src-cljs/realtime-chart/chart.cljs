(ns realtime-chart.chart
  (:require [domina :as dom]
            [domina.xpath :as xpath]
            [goog.net.XhrIo :as xhr])
  (:use [jayq.core :only [$]]))

(defn add-rows [chart]
  (let [rows (:raw-data chart)
        columns (:columns chart)
        data (js/google.visualization.DataTable.)]
    ;(.log js/console (str "data for chart" chart))
    (.addColumn data "datetime" "Time")
    (doseq [column columns]
      ;(.log js/console column)
      (.addColumn data "number" column))
    (doseq [[t row] (:raw-data chart)]
      ;(.log js/console (str "row " t row))
      (let [vs (map #(get row %) columns)
            data-row (conj vs t)]
        (.addRow data (clj->js data-row))))
    data))

(defn chart-options [default-options options]
  (let [options (into default-options options)]
    (clj->js options)))

(defn get-chart [selector]
  #_(.log js/console (str "container" selector))
  (js/google.visualization.LineChart. (.get ($ selector) 0)))

(.load js/google "visualization" "1" (clj->js {:packages ["corechart"]}))
#_(.setOnLoadCallback js/google #(my-get "test.json"))


(defn draw-chart [charts-data]
  (let [visible-chart-id (:visible charts-data)
        visible-chart (get-in charts-data [:charts visible-chart-id])
        chart (get-chart (:container-selector charts-data))
        options (chart-options (:gchart-options charts-data) 
                               (:gchart-options visible-chart))
        data (add-rows visible-chart)]
    (.draw chart data options)))

