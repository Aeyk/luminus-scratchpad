(ns luminus-scratchpad.views
  (:require [reagent.core :as r]
            [reitit.core :as reitit]
            [re-frame.core :as rf]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]))


(def current-user (r/atom nil))

(defn logout-handler []
  (reset! current-user nil)
  (js/localStorage.removeItem "scratch-client-key")
  (js/localStorage.removeItem "scratch-client-name"))

(defn logged-in? []
  (not (nil? @current-user)))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page])) :is-active)}
   title])

(defn sign-up-login []
  [:<>
   [nav-link "#/login" "Login" :login]
   [nav-link "#/signup" "Signup" :signup]])

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
       (if @current-user
         [:a.navbar-item
          {:on-click logout-handler}
          (str "Sign Out of " @current-user)]
         [sign-up-login])]]]))



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
                       :flash []})]
    (fn []
      [:section.section>div.container>div.content
       (if @current-user
         [:div (str "Hello " @current-user)]
         [:form.section
          {:method "POST"
           :action "/actions/login"
           :on-submit (fn [e]
                        (e.preventDefault)
                        (js/console.log e))}
          (if (not (empty? (:flash @state)))
            (let [[status text reason] (:flash @state)]
              [:div.card.flash.is-danger.columns
               [:p.subtitle.is-inline.column status]
               [:p.subtitle.is-inline.column text]
               [:p..subtitle.is-inline.column reason]]))
          [:div.field
           [:label.label "email"]
           [:input.input
            {:default-value (:email @state)
             :on-change #(swap! state assoc :email (-> % .-target .-value))}]]
          [:div.field
           [:label.label "password"]
           [:input.input.password
            {:type :password
             :default-value (:password @state)
             :on-change #(swap! state assoc :password (-> % .-target .-value))}]]
          [:div.control
           [:input.button.is-primary
            {:on-click
             (fn [e]
               (e.preventDefault)
               ;; * Login
               (POST
                "/actions/login"
                {:headers
                 {"Accept" "application/transit+json"
                  "x-csrf-token" js/csrfToken}
                 :params
                 @state
                 :handler
                 (fn [ok]
                   (swap! state assoc "x-csrf-token" js/csrfToken)
                   (swap! state assoc :flash
                          ["OK" (str "Logged In As " (:email @state))])

                   (js/localStorage.setItem "scratch-client-key" (:token ok))
                   (js/localStorage.setItem "scratch-client-name" (:identity ok))
                   (reset! current-user (js/localStorage.getItem "scratch-client-name")))
                 :error-handler
                 (fn [{:keys [status status-text fail response] :as err}]
                   (swap! state assoc :flash [status status-text (get-in response [:status :type])])
                   (js/console.log @state))})
               ;; * Check Auth
               (GET "/me"
                    {:headers {#_#_"Accept" "application/transit+json"
                               "x-csrf-token" js/csrfToken
                               "identity" (js/localStorage.getItem "scratch-client-key")
                               "Authorization"
                               (str "Token " (js/localStorage.getItem "scratch-client-key"))}
                     :handler (fn [ok] ok)} )
               )
             :default-value "Sign in"}]]])])))


(defn signup-page []
  (let [state (r/atom {:email ""
                       :username ""
                       :password ""
                       :password_confirmation ""
                       :flash []})]
    (fn []
      (if @current-user
        [:div (str "Hello " @current-user)]
        [:section.section>div.container>div.content
         [:form.section
          {:method "POST"
           :action "/actions/register"
           :on-submit (fn [e]
                        (e.preventDefault)
                        (js/console.log e))}
          (if (not (empty? (:flash @state)))
            (let [[status text reason] (:flash @state)]
              [:div.card.flash.is-danger.columns
               [:p.subtitle.is-inline.column status]
               [:p.subtitle.is-inline.column text]
               [:p..subtitle.is-inline.column reason]]))
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
                           {"Accept" "application/transit+json"}
                           :params (select-keys @state [:email :username :password])
                           #_(into {#_#_"x-csrf-token" js/csrfToken}
                                   @state)
                           :handler
                           (fn [ok]
                             (js/localStorage.setItem "scratch-client-key" (:token ok))
                             (js/localStorage.setItem "scratch-client-name" (:identity ok))
                             (reset! current-user (js/localStorage.getItem "scratch-client-name"))
                             
                             (swap! state assoc :flash ["" "OK" "User created"])
                             (GET "/me"
                                  {:headers {#_#_"Accept" "application/transit+json"
                                             "x-csrf-token" js/csrfToken
                                             "identity" (js/localStorage.getItem "scratch-client-key")
                                             "Authorization"
                                             (str "Token " (js/localStorage.getItem "scratch-client-key"))}
                                   :handler (fn [ok] ok)} ))
                           :error-handler
                           (fn [{:keys [status status-text fail response] :as err}]
                             (swap! state assoc :flash [status status-text (get-in response [:status :type])])
                             )}))
             :default-value "Register an account"}]]]]))))


(defn me-page []
  [:div
   (str "Hello User")]
  )

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn user-page []
  [:div "Hello"])
