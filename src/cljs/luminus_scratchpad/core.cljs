(ns luminus-scratchpad.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [luminus-scratchpad.ajax :as ajax]
    [luminus-scratchpad.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string]
    [ajax.core :refer [GET POST]])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "luminus-scratchpad"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]
                 [nav-link "#/login" "Login" :login]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn login-page []
  (let [state (r/atom {:email ""
                       :username ""
                       :password ""
                       :password_confirmation ""
                       :flash []})]
    (fn []
      [:section.section>div.container>div.content
       (if (not (empty? (:flash @state)))
         (let [[status text] (:flash @state)]
           [:div.flash.is-danger
            [:p.is-inline status]
            [:p.is-inline text]]))
       [:form.section
        {:method "POST"
         :action "/actions/register"
         :on-submit (fn [e]
                      (e.preventDefault)
                      (js/console.log e))}
        [:div.field
         [:label.label "email"]
         [:input.input
          {:default-value (:email @state)
           :on-change #(swap! state assoc :email (-> % .-target .-value))}]]
        [:div.field
         [:label.label "username"]
         [:input.input
          {:default-value (:username @state)
           :on-change #(swap! state assoc :username (-> % .-target .-value))}]]
        [:div.field
         [:label.label "password"]
         [:input.input.password
          {:type :password
           :default-value (:password @state)
           :on-change #(swap! state assoc :password (-> % .-target .-value))}]]
        [:div.field
         [:label.label "password confirmation"]
         [:input.input.password {:type :password
                                 :default-value (:password_confirmation @state)
                                 :on-change #(swap! state assoc :password_confirmation (-> % .-target .-value))}]]
        [:div.control
         [:input.button.is-primary
          {:on-click (fn [e]
                       (e.preventDefault)
                       (POST
                        "/actions/register"
                        {:headers
                         {"Accept" "application/transit+json"
                          "x-csrf-token" js/csrfToken}
                         :params
                         @state
                         :error-handler
                         (fn [{:keys [status status-text]}]
                           (swap! state assoc :flash [status status-text])
                           (js/console.log [status status-text]))}))
           :default-value "Register an account"}]]]])))


(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))


(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/login" {:name :login
                :view login-page}]
     ["/about" {:name :about
                :view #'about-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
