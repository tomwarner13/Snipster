CREATE TABLE snips (
    id serial PRIMARY KEY,
    username VARCHAR( 150 ) NOT NULL,
    title VARCHAR ( 150 ) NOT NULL,
    content TEXT,
    created_on TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL );