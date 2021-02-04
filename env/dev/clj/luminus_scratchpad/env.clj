(ns luminus-scratchpad.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [shadow.cljs.devtools.server :as server]
    [shadow.cljs.devtools.api :as shadow]
    [mount.core :as mount]
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
