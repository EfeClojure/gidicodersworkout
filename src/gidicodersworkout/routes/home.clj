(ns gidicodersworkout.routes.home
  (:require [gidicodersworkout.layout :as layout]
            [schema.core :as s]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok found]]
            [ring.util.response :refer [response]]
            [clojure.java.io :as io]
            [gidicodersworkout.db.dbaccess :as the-db]
            
            [taoensso.timbre :as timbre :only [trace debug info 
                                               warn error fatal]]
            [selmer.filters :as filters]
            [digest :as digest]
            [clojure.data.json :as json])
  (:import [java.net URLEncoder]))


(defonce username-no-exists "That username does NOT exist.")
(defonce username-exists "That username already exists.")
(defonce password-no-match "Password does NOT match.")

#_(filters/add-filter! :ellipsify #(if (> (.length %) 3) 
                                     (.substring % 0 3) %))

#_(defn render-with-cookie [page-response cookie-key cookie-val]
  (println "cookie-key : " cookie-key " cookie-val: " cookie-val)
  (-> page-response 
      response 
      (assoc :cookie {cookie-key {:value cookie-val}})))


(defn login-page []
  (layout/render "loginScreen.html"))


(defn home-page [user-id _error]
  (let [the-user (the-db/get-user-by-id user-id)
        my-workouts (the-db/get-workout-by-userId user-id)
        my-entries (the-db/get-user-entries-by-userId user-id)
        all-workouts (the-db/get-workouts)]
    (layout/render "home.html" 
                   {:user the-user
                    :all-workouts all-workouts
                    :my-workouts my-workouts
                    :my-entries my-entries
                    :_error _error})))

#_(defn get-workout-details [workout-id] 
  (let [the-workout (the-db/get-workout-by-id workout-id)]
    (if (not (empty? the-workout))
      {:status 200 :headers {"Content-Type" "application/json"}  
       :body (json/write-str (first the-workout))}
      {:status 200 :body ""})))

(defn submission-page [workout-id user-id]
  (let [the-workout (the-db/get-workout-by-id workout-id)
        the-submitter (the-db/get-user-by-id user-id)]
    (if (not-empty the-workout)
      (layout/render "submissionScreen.html" 
                     {:the_workout the-workout 
                      :the_submitter the-submitter})
      (layout/render "submissionScreen.html"  {}))))

(defn acc-lang [lang]
  "Accepted language: To get the id for the language name. 
Will return nil if the language is non-existent"
  (let [the-langs (the-db/get-languages)
        found (for [langs the-langs
                    :when (.equalsIgnoreCase (langs :language_name) 
                                             lang)]
                (langs :language_id))]
    (if (not (empty? found))
      (first found)
      nil)))

(defn accept-submission [workoutId userId languageName sourceText]
  (let [lang-id (acc-lang languageName)
        submitted-before (the-db/user-submitted? workoutId userId)]
    (if (not submitted-before)
      (do 
        (let [added-entry (the-db/add-workout-entry workoutId userId 
                                                    lang-id sourceText)]
          (timbre/info "Added entry: " added-entry))
        (found (str "/homePage?userId=" userId "&_error=")))
      (do (timbre/info "entry already made for workout" workoutId
                       " by user: " userId) 
          (found (str "/homePage?userId=" userId "&_error=" 
                      (URLEncoder/encode "The user has already made a submission" 
                                         "UTF-8")))))))

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
        #_(home-page (the-user :user_id))
        (found (str "/homePage?userId=" 
                    (the-user :user_id) "&_error="))
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
          (found (str "/homePage?userId=" (the-user :user_id) 
                      "&_error=")))))))


(defn create-workout [username title desc start end]
  (timbre/info "Create workout endpoint reached!!!")
  
  (let [the-user (the-db/get-user-by-name username)]
    (if the-user
      (do 
        (the-db/add-workout (the-user :user_id) title desc start end)
        (found (str "/homePage?userId=" (the-user :user_id) 
                    "&_error=")))
      (found (str "/homePage?userId=" (the-user :user_id) 
                  "&_error=" (URLEncoder/encode "Who are you? You don't exist!" 
                                         "UTF-8"))))))

(defn view-all-entries [workout-id workout-title]
  (let [the-entries (the-db/get-workout-entries workout-id)]
    (layout/render "viewAllEntriesForWorkout.html" 
                   {:workout-entries the-entries
                    :workout-title workout-title})))

(defn about-page []
  (layout/render "about.html"))



(defroutes home-routes
  (GET "/" [] (fn [req] (index-page req)))
  (GET "/about" [] (about-page))

  (GET "/login" [] (login-page))
  (GET "/homePage" [userId _error] 
       (home-page userId _error))

  (POST "/logUserIn" [username password] 
        (fn [req] (logUserIn req username password)))
  (POST "/signUserUp" [firstname lastname username email password]
        (signUserUp firstname lastname username 
                    email password))
  
  (POST "/createWorkout" [username title desc startDateInput endDateInput] 
        (create-workout username title desc startDateInput endDateInput))
  #_(GET "/getWorkout" [workoutId] 
       (get-workout-details workoutId))
  (GET "/submissionPage" [workoutId userId] 
       (submission-page workoutId userId))
  (POST "/acceptSubmission" [workoutId userId languageName sourceText] 
        (accept-submission workoutId userId languageName sourceText))

  (GET "/viewWorkoutEntries" [workoutId workoutTitle] 
       (view-all-entries workoutId workoutTitle)))

;; postgres timestamp format
;; 2002-12-31 16:00:00
