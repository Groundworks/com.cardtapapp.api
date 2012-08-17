# Registration Schema
 
# --- !Ups

CREATE TABLE account (
    id     SERIAL,
    email  TEXT  NOT NULL,
    buffer BYTEA NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE device (
    id      SERIAL,
    secret  TEXT NOT NULL,
    device  TEXT NOT NULL,
    buffer  BYTEA NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE share (
    id     SERIAL,
    buffer BYTEA NOT NULL,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE account;
DROP TABLE device;
DROP TABLE share;
