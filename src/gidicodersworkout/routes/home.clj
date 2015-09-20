(ns gidicodersworkout.routes.home
  (:require [gidicodersworkout.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :refer [response]]
            [clojure.java.io :as io]
            [gidicodersworkout.db.dbaccess :as the-db]
            
            [taoensso.timbre :as timbre :only [trace debug info 
                                               warn error fatal]]
            [digest :as digest]))


(defonce username-no-exists "That username does NOT exist.")
(defonce username-exists "That username already exists.")
(defonce password-no-match "Password does NOT match.")

#_(defn render-with-cookie [page-response cookie-key cookie-val]
  (println "cookie-key : " cookie-key " cookie-val: " cookie-val)
  (-> page-response 
      response 
      (assoc :cookie {cookie-key {:value cookie-val}})))


(defn login-page []
  (layout/render "loginScreen.html"))


(defn home-page [user-record]
  (let [user-name (user-record :username)
        my-workouts (the-db/get-workout-by-username user-name)
        my-entries (the-db/get-user-workout-entries user-name)]
    (layout/render "home.html" 
                   {:user user-record
                    :my-workouts my-workouts
                    :my-entries my-entries})))

(defn about-page []
  (layout/render "about.html"))


(defn index-page [the-request]
  (let [the-cookies (:cookies the-request)
        the-coders (the-db/get-users)
        the-workouts (the-db/get-workouts)]
    (if (empty? the-cookies)
      (layout/render "index.html" {:coders the-coders 
                                   :workouts the-workouts})
      (let [username-cookie (the-cookies "username")]
        (if username-cookie
          (let [the-user (the-db/get-user-by-name username-cookie)]
            (if (empty? the-user)
              (do (println "Couldn't find user with username: " username-cookie)
                  (layout/render "index.html" {:coders the-coders
                                               :workouts the-workouts}))
              (do 
                (println "Got the user with username: " username-cookie) 
                (home-page the-user))))
          (do (println "No cookies with :username key")
            (layout/render "index.html" 
                           {:coders the-coders
                            :workouts the-workouts})))))))


(defn logUserIn [req username password]
  (let [the-user (the-db/get-user-by-name username)]
    (if the-user
      (if (= (digest/md5 password) 
             (:password the-user))
        (home-page the-user)
        (layout/render "loginScreen.html"
                       {:auth_error password-no-match}))
      (layout/render "loginScreen.html" 
                     {:auth_error username-no-exists}))))

(defn signUserUp [first-name last-name username 
                  email-add password]
  (let [the-user (the-db/get-user-by-name username)]
    (if the-user
      (layout/render "loginScreen.html" 
                     {:auth_error username-exists})
      (do 
        (let [new-user (the-db/add-user first-name last-name 
                                        username email-add password)]
          (home-page new-user))))))


#_(defn view-workout-entry [workouts-entry-id]
  (layout/render "createWorkout.html"))

(defn create-workout [username title desc start end]
  (timbre/info "Create workout endpoint reached!!!")
  
  (let [the-user (the-db/get-user-by-name username)]
    (if the-user
      (do 
        (the-db/add-workout (the-user :user_id) title desc start end)
        (home-page the-user))
      {:status 500 :headers {"Content-Type" "text/html"} 
       :body  (pr-str ["Hello" :from 'Refresh])})))



(defroutes home-routes
  (GET "/" [] (fn [req] (index-page req)))
  (GET "/about" [] (about-page))

  (GET "/login" [] (login-page))

  (POST "/logUserIn" [username password] 
        (fn [req] (logUserIn req username password)))
  (POST "/signUserUp" [firstname lastname username 
                       email password]
        (signUserUp firstname lastname username 
                    email password))
  
  (POST "/createWorkout" [username title desc startDateInput endDateInput] 
        (create-workout username title desc startDateInput endDateInput)))

;; postgres timestamp format
;; 2002-12-31 16:00:00
