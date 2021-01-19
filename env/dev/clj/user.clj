(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [luminus-scratchpad.config :refer [env]]
    [clojure.pprint]
    [clojure.spec.alpha :as s]
    [expound.alpha :as expound]
    [mount.core :as mount]
    [luminus-scratchpad.core :refer [start-app]]
    [luminus-scratchpad.db.core]
    [conman.core :as conman]
    [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'luminus-scratchpad.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'luminus-scratchpad.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'luminus-scratchpad.db.core/*db*)
  (mount/start #'luminus-scratchpad.db.core/*db*)
  (binding [*ns* (the-ns 'luminus-scratchpad.db.core)]
    (conman/bind-connection luminus-scratchpad.db.core/*db* "sql/queries.sql")))

(defn reset-db
  "Resets database."
  []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate
  "Migrates database up for all outstanding migrations."
  []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback
  "Rollback latest database migration."
  []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  (migrations/create name (select-keys env [:database-url])))

(comment
  (do
    (reset-db)
    (create-migration "enable-uuid-ossp")
    (create-migration "add-users-table")))

