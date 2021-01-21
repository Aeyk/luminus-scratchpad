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
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(defn login-handler [request]
  (let [email (get request :email)
        password (get request :password)
        valid?   (hashers/check
                  password
                  (:password
                   (db/get-user-by-email {:email email})))]
    (ok (str [email password request]))
    #_(if valid?
      (do
        (ok (jwt/create-token email))
        (resp/redirect "/me"))
      (bad-request {:auth [email password]
                    :message "wrong auth data"}))))


(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]

   ["/me"
    {:middleware []
     :get
     (fn [req]
       (if (:identity req)
         {:status 200 :body {:status (:identity req)} }
         {:status 200 :body {:status "ANON"} }))}]

   ["/actions/login"
    {:post
     {
      :middleware []
      :handler
      login-handler}}]

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
              {:status 201
               :body
               {#_#_:identity user
                :identity (json/encode (jwt/sign {:id (:id user)}))
                :jwt (json/encode (jwt/sign {:id (:id user)}))}})
            (catch clojure.lang.ExceptionInfo e
              {:status 401
               :body   {:status (ex-data e)}}))))}}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

