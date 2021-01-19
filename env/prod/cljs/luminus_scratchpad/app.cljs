(ns luminus-scratchpad.app
  (:require [luminus-scratchpad.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
