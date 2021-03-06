(ns luminus-scratchpad.websockets
  (:require
   [clojure.string  :as str]
   [cljs.core.async :as async  :refer (<! >! put! chan)]
   [taoensso.encore :as encore :refer-macros (have have?)]
   [taoensso.timbre :as timbre :refer-macros (logf tracef debugf infof warnf errorf)]
   [taoensso.sente  :as sente  :refer (cb-success?)]

   ;; Optional, for Transit encoding:
   [taoensso.sente.packers.transit :as sente-transit])

  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(defn ->output! [fmt & args]
  (let [msg (apply encore/format fmt args)]
    (timbre/debugf msg)))

(->output! "ClojureScript appears to have loaded correctly.")

(def ?csrf-token
  js/csrfToken
  #_(when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(if ?csrf-token
  (->output! "CSRF token detected in HTML, great!")
  (->output! "CSRF token NOT detected in HTML, default Sente config will reject requests"))

(let [;; Serializtion format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
        "/ws" ; Must match server Ring routing URL
        ?csrf-token
        {:type   :ws
         :packer packer})]

  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))


(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] (have vector? ?data)]
    (if (:first-open? new-state-map)
      (->output! "Channel socket successfully established!: %s" new-state-map)
      (->output! "Channel socket state change: %s"              new-state-map))))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (->output! "Push event from server: %s" ?data))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (->output! "Handshake: %s" ?data)))

(defmethod -event-msg-handler :messages/recent
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (->output! "Data: %s" ?data)))


(go-loop []
  (let [{:as ev-msg :keys [event]} (<! (:ch-chsk chsk))]
    (js/console.log #_event-handler event)))

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
           ch-chsk event-msg-handler)))

;; (defonce ws-chan (atom nil))

; (defn send
;;   [message]
;;   (if @ws-chan
;;     (.send @ws-chan message)
;;     (throw (js/Error. "Websocket is not available!"))))

;; (defn connect-ws [url handler]
;;   (if-let [chan (js/WebSocket. url)]
;;     (do
;;       (set! (.-onmessage chan) #(-> % .-data handler))
;;       (reset! ws-chan chan))
;;     (throw (js/Error. "Websocket connection failed!"))))
