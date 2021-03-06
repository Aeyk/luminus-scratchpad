(ns luminus-scratchpad.routes.home
  (:require
   [luminus-scratchpad.layout :as layout]
   [clojure.tools.logging :as log]
   [org.httpkit.server
    :refer [as-channel send! with-channel on-close on-receive]]
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
   [cognitect.transit :as t]
   [ring.util.http-response :as response]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.encore    :as encore :refer (have have?)]
   [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
   [taoensso.sente     :as sente]))

(defn set-user! [id {session :session}]
  (-> (resp/response (str "User set to: " id))
      (assoc :session (assoc session :user id))
      (assoc :headers {"Content-Type" "text/plain"})))

;; * Sentes Websockets
(let [;; Serialization format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      chsk-server
      (sente/make-channel-socket-server!
       (get-sch-adapter) {:packer packer})

      {:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      chsk-server]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defn broadcast-to-all-connections [payload]
  (for [connection (:any @connected-uids)]
    (chsk-send! connection payload)))

;; We can watch this atom for changes if we like
(add-watch connected-uids :connected-uids
           (fn [_ _ old new]
             (when (not= old new)
               (infof "Connected uids change: %s" new))))

;; * Sentes Event Handlers
(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

#_(defmethod -event-msg-handler :example/test-rapid-push
  [ev-msg] (test-fast-server>user-pushes))

#_(defmethod -event-msg-handler :example/toggle-broadcast
  [{:as ev-msg :keys [?reply-fn]}]
  (let [loop-enabled? (swap! broadcast-enabled?_ not)]
    (?reply-fn loop-enabled?)))

(defmethod -event-msg-handler :messages/recent
  [{:as ev-msg :keys [id ?data event ?reply-fn]}]
  (let [old-msgs (db/get-most-recent-messages {:count 7})]
    (log/info old-msgs)
    (doseq [uid (:any @connected-uids)]
      (chsk-send! uid [:chsk/state [old-msgs]]))
    (when ?reply-fn
      (?reply-fn {:old-messages old-msgs
                  :umatched-event-as-echoed-from-server event})))) 

;; * WebSocket Utils  etc
(defonce channels (atom #{}))

(defn persist-event! [_ event]
  (db/event! {:event event}))

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
(defn websocket-handler' [request]
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
        user (db/get-user-by-email {:email email})
        session (:session request)
        valid?   (hashers/check
                  password
                  (:password
                    user))]
    (if valid?
      (do
        (let [claims {:user (keyword email)
                      :exp (.plusSeconds
                            (java.time.Instant/now) 3600)
                      #_(time/plus (time/now) (time/seconds 3600))}
              token (jwt/sign claims)]
          (ok
           (set-user!
            {:session (assoc session :uid email)
             :identity email
             :token token})))
        #_{:body {:identity (jwt/create-token {:id email})}})
      (bad-request {:auth [email password]
                    :message "Incorrect Email or Password."}))))
;; * Routes
(defn home-routes []
  [""
   {}
   ["/" {:middleware [#_#_middleware/wrap-csrf
                      middleware/wrap-formats]
         :get home-page}]
   ;; * Get Request and Response
   ["/params"
    {:get
     (fn [req]
       {:headers {"Content-Type" "text/plain"}
        :status 200
        :body (:session req)})}]
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
        (ok
         (try
           (let [message
                 {:content
                  (-> request :body-params :message)
                  :from_user_id
                  (:id
                   (db/get-user-by-username
                    {:username (-> request :body-params :whoami)}))}]
             (db/event! {:event 
                         {:message message}})
             (db/insert-message!
              message))
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
   ["/ws" {:post ring-ajax-post
           :get ring-ajax-get-or-ws-handshake}]
   
   #_["/ws" websocket-handler]
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
              (send! channel message))))
  :stop (db/remove-listener
         db/notifications-connection
         event-listener))


