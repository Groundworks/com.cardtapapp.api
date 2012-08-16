# Registration Schema
 
# --- !Ups

CREATE TABLE accounts (
    id SERIAL,
    userid     TEXT NOT NULL,
    authcode   TEXT NOT NULL,
    devicekey  TEXT NOT NULL,
    authorized BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE accounts;
