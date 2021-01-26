(ns luminus-scratchpad.routes.home
  (:require
   [luminus-scratchpad.layout :as layout]
   [clojure.tools.logging :as log]
   [org.httpkit.server
    :refer [send! with-channel on-close on-receive]]
   [luminus.http-server :as http]
   [luminus-scratchpad.db.core :as db]
   [mount.core :as mount]
   [cheshire.core :as json]
   [luminus-scratchpad.jwt :as jwt]
   [clojure.java.io :as io]
   [luminus-scratchpad.middleware :as middleware]
   [ring.util.response :as resp]
   [buddy.hashers :as hashers]
   [buddy.auth :as auth]
   [buddy.auth.accessrules :as acl]
   [ajax.core :refer [GET POST]]   
   [ring.util.http-response :as response]))

;; * WebSocket Utils  etc
(defonce channels (atom #{}))

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (log/info "channel closed:" status)
  (swap! channels #(remove #{channel} %)))

(defn notify-clients [msg]
  (doseq [channel @channels]
    (send! channel msg)))

;; * WebSocket Handler
(defn websocket-handler [request]
  (with-channel request channel
    (connect! channel)
    (on-close channel (partial disconnect! channel))
    (on-receive channel #(notify-clients %))))

;; * Main
(defn home-page [request]
  (layout/render request "home.html"))
;; * Request Helpers
(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})
;; * Login Handler
(defn login-handler [request]
  (let [email (get-in request [:body-params :email])
        password (get-in request [:body-params :password])
        valid?   (hashers/check
                  password
                  (:password
                   (db/get-user-by-email {:email email})))]
    (if valid?
      (do
        (let [claims {:user (keyword email)
                      :exp (.plusSeconds
                            (java.time.Instant/now) 3600)
                      #_(time/plus (time/now) (time/seconds 3600))}
              token (jwt/sign claims)]
          (ok {:identity email
               :token token}))
        #_{:body {:identity (jwt/create-token {:id email})}})
      (bad-request {:auth [email password]
                    :message "Incorrect Email or Password."}))))
;; * Routes
(defn home-routes []
  [""
   {}
   ["/" {:middleware [middleware/wrap-csrf
                      middleware/wrap-formats]
         :get home-page}]

;; * Check if logged in 
   ["/me"
    {:middleware [middleware/wrap-auth]
     :get
     (fn [request]
       #_(if-not (auth/authenticated? request)
         (auth/throw-unauthorized))
       (ok {:status "Logged" :message (str "hello logged user "
                                            request)}))}]
   ;; * Get Authentication Token for [email password]
   ["/actions/login"
    {:post
     {
      :middleware []
      :handler
      login-handler}}]
;; * Get Authentication Token for unclaimed email (plus password)
   ["/actions/register"
    {:post
     {:handler
      (fn [req]
        (let [user (-> req
                       :body-params
                       (dissoc :permissions))]
          (try
            (do
              (db/add-user! user)
              (login-handler req))
            (catch clojure.lang.ExceptionInfo e
              {:status 401
               :body   {:status (ex-data e)}}))))}}]
;; * Send message
   ["/actions/send"
    {:middleware [middleware/wrap-auth]
     :post
     {:handler
      (fn [request]
        (ok (try
              (db/insert-message!
                   {:content (-> request :body-params :message)
                    :from_user_id
                    (:id
                     (db/get-user-by-username {:username (-> request :body-params :whoami)}))}
               #_(db/insert-message! (:body-params request)))
              (catch Exception e
                (str (.getCause e))))))}}]
;; * Index messages
   ["/query/messages"
    {:middleware [middleware/wrap-auth]
     :get
     {:handler
      (fn [request]
        (ok (try
              (db/get-most-recent-messages {:count 7})
              (catch Exception e
                (str (.getCause e))))))}}]

;; * WebSocket Routes
   ["/ws" websocket-handler]
;; * Come with Luminus, just docs
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])


;; * Event Listener
(mount/defstate channels
  :start (atom #{}))

(mount/defstate ^{:on-reload :noop} event-listener
  :start (db/add-listener
          db/notifications-connection
          :events
          (fn [_ _ message]
            (doseq [channel @channels]
              #_(http/send! channel message))))
  :stop (db/remove-listener
         db/notifications-connection
         event-listener))


