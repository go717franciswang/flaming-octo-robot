(ns realtime-chart.hello
  (:require [domina :as dom]
            [domina.xpath :as xpath]
            [goog.net.XhrIo :as xhr]))

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

; (js/alert "hello from clojurescript")

(my-get "test.json")
