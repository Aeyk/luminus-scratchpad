(ns luminus-scratchpad.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [luminus-scratchpad.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[luminus-scratchpad started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[luminus-scratchpad has shut down successfully]=-"))
   :middleware wrap-dev})
