(ns luminus-scratchpad.routes.home
  (:require
   [luminus-scratchpad.layout :as layout]
   [luminus-scratchpad.db.core :as db]
   [clojure.java.io :as io]
   [luminus-scratchpad.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/user"
    {:middleware []
     :get
     {:handler
      (fn [req]
        (-> (response/ok "1")
            (response/header "Content-Type" "text/plain; charset=utf-8")))}}]

   ["/actions/login"
    {:post
     {:middleware [middleware/auth]
      :handler
      (fn [{:keys [identity]}]
        (db/login! identity)
        {:status 200 :body identity})}}]

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
               {:status "OK"}})
            (catch clojure.lang.ExceptionInfo e
              {:status 401
               :body   {:status (ex-data e)}}))))}}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

