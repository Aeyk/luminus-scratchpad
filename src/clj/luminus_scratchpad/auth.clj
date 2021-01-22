(ns luminus-scratchpad.auth
  (:require
   [buddy.auth.backends :refer [jws]]
   [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
   [buddy.hashers :as hashers]
   [luminus-scratchpad.config :refer [env]]
   [luminus-scratchpad.jwt :as jwt]
   [luminus-scratchpad.db.core :as db]))

(def token-backend
  (jws {:secret (:auth-key env)
        :options {:alg :hs512}}))
