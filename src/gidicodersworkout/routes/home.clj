(ns gidicodersworkout.routes.home
  (:require [gidicodersworkout.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [gidicodersworkout.db.dbaccess :as the-db]
            
            [digest :as digest]))



(defn login-page []
  (layout/render "loginScreen.html"))


(defn home-page [user-record]
  #_(layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)})
  
  (layout/render "homePage.html"))

(defn about-page []
  (layout/render "about.html"))


(defn index-page [the-request]
  (let [the-coders (the-db/get-users)
        the-cookies (:cookies the-request)
        the-workouts (the-db/get-workouts)]
    (if (empty? the-cookies)
      (layout/render "index.html" {:coders the-coders 
                                   :workouts the-workouts})
      (let [userid-cookie (the-cookies :userId)]
        (if userid-cookie
          (let [the-user (the-db/get-user-by-id userid-cookie)]
            (if (empty? the-user)
              (do (println "Couldn't find user with userId: " userid-cookie)
                  (layout/render "index.html" {:coders the-coders
                                               :workouts the-workouts}))
              (do 
                (println "Got the user with userId: " userid-cookie) 
                (layout/render "index.html" (:user the-user
                                                   :coders the-coders
                                                   :workouts the-workouts)))))
          (do (println "No cookies with :userId key")
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
                       {:auth_error "Password doesn't match"}))
      (layout/render "loginScreen.html" 
                     {:auth_error "Username does NOT exist!"}))))

(defn signUserUp [first-name last-name username 
                  email-add password]
  (let [the-user (the-db/get-user-by-name username)]
    (if the-user
      (layout/render "loginScreen.html" 
                     {:auth_error "A user with that username already exists!"})
      (do 
        (let [new-user (the-db/add-user first-name last-name 
                                        username email-add password)]
          (home-page new-user))))))


#_(defn view-workout-entry [workouts-entry-id]
  (layout/render "createWorkout.html"))

(defn create-workout []
  (layout/render "createWorkout.html"))


#_(defn set-cookie []
  (-> "cookie set" response (update-in [:cookies "username" :value] "Alice"))
  {:status 200 :body "Hello World"})



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
  
  (GET "/createWorkout" [] (create-workout)))

;; postgres timestamp format
;; 2002-12-31 16:00:00
