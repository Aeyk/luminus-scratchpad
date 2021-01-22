(ns luminus-scratchpad.routes.home
  (:require
   [luminus-scratchpad.layout :as layout]
   [luminus-scratchpad.db.core :as db]
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
                                           (:identity request))}))}]
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
                    :from_user_id (:id (db/get-user-by-email {:email "mksybr@gmail.com"}))}
               #_(db/insert-message! (:body-params request)))
              (catch Exception e
                (str (.getCause e))))))}}]
   
;; * Come with Luminus, just docs
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])


