(ns gidicodersworkout.db.dbaccess
  (:refer-clojure :exclude [update])
  (:require [immutant.web :as immutant]
            [digest :as digest]
            
            [clj-time.core :as tyme]
            [clj-time.format :as tyme-format]
            [clj-time.coerce :as tyme-coerce])
  (:use [korma.db :only (defdb postgres)]
        [korma.core]))



#_(defdb the-db (postgres {:db "gidicodersworkoutdb"
                         :user "postgres"
                         :password "asdffdsa"}))

(defdb the-db 
  (if (System/getenv "DATABASE_URL")
    (let [db-uri (java.net.URI. (System/getenv "DATABASE_URL"))
          user-and-password (clojure.string/split (.getUserInfo db-uri) #":")]
      {:classname "org.postgresql.Driver"
       :subprotocol "postgresql"
       :user (get user-and-password 0)
       :password (get user-and-password 1) ; may be nil
       :subname (if (= -1 (.getPort db-uri))
                  (format "//%s%s" (.getHost db-uri) (.getPath db-uri))
                  (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri) (.getPath db-uri)))})
    (postgres
     {:db "gidicodersworkoutdb"
      :user "postgres"
      :password "asdffdsa"})))
;;https://gist.github.com/flyingmachine/4004807


(declare workouts users languages roles 
         workout-entries)


(defonce postgres-timestamp (tyme-format/formatter "yyyy-MM-dd hh:mm:ss"))
;; postgres timestamp format
;; 2002-12-31 16:00:00

;; valid time stamp: '2004-10-19 10:23:54'
(defn to-timestamp [lg-time]
  (tyme-format/unparse postgres-timestamp
                       (tyme-coerce/from-long lg-time)))

(defn timestamp-to-lgtime [the-timestamp]
  (tyme-coerce/to-long the-timestamp))

(defn to-Date-Time [the-lg-time]
  (tyme-coerce/from-long the-lg-time))
  
(defn to-long-time [the-Date-Time]
  "Takes a DateTime object to get the long milliseconds since the epoch"
  (tyme-coerce/to-long the-Date-Time))


;;; Entity definitions

(defentity workouts 
  (pk :workout_id)
  (table :workout)
  
  (entity-fields :workout_id :creator_id :workout_title :workout_text 
                 :start_date :end_date :is_active)
  (belongs-to users {:fk :creator_id})
  (has-many workout-entries)

  (prepare (fn [{timestamp :start_date :as v}]
             (if timestamp
               (assoc v :start_date (timestamp-to-lgtime timestamp))
               v)))
  (transform (fn [{lg-timestamp :start_date :as v}]
               (if lg-timestamp 
                 (assoc v :start_date (to-timestamp (Long/parseLong lg-timestamp)))
                 v)))

  (prepare (fn [{timestamp :end_date :as v}]
             (if timestamp
               (assoc v :end_date (timestamp-to-lgtime timestamp))
               v)))
  (transform (fn [{lg-timestamp :end_date :as v}]
               (if lg-timestamp 
                 (assoc v :end_date (to-timestamp (Long/parseLong lg-timestamp)))
                 v))))

(defentity users
  (pk :user_id) 
  (table :usermaster)
  
  (entity-fields :user_id :first_name :last_name 
                 :email_address :username :password)

  (has-one roles {:fk :role_id})
  (has-many workouts)
  (has-many workout-entries))

(defentity roles 
  (pk :role_id)
  (table :role)

  (entity-fields :role_id :role_title)
  (belongs-to users))

(defentity languages
  (pk :language_id)
  (table :language)
 
  (entity-fields :language_id :language_name))

(defentity workout-entries 
  (pk :workout_entry_id)
  (table :workout_entry)
  
  (entity-fields :workout_entry_id :user_id :source_text 
                 :date_sent_in :language_id :workout_id)
  
  (belongs-to workouts {:fk :workout_id})
  (has-one users {:fk :user_id})
  (has-one languages {:fk :language_id})

  (prepare (fn [{timestamp :date_sent_in :as v}]
             (if timestamp
               (assoc v :date_sent_in (timestamp-to-lgtime timestamp))
               v)))
  (transform (fn [{lg-timestamp :date_sent_in :as v}]
               (if lg-timestamp 
                 (assoc v :date_sent_in (to-timestamp (Long/parseLong lg-timestamp)))
                 v)))
  #_(transform (fn [{timestamp :date_sent_in :as v}]
               (if timestamp 
                 (assoc v :date_sent_in (timestamp-to-lgtime timestamp))
                 v))))

