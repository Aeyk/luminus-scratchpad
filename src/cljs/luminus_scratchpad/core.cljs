(ns luminus-scratchpad.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [luminus-scratchpad.websockets :as ws]
    [luminus-scratchpad.ajax :as ajax]
    [luminus-scratchpad.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string]
    [ajax.core :refer [GET POST]]
    [luminus-scratchpad.views :as views])
  (:import goog.History
           (goog.crypt Hmac Sha512)))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'views/home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/signup" {:name :signup
                 :view #'views/signup-page}]
     ["/login" {:name :login
                :view #'views/login-page}]
     ["/about" {:name :about
                :view #'views/about-page}]
     ["/me" {:name :me
             :view #'views/me-page}]
     ["/chat" {:name :chat
             :view #'views/chat-page}]]))


(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)

  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws")
                      views/update-messages!)
  (if (not (nil? (js/localStorage.getItem "scratch-client-name")))
    (reset! views/current-user (js/localStorage.getItem "scratch-client-name")))
  (rdom/render [#'views/page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
