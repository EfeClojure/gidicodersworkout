CREATE TABLE usermaster (
  user_id 	serial PRIMARY KEY
, first_name	text NOT NULL
, last_name 	text NOT NULL
, email_address text NOT NULL
, username 	text NOT NULL
, password	text NOT NULL
, role_id 	int REFERENCES role (role_id) ON UPDATE CASCADE
);