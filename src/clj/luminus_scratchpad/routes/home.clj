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

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(defn login-handler [request]
  (let [session (get-in request [:session])
        email (get-in request [:params :email])
        password (get-in request [:params :password])
        valid?   (hashers/check
                  password
                  (:password
                   (db/get-user-by-email {:email email})))]    
    (if valid?
      #_{:status 302
       :headers {"Location" "/me"}}
      {:body {:identity (jwt/create-token {:id email})}}
      (bad-request {:auth [email password]
                    :message "Incorrect Email or Password."}))))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]

   ["/me"
    {:middleware [middleware/token-auth]
     :get
     (fn [req]
       {:status 200
        :headers
        {#_#_"Authorization"
         (str "Bearer "
              (first
               (vals (select-keys (:headers req) ["identity"]))))}
        :body {:identity
               (get (:query-params req) "identity")}
        #_{"Authorization"
                   (str "Bearer "
                        (first
                         (vals (select-keys (:headers req) ["identity"]))))}
        #_(select-keys (:headers req) ["identity" "x-csrf-token"])})}]

   ["/actions/login"
    {:post
     {
      :middleware [#_#_(middleware/basic-auth {})
                   middleware/auth]
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
              {:status 200
               :params {"identity"
                        (json/encode (jwt/sign {:id (:id user)}))}
               :headers {"identity"
                         (json/encode (jwt/sign {:id (:id user)}))}

               :body
               {:token
                (json/encode (jwt/sign {:id (:id user)}))
                :test
                (str @(GET
                      "http://localhost:3000/me"
                      {:handler (fn [ok] ok)
                       :headers
                       {"identity"
                        (json/encode (jwt/sign {:id (:id user)}))}}))}})
            (catch clojure.lang.ExceptionInfo e
              {:status 401
               :body   {:status (ex-data e)}}))))}}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])


