(ns luminus-scratchpad.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[luminus-scratchpad started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[luminus-scratchpad has shut down successfully]=-"))
   :middleware identity})
