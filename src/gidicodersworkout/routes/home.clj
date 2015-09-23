(ns gidicodersworkout.routes.home
  (:require [gidicodersworkout.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :refer [response]]
            [clojure.java.io :as io]
            [gidicodersworkout.db.dbaccess :as the-db]
            
            [taoensso.timbre :as timbre :only [trace debug info 
                                               warn error fatal]]
            [selmer.filters :as sel-fils]
            [digest :as digest]
            [clojure.data.json :as json]))


(defonce username-no-exists "That username does NOT exist.")
(defonce username-exists "That username already exists.")
(defonce password-no-match "Password does NOT match.")

(sel-fils/add-filter! :ellipsify 
                      #(if (> (count %) 3) 
                         (.substring % 0 3) %))

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
        my-entries (the-db/get-user-workout-entries user-name)
        all-workouts (the-db/get-workouts)]
    (layout/render "home.html" 
                   {:user user-record
                    :all-workouts all-workouts
                    :my-workouts my-workouts
                    :my-entries my-entries})))

(defn get-workout-details [workout-id] 
  (let [the-workout (the-db/get-workout-by-id workout-id)]
    (if (not (empty? the-workout))
      {:status 200 :body (json/write-str (first the-workout))}
      {:status 200 :body ""})))


(defn index-page [the-request]
  (let [the-coders (the-db/get-users)
        the-workouts (the-db/get-workouts)]
    (layout/render "index.html" {:coders the-coders 
                                 :workouts the-workouts})))


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


(defn about-page []
  (layout/render "about.html"))



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
        (create-workout username title desc startDateInput endDateInput))
  (GET "/getWorkout" [workoutId] (get-workout-details workoutId)))

;; postgres timestamp format
;; 2002-12-31 16:00:00
