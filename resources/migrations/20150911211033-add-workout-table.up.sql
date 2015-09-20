CREATE TABLE workout (
-- If you type 'serial' there's no need to precede it with 'int'
  workout_id 	serial PRIMARY KEY
, creator_id    integer NOT NULL
, workout_title text NOT NULL
, workout_text	text NOT NULL
, start_date 	varchar(30) NOT NULL
, end_date 	varchar(30)

, is_active	boolean
);
