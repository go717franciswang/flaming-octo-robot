(ns realtime-chart.hello
  (:require [domina :as dom]
            [domina.xpath :as xpath]
            [goog.net.XhrIo :as xhr])
  (:use [jayq.core :only [$]]))

(defn append-to-body [s]
  (dom/append! (xpath/xpath "//body") (str "<div>" s "</div>")))

(defn receiver [event]
  (let [response (.-target event)
        text (.getResponseText response)]
    (loop [i 0]
      (when (< i 100)
        (append-to-body (str i text))
        (recur (inc i))))))

(defn my-get [url]
  (xhr/send url receiver "GET"))

(defn add-rows []
  (let [data (js/google.visualization.DataTable.)]
    (.addColumn data "string" "Topping")
    (.addColumn data "number" "slices")
    (.addRows data (clj->js [["Mushrooms" 3] ["Onions" 1] ["Olives" 1]]))
    data))

(defn chart-options []
  (clj->js {:title "How much Pizza I ate last night" 
            :width 400 
            :height 300}))

(defn get-chart []
  (js/google.visualization.PieChart. (.getElementById js/document "mydiv")))

(defn draw-chart []
  (let [data (add-rows)
        options (chart-options)
        chart (get-chart)]
    (.draw chart data options)))

(.load js/google "visualization" "1" (clj->js {:packages ["corechart"]}))
(.setOnLoadCallback js/google draw-chart)
(.setOnLoadCallback js/google #(my-get "test.json"))



