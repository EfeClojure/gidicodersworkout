CREATE TABLE workout_entry (
 workout_entry_id 	integer NOT NULL 
				PRIMARY KEY
, workout_id 		int REFERENCES workout (workout_id) ON UPDATE CASCADE
, user_id 		int REFERENCES usermaster (user_id) ON UPDATE CASCADE
, language_id 		int REFERENCES language (language_id) ON UPDATE CASCADE
, source_text 		text
, date_sent_in		varchar(30) NOT NULL
);
