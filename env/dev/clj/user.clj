(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [luminus-scratchpad.config :refer [env]]
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [mount.core :as mount]
   [luminus-scratchpad.core :refer [start-app]]
   [ring.util.response :as resp]
   [ring.util.request :as request]
   [buddy.auth.backends :refer [jws]]
   [luminus-scratchpad.auth :as auth]
   [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
   [luminus-scratchpad.jwt :as jwt]
   [luminus-scratchpad.middleware :as middleware]
   [luminus-scratchpad.db.core :as db]
   [conman.core :as conman]
   [buddy.hashers :as hashers]
   [ajax.core :refer [GET POST]]
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
    (mount/stop)
    (mount/start))


  (do 
    (reset-db)
    (migrate))

  )

(comment
  (mount/start)

  (db/insert-user! {:status "active"
                    :email "mksybr@gmail.com"
                    :username "mksybr"
                    :password
                    (hashers/derive "OceanicReterritorializationProcess")
                    :history (db/clj->jsonb-pgobj "{}")
                    :user_data (db/clj->jsonb-pgobj "{}")
                    :permissions (db/clj->jsonb-pgobj {:role :user})})

  (jwt/sign #_{:claims {}})

  (db/insert-message!
   {:content "asdfasdf"
    :from_user_id
    (:id (db/get-user-by-email {:email "mksybr@gmail.com"}))})

  (db/get-most-recent-messages {:count 1}) 

  (db/get-user-by-email {:email "1"})
  
  @(GET
    "http://localhost:3000/me"
    {:identity
     (get-in (luminus-scratchpad.routes.home/login-handler {:body-params {:email "z" :password "z"}}) [:body :token])
     })

  
  {:headers
     {identity
      (jwt/create-token {:id "mksybr@gmail.com"})}
     :handler (fn [ok] ok)}q
  
  (GET
   "/me"
   
   :headers
   {identity
    (jwt/create-token {:id "mksybr@gmail.com"})}
   :handler (fn [ok]
                ok)
   :error-handler (fn [err]
                      err))
  
  (hashers/check "4"
                 (:password (db/get-user-by-email {:email "4"})))
  
  (into (resp/redirect "/me")
        {:body {:identity (jwt/create-token {:id "4"})}})
  
  (auth/basic-auth {}
                   {:email "4" :password "4"})
  
  (let [{:keys [email password]}
        (db/get-user-by-email "4")]
    (hashers/check password password)
    [email password])
  {:email "1" :password ""}


  (create-migration "add-messages-table")

  (create-migration "enable-pg_notify")
  (do
    (mount/stop)
    (mount/start))

  (db/add-listener
   db/notifications-connection
   "events"
   (fn [& args]
     (apply println "got message:" args)))
  (db/event! {:event "Hello world!"})

  
  )