;; End of entity definitions


;; -- Database access utility functions

(defn get-users []
  (let [the-coders (select users 
                           (with roles))]
    (if (not (empty? the-coders))
      the-coders
      nil)))

(defn get-user-by-id [the-user-id]
  (let [results (select users (with roles)
                        (where {:user_id (Integer/parseInt the-user-id)}))]
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


(defn get-workouts
  ([]
   (select workouts
           (with workout-entries))) 
  ([the-offset the-limit]
   (select workouts
           (with workout-entries)
           (limit the-limit)
           (offset the-offset))))


(defn add-workout [creator-id workout-name workout-text 
                   start-date end-date]
  (insert workouts (values {:creator_id creator-id
                            :workout_title workout-name 
                            :workout_text workout-text
                            :start_date (timestamp-to-lgtime start-date) 
                            :end_date (timestamp-to-lgtime end-date)
                            :is_active false})))

#_(insert workouts (values {:workout_title "League of Anagram detectives" 
                          :workout_text "Tell when two words given are anagrams"
                          :start_date (tyme-coerce/to-long (tyme/now)) 
                             
                          :end_date (tyme-coerce/to-long (tyme/now))
                          :is_active false}))

(defn get-workout-by-id [workout-id]
 (let [results (select workouts
                       (with users)
                       (where {:workout_id 
                               (Integer/parseInt workout-id)}))]
   (if (not (empty? results))
     (first results)
     nil)))


(defn get-workout-by-username [username]
  (select workouts 
          (with users (where {:username username}))))

(defn get-workout-entries [workout-id]
  (let [workoutIdAsInt (Integer/parseInt workout-id)
        the-entries (select workout-entries
                            (with users)
                            (where {:workout_id workoutIdAsInt}))]
    (if (not (empty? the-entries))
      the-entries
      nil)))

(defn get-user-entries [username]
  (let [results (select workout-entries
                        (with workouts)
                        (with users (where {:username username}))
                        (order :date_sent_in :ASC))]
    (if (not (empty? results))
      (map #(assoc % :date_sent_in 
                   (% :date_sent_in)) 
           results)
      nil)))

(defn get-workout-entry [workout-entry-id]
  (let [results (select workout-entries
                        (with users)
                        (where {:workout_entry_id 
                                (Integer/parseInt workout-entry-id)}))]
    (if (not (empty? results))
      (first results)
      nil)))

(defn user-submitted? [workout-id user-id]
  (let [workoutIdAsInt (Integer/parseInt workout-id)
        userIdAsInt (Integer/parseInt user-id)
        the-entries (select workout-entries
                            (with users (where {:user_id userIdAsInt}))
                            (with workouts 
                                  (where {:workout_id workoutIdAsInt})))]
    (if (empty? the-entries)
      false true)))

(defn add-workout-entry [workoutId submitter-id
                         language-type-id source-text]
  (insert workout-entries (values {:workout_id (Integer/parseInt workoutId)
                                   :user_id (Integer/parseInt submitter-id)
                                   :source_text source-text
                                   :language_id language-type-id                                   
                                   :date_sent_in (-> (tyme/now)
                                                     to-long-time
                                                     to-timestamp)})))

(defn get-languages []
  (select languages))

(defn get-roles []
  (select roles))


