(ns realtime-chart.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]
            [jayq.core :as jq
             :refer [$ append ajax inner html $deferred when done resolve pipe on]]
            [clojure.browser.repl :as repl]))

(.setOnLoadCallback js/google (fn [] (repl/connect "http://localhost:9000/repl")))

(defn click-chan [selector msg-name]
  (let [rc (chan)]
    (on ($ "body") :click selector {}
        (fn [e]
          (jq/prevent e)
          (put! rc [msg-name (data-from-event e)])))
    rc))

(defn app-loop [start-state]
  (let [new-todo-click (click-chan "#btn-new" :new-todo)
        cancel-new-form-click (click-chan "#btn-cancel" :cancel-new-form)]
    (go (loop [state start-state]
          (render-templates state)
          (<! new-todo-click)
