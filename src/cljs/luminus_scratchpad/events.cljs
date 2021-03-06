(ns luminus-scratchpad.events
  (:require
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.controllers :as rfc]))

;;dispatchers
(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success       [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-event-fx
  :page/init-home
  (fn [_ _]
    {:dispatch [:fetch-docs]}))

;;subscriptions

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))


;; *  event
(rf/reg-event-db
 :events
 (fn [db [_ event]]
   (update db :events (fnil conj []) event)))

(rf/reg-event-fx
 :fetch-latest-messages
 (fn [_ _]
   {:http-xhrio
    {:method          :get
     :uri             "/query/messages"
     :response-format (ajax/raw-response-format)
     :on-success [:load-fetched-messages]
     :on-failure []}}))

(rf/reg-event-db
 :load-fetched-messages
 (fn [db [_ result]]
   (assoc db :chat/messages 
          (js->clj (js/JSON.parse result)
                   :keywordize-keys true))))

(rf/reg-sub
 :events
 (fn [db _]
   {:dispatch [:fetch-latest-messages
               :load-fetched-messages]
    :chat/messages
    (map :content (:chat/messages db))
    :events (:events db)}))
