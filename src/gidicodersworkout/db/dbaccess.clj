(ns dbaccess
  (:require [immutant.web :as immutant]
            [korma.core :as korma-core])
  (:use [korma.db :only (defdb postgres)]))



(defdb the-db (postgres {:db "gidicodersworkoutdb"
                         :user "postgres"
                         :password "asdffdsa"}))
#_(defdb the-db (postgres {:db (or (System/getenv "DATABASE_URL")
                                 "postgresql://localhost:5432/gidicodersworkout")
                         :user "postgres"
                         :password "asdffdsa"}))

(declare workouts users languages roles 
         workout-entries workout-persons user-roles)

(korma-core/defentity workouts 
  (korma-core/pk :workout_id)

  (korma-core/table :workout)
  (korma-core/entity-fields :workout_title :workout_text 
                            :start_date :end_date :is_active)
  (korma-core/has-many users))

(korma-core/defentity users
  (korma-core/pk :user_id) 
  
  (korma-core/table :usermaster)   
  (korma-core/entity-fields :first_name :last_name :email_address 
                            :username :password)

  (korma-core/has-many roles))

(korma-core/defentity roles 
  (korma-core/pk :role_id)
  (korma-core/table :role)
  (korma-core/entity-fields :role_title)

  (korma-core/belongs-to users))

(korma-core/defentity languages
  (korma-core/pk :language_id)
  (korma-core/table :language)
  (korma-core/entity-fields :language_name))


(korma-core/defentity workout-entries 
  (korma-core/pk :workout_entry_id)

  (korma-core/table :workout_entry)
  (korma-core/entity-fields :source_text)
  
  (korma-core/belongs-to workouts)
  (korma-core/has-one users)
  (korma-core/has-one languages))


;; -- 

(korma-core/select roles
  #_(with address)
  (korma-core/fields :role_title))











