(ns luminus-scratchpad.email
  (:require
   [luminus-scratchpad.config :as config]
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [clojure.java.io :as io]
   [postal.core :as postal]))


(defprotocol Emailer
  (send! [config message]))

(def templates
  {:en
   {:permissions-updated
    {:subject "Permissions Updated"
     :html    "<html><body>Hello!</body></html>"
     :text    "Hello!"}
    :magic-link
    {:portal
     {:subject "Jää- ja uimahalliportaalit ovat nyt Lipaksessa"
      :html    "<html><body>Hello!</body></html>"
      :text    "Hello!"}
     :lipas
     {:subject "LIPAS sisäänkirjautumislinkki"
      :html    "<html><body>Hello!</body></html>"
      :text    "Hello!"}}
    :reminder
    {:subject "LIPAS-muistutus"
     :html    "<html><body>Hello!</body></html>"
     :text    "Hello!"}}
#_#_   :fi
   {:permissions-updated
    {:subject "Käyttöikeutesi on päivitetty"
     :html    (slurp (io/resource "email_templates/permissions_updated_fi.html"))
     :text    (slurp (io/resource "email_templates/permissions_updated_fi.txt"))}
    :magic-link
    {:portal
     {:subject "Jää- ja uimahalliportaalit ovat nyt Lipaksessa"
      :html    (slurp (io/resource "email_templates/magic_link_portal_fi.html"))
      :text    (slurp (io/resource "email_templates/magic_link_portal_fi.txt"))}
     :lipas
     {:subject "LIPAS sisäänkirjautumislinkki"
      :html    (slurp (io/resource "email_templates/magic_link_lipas_fi.html"))
      :text    (slurp (io/resource "email_templates/magic_link_lipas_fi.txt"))}}
    :reminder
    {:subject "LIPAS-muistutus"
     :html    (slurp (io/resource "email_templates/reminder_fi.html"))
     :text    (slurp (io/resource "email_templates/reminder_fi.txt"))}}})

(defn send*!
  "Thin wrapper for postal."
  [{:keys [host user pass from]}
   {:keys [to subject plain html]}]
  (postal/send-message
   {:host host
    :user user
    :pass pass
    :ssl  true}
   {:from    from
    :to      to
    :subject subject
    :body    [:alternative
              {:type "text/plain" :content plain}
              {:type "text/html;charset=utf-8" :content html}]}))

(defn send-reset-password-email!
  [emailer to {:keys [link]}]
  (.send! emailer {:subject "Salasanan vaihtolinkki"
                   :to      to
                   :plain   (str link)
                   :html    (str "<html><body>" link "</body></html>")}))

(defn send-magic-login-email!
  [emailer to variant {:keys [link valid-days]}]
  (.send! emailer {:subject (-> templates :en :magic-link variant :subject)
                   :to      to
                   :plain   (-> templates
                                :en
                                :magic-link
                                variant
                                :text
                                (str/replace "{{link}}" link)
                                (str/replace "{{valid-days}}" (str valid-days)))
                   :html    (-> templates
                                :en
                                :magic-link
                                variant
                                :html
                                (str/replace "{{link}}" link)
                                (str/replace "{{valid-days}}" (str valid-days)))}))

(defn send-register-notification!
  [emailer to user]
  (.send! emailer {:subject "Uusi rekisteröitynyt käyttäjä"
                   :to      to
                   :plain   (with-out-str (pprint/pprint user))
                   :html    (str "<html><body>"
                                 (with-out-str (pprint/pprint user))
                                 "</body></html>")}))

(defn send-permissions-updated-email!
  [emailer to {:keys [link valid-days]}]
  (.send! emailer {:subject (-> templates :en :permissions-updated :subject)
                   :to      to
                   :plain   (-> templates
                                :en
                                :permissions-updated
                                :text
                                (str/replace "{{link}}" link)
                                (str/replace "{{valid-days}}" (str valid-days)))
                   :html    (-> templates
                                :en
                                :permissions-updated
                                :html
                                (str/replace "{{link}}" link)
                                (str/replace "{{valid-days}}" (str valid-days)))}))

(defn send-reminder-email!
  [emailer to {:keys [link valid-days]} {:keys [message]}]
  (.send! emailer {:subject (-> templates :en :reminder :subject)
                   :to      to
                   :plain   (-> templates
                                :en
                                :reminder
                                :text
                                (str/replace "{{message}}" message)
                                (str/replace "{{link}}" link)
                                (str/replace "{{valid-days}}" (str valid-days)))
                   :html    (-> templates
                                :en
                                :reminder
                                :html
                                (str/replace "{{message}}" message)
                                (str/replace "{{link}}" link)
                                (str/replace "{{valid-days}}" (str valid-days)))}))

(defrecord SMTPEmailer [config]
  Emailer
  (send! [_ message] (send*! config message)))

(defrecord TestEmailer []
  Emailer
  (send! [_ message] {:status "OK"}))


(comment
  (require '[luminus-scratchpad.config :as config])
  
(:emailer config/default-config)

(.send! emailer2
 {:from "mksybr@gmail.com"
  :to "mksybr@gmail.com"
  :subject "Hello!"
  :body "Hello!"})

  (def emailer (SMTPEmailer. (-> config/default-config :emailer)))
  (def emailer (SMTPEmailer. (-> config/default-config :emailer)))
  (def emailer2 (SMTPEmailer. (-> config/default-config
                                  :emailer
                                  (assoc :from "mksybr@gmail.com"))))
  emailer2
  (send-permissions-updated-email! emailer2 "mksybr@gmail.com"
                                   {:link "www.kissa.fi" :valid-days 1}))
