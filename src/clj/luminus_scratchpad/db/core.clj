(ns luminus-scratchpad.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [clojure.java.jdbc :as jdbc] ;; TODO update
    
    [buddy.hashers :as hashers]
    [next.jdbc.date-time]
    [next.jdbc.prepare]
    [next.jdbc.result-set]
    [clojure.tools.logging :as log]
    [conman.core :as conman]
    [luminus-scratchpad.config :refer [env]]
    [mount.core :refer [defstate]])
  (:import (com.impossibl.postgres.api.jdbc
            PGNotificationListener
            PGConnection)))

(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

#_(defn pgobj->clj [^org.postgresql.util.PGobject pgobj]
    (let [type (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (parse-string value true)
        "jsonb" (parse-string value true)
        "citext" (str value)
        value)))



(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v))
  java.sql.Array
  (read-column-by-label [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^java.sql.Array v _2 _3]
    (vec (.getArray v)))
  ;; TODO FIXME
  #_#_#_
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^org.postgresql.util.PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))

#_(defn clj->jsonb-pgobj [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol next.jdbc.prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^java.sql.PreparedStatement stmt ^long idx]
    (.setObject stmt idx v #_(clj->jsonb-pgobj v)))
  clojure.lang.IPersistentVector
  (set-parameter [^clojure.lang.IPersistentVector v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_)
                           (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx v #_(clj->jsonb-pgobj v))))))

(defn username-exists? [user]
  (some? (get-user-by-username user)))

(defn email-exists? [user]
  (some? (get-user-by-email user)))

(defn add-user! [user]
  (when (username-exists? user)
    (throw (ex-info "Username is already in use!"
                    {:type :username-conflict})))

  (when (email-exists? user)
    (throw (ex-info "Email is already in use!"
                    {:type :email-conflict})))

  (let [defaults {:permissions {:role :user} 
                  :status      "active" 
                  :email    (:email user)
                  :user_data   {}
                  :history {}
                  :password    "" #_(str (utils/gen-uuid))}
        user     (-> (merge defaults user)
                     (update :password hashers/encrypt))]

    (insert-user! user)
    {:status ["OK" user]}))

(defn timestamp
  "Returns current timestamp in \"2018-07-11T09:38:06.370Z\" format.
  Always UTC."
  []
  (.toString (java.time.Instant/now)))

(defn- add-user-event!
  ([user evt-name]
   (add-user-event! user evt-name {}))
  ([user evt-name data]
   (let [defaults {:event-date (timestamp) :event evt-name}
         evt      (merge defaults data)
         user     (update-in user [:history :events] conj evt)]     
     (update-user-history! (into user {:id (:id user)})))
   user))

(defn login! [user]
  (add-user-event! user "login"))



(defstate notifications-connection
  :start (jdbc/get-connection {:connection-uri (env :database-url)})
  :stop (.close notifications-connection))

(defn add-listener [conn id listener-fn]
  (let [listener (proxy [PGNotificationListener] []
                   (notification [chan-id channel message]
                     (listener-fn chan-id channel message)))]
    (.addNotificationListener conn listener)
    (jdbc/db-do-commands
     {:connection notifications-connection}
     (str "LISTEN " (name id)))
    listener))

(defn remove-listener [conn listener]
  (.removeNotificationListener conn listener))
