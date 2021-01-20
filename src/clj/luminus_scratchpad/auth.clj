(ns luminus-scratchpad.auth
  (:require
   [buddy.auth.backends :refer [jws]]
   [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
   [buddy.hashers :as hashers]
   [luminus-scratchpad.config :refer [env]]
   [luminus-scratchpad.jwt :as jwt]
   [luminus-scratchpad.db.core :as db]))

(defn basic-auth
  [request {:keys [username password]}]
  (let [user (db/get-user-by-username username)]
    (if (and user (hashers/check password (:password user)))
      (-> user
          (dissoc :password)
          (assoc :token (jwt/create-token user)))
      false)))

(defn basic-auth-backend
  []
  (http-basic-backend {:authfn (partial basic-auth)}))

(def token-backend
  (jws {:secret (:auth-key env)
        :options {:alg :hs512}}))
