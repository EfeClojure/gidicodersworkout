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
        the-cookies (:cookies the-request)]
    (if (empty? the-cookies)
      (layout/render "index.html" {:coders the-coders})
      (let [userid-cookie (the-cookies :userId)]
        (if userid-cookie
          (let [the-user (the-db/get-user-by-id userid-cookie)]
            (if (empty? the-user)
              (do (print "Couldn't find user with userId: " userId)
                  (layout/render "index.html" {:coders the-coders}))
              (do 
                (print "Got the user with userId: " userId) 
                (layout/render "index.html" (:user the-user
                                                   :coders the-coders)))))
          (do (print "No cookies with :userId key")
            (layout/render "index.html" 
                           {:coders the-coders})))))))


(defn logUserIn [username password]
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


(defroutes home-routes
  (GET "/" [] (fn [req] (index-page req)))
  (GET "/about" [] (about-page))

  (GET "/login" [] (login-page))

  (POST "/logUserIn" [username password] 
        (logUserIn username password))
  (POST "/signUserUp" [firstname lastname username 
                       email password]
        (signUserUp firstname lastname username 
                    email password)))

