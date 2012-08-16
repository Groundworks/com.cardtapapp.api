# Registration Schema
 
# --- !Ups

CREATE TABLE account (
    id SERIAL,
    email   TEXT  NOT NULL,
    buffer  bytea NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE device (
    id SERIAL,
    account    integer references account(id),
    devkey     TEXT  NOT NULL,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE account;
DROP TABLE device;
