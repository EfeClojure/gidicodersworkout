CREATE TABLE workout (
-- If you type 'serial' there's no need to precede it with 'int'
  workout_id 	serial PRIMARY KEY
, workout_title text NOT NULL
, workout_text	text NOT NULL
, start_date 	timestamp NOT NULL
, end_date 	timestamp

, is_active	boolean
);
