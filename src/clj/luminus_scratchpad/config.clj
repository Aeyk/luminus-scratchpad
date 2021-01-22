(ns luminus-scratchpad.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]))


(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env)]))

(def default-config
  {
   #_#_#_#_#_#_#_#_#_#_#_#_#_
   :db ;; TODO
   :dbtype   "postgresql"
   :dbname   (:db-name env)
   :host     (:db-host env)
   :user     (:db-user env)
   :port     (:db-port env)
   :password (:db-password env)
   :emailer
   {:host (:smtp-host env)
    :user (:smtp-user env)
    :pass (:smtp-pass env)
    :from (:smtp-from env)}})
