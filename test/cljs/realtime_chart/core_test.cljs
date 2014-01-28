(ns realtime-chart.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is are testing]])
  (:require [cemerick.cljs.test :as t]
            [realtime-chart.core :refer [filter-old-data update-charts-data]]))

(deftest data-manipulation
  (testing "filter-old-data"
    (let [chart-data {(js/Date. 1390940275000) :a
                      (js/Date. 1390940277000) :b}]
      (is (= {(js/Date. 1390940277000) :b} (filter-old-data chart-data 1390940276000)))))

  (testing "update-charts-data"
    (let [source-id 0
          charts-data {:display 1000000
                       :charts [{:raw-data {(js/Date. 1390940275000) :a
                                            (js/Date. 1390940276000) :b
                                            (js/Date. 1390940277000) :c}}]}]
      (is (= {(js/Date. 1390940275000) :a-mod
              (js/Date. 1390940276000) :b
              (js/Date. 1390940277000) :c-mod}
             (->
               (update-charts-data charts-data source-id {(js/Date. 1390940275000) :a-mod
                                                          (js/Date. 1390940277000) :c-mod})
               :charts first :raw-data))))))

