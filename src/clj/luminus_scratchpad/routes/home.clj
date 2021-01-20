(ns luminus-scratchpad.routes.home
  (:require
   [luminus-scratchpad.layout :as layout]
   [luminus-scratchpad.db.core :as db]
   [cheshire.core :as json]
   [luminus-scratchpad.jwt :as jwt]
   [clojure.java.io :as io]
   [luminus-scratchpad.middleware :as middleware]
   [ring.util.response]
   [buddy.hashers :as hashers]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(defn login-handler [request]
  (let [email (get-in request [:body :email])
        password (get-in request [:body :password])
        valid?   (hashers/check
                  password
                  (:password
                   (db/get-user-by-email {:email email})))]
    (if valid?
      (ok (jwt/create-token email))
      (bad-request (str request) #_{:auth [email password]
                                :message "wrong auth data"}))))


(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/user"
    {:middleware [middleware/auth]
     :get
     {:handler
      (fn [req]
        (-> (response/ok "1")
            (response/header "Content-Type" "text/plain; charset=utf-8")))}}]

   ["/actions/login"
    {:post
     {:handler
      #_login-handler
      (fn [{:keys [identity] :as req}]
        (try
          (do
            #_(db/login! identity)
            {:status 201
             :clear (str req)
             :body
             (json/encode (jwt/sign {:id (:id identity)}))})
          (catch clojure.lang.ExceptionInfo e
            {:status 401
             :body   {:status (ex-data e)}})))}}]

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
               (json/encode (jwt/sign {:id (:id user)}))
               #_{:status "OK"}})
            (catch clojure.lang.ExceptionInfo e
              {:status 401
               :body   {:status (ex-data e)}}))))}}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

