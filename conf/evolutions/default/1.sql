# Registration Schema
 
# --- !Ups

CREATE TABLE account (
    id SERIAL,
    userid     TEXT NOT NULL,
    authcode   TEXT NOT NULL,
    devicekey  TEXT NOT NULL,
    authorized TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE card (
    id SERIAL,
    ownerPhone TEXT NOT NULL,
    ownerName  TEXT NOT NULL,
    ownerEmail TEXT NOT NULL,
    imageFront TEXT NOT NULL,
    imageRear  TEXT NOT NULL,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE account;
DROP TABLE card;