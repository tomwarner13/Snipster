CREATE TABLE snips (
    id serial PRIMARY KEY,
    username VARCHAR( 150 ) NOT NULL,
    title VARCHAR ( 150 ) NOT NULL,
    content TEXT,
    created_on TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL );

CREATE TABLE user_settings (
    username VARCHAR( 150 ) UNIQUE,
    use_line_numbers BOOLEAN NOT NULL DEFAULT FALSE,
    insert_closing BOOLEAN NOT NULL DEFAULT FALSE);