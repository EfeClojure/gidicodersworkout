(ns gidicodersworkout.db.dbaccess
  (:refer-clojure :exclude [update])
  (:require [immutant.web :as immutant]
            [digest :as digest]
            
            [clj-time.core :as tyme]
            [clj-time.format :as tyme-format]
            [clj-time.coerce :as tyme-coerce])
  (:use [korma.db :only (defdb postgres)]
        [korma.core]))



(defdb the-db (postgres {:db "gidicodersworkoutdb"
                         :user "postgres"
                         :password "asdffdsa"}))


(declare workouts users languages roles 
         workout-entries)


#_(defonce built-in-formatter (tyme-format/formatters :basic-date-time))
(defonce postgres-timestamp (tyme-format/formatter "yyyy-MM-dd hh:mm:ss"))
;; postgres timestamp format
;; 2002-12-31 16:00:00

;; valid time stamp: '2004-10-19 10:23:54'


(defentity workouts 
  (pk :workout_id)
  (table :workout)
  
  (entity-fields :workout_title :workout_text 
                 :start_date :end_date :is_active)
  (has-many workout-entries)

  (prepare (fn [{timestamp :start_date :as v}]
             (if timestamp
               (assoc v :start_date 
                      (tyme-format/unparse postgres-timestamp
                                           (tyme-coerce/from-long timestamp)))
                   v)))
  (transform (fn [{timestamp :start_date :as v}]
               (if timestamp
                 (assoc v :start_date (.getTime timestamp))
                 v)))
  (prepare (fn [{timestamp :end_date :as v}]
             (if timestamp
               (assoc v :end_date 
                      (tyme-format/unparse postgres-timestamp
                                           (tyme-coerce/from-long timestamp)))
                   v)))
  (transform (fn [{timestamp :end_date :as v}]
               (if timestamp
                 (assoc v :end_date (.getTime timestamp))
                 v))))

(defentity users
  (pk :user_id) 
  (table :usermaster)
  
  (entity-fields :first_name :last_name :email_address 
                 :username :password)

  (has-one roles {:fk :role_id}))

(defentity roles 
  (pk :role_id)
  (table :role)

  (entity-fields :role_title)
  (belongs-to users))

(defentity languages
  (pk :language_id)
  (table :language)
 
  (entity-fields :language_name))

(defentity workout-entries 
  (pk :workout_entry_id)
  (table :workout_entry)
  
  (entity-fields :source_text :date_sent)
  
  (belongs-to workouts)
  (has-one users)
  (has-one languages))

;; -- 

(defn get-users []
  (let [the-coders (select users 
                           (with roles))]
    (if (not (empty? the-coders))
      the-coders
      nil)))

(defn get-user-by-id [the-user-id]
  (let [results (select users (with roles)
                        (where {:user_id the-user-id}))]
    (if (not (empty? results))
      (first results)
      nil)))

(defn get-user-by-name [the-username]
  (let [results (select users (with roles)
                        (where {:username the-username}))]
    (if (not (empty? results))
      (first results)
      nil)))


(defn add-user [first-name last-name username 
                email-add password]
  (insert users (values {:first_name first-name :last_name last-name
                         :email_address email-add :username username
                         :password (digest/md5 password) :role_id 1})))




(defn get-workouts []
  (select workouts
          (with workout-entries)))

(defn to-timestamp [long-time]
  (tyme-format/parse postgres-timestamp 
                     (.toString (tyme-coerce/from-long long-time))))


(defn to-Date-Time [the-long-time]
  (tyme-coerce/from-long the-long-time))
  
(defn to-long-time [the-Date-Time]
  (tyme-coerce/to-long the-Date-Time))


(defn add-workout [workout-name workout-text start-date end-date]
  (insert workouts (values {:workout_title workout-name 
                         :workout_text workout-text
                         :start_date start-date :end_date end-date 
                         :is_active false})))

(defn get-workout-entries [workout-id]
  (let [the-workout (select workouts
                            (with workout-entries)
                            (where {:worker_id workout-id}))]
    (if the-workout
      the-workout)))

(defn get-languages []
  (select languages))




