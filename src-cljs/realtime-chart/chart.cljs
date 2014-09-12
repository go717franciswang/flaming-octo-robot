(ns realtime-chart.chart
  (:require [domina :as dom]
            [domina.xpath :as xpath]
            [goog.net.XhrIo :as xhr])
  (:use [jayq.core :only [$ fade-out fade-in]]))

(defn add-rows [chart]
  (let [rows (:raw-data chart)
        columns (:columns chart)
        data (js/google.visualization.DataTable.)]
    (.addColumn data "datetime" "Time")
    (doseq [column columns]
      (.addColumn data "number" column))
    (doseq [[t row] (:raw-data chart)]
      (let [vs (map #(get row %) columns)
            data-row (conj vs t)]
        (.addRow data (clj->js data-row))))
    data))

(defn chart-options [default-options options]
  (let [options (into default-options options)]
    (clj->js options)))

(defn get-chart [selector]
  (js/google.visualization.LineChart. (.get ($ selector) 0)))

(.load js/google "visualization" "1" (clj->js {:packages ["corechart"]}))

(defn draw-chart [charts-data]
  (let [visible-chart-id (:visible charts-data)
        visible-chart (get-in charts-data [:charts visible-chart-id])
        chart (:chart charts-data)
        options (chart-options (:gchartOptions charts-data) 
                               (:gchartOptions visible-chart))
        data (add-rows visible-chart)]
    (.draw chart data options)))

(defn get-physical-chart [charts-data]
  ($ (:containerSelector charts-data)))

(defn add-keypress-listener-to-chart [key-id func charts-data]
  (.keydown ($ js/document)
             (fn [e]
               (when (= (.-which e) key-id)
                 (func)))))

(defn fade-out-chart [charts-data onfinish]
  (fade-out (get-physical-chart charts-data) 400 onfinish))

(defn fade-in-chart [charts-data onfinish]
  (fade-in (get-physical-chart charts-data) 1000 onfinish))
