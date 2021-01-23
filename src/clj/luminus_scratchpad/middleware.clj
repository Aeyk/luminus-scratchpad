(ns luminus-scratchpad.middleware
  (:require
   [ring.util.http-response :as resp]
   [ring.middleware.flash :refer [wrap-flash]]
   [luminus-scratchpad.env :refer [defaults]]
   [luminus-scratchpad.auth :as auth]
   [clojure.tools.logging :as log]
   [luminus-scratchpad.layout :refer [error-page]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [luminus-scratchpad.middleware.formats :as formats]
   [muuntaja.middleware :refer [wrap-format wrap-params]]
   [luminus-scratchpad.config :refer [env]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth.backends.token :refer [jwe-backend]]
   [buddy.sign.jwt :refer [encrypt]]
   [buddy.core.nonce :refer [random-bytes]]
   [buddy.sign.util :refer [to-timestamp]])
  (:import
   [java.util Calendar Date]
   ))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message (str "We've dispatched a team of highly trained gnomes to take care of the problem." (.getMessage t))})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn on-error [request response]
  (error-page
    {:status 403
     :title (str "Access to " (:uri request) " is not authorized")}))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-auth [handler]
  (-> handler
    (wrap-authentication auth/token-backend)
    (wrap-authorization auth/token-backend)))

;; (defn auth
;;   "Middleware used in routes that require authentication. If request is not
;;    authenticated a 401 not authorized response will be returned"
;;   [handler]
;;   (fn [request]
;;     (if (authenticated? request)
;;       (handler request)
;;       (resp/unauthorized {:error "Not authorized"}))))

;; (defn basic-auth [_]
;;   (fn [handler]
;;     (wrap-authentication handler (auth/basic-auth-backend _))))

(def allow-methods "GET, PUT, PATCH, POST, DELETE, OPTIONS")
(def allow-headers "Authorization, Content-Type")

(defn add-cors-headers [resp]
  (-> resp
      (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
      (assoc-in [:headers "Access-Control-Allow-Methods"] allow-methods)
      (assoc-in [:headers "Access-Control-Allow-Headers"] allow-headers)))

(defn cors
  "Cross-origin Resource Sharing (CORS) middleware. Allow requests from all
   origins, all http methods and Authorization and Content-Type headers."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (add-cors-headers response))))

;; (defn token-auth
;;   "Middleware used on routes requiring token authentication"
;;   [handler]
;;   (wrap-authentication handler auth/token-backend))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-formats
      wrap-flash
      (wrap-defaults
       (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in [:session :store] (ttl-memory-store (* 60 30)))))
      wrap-internal-error))
