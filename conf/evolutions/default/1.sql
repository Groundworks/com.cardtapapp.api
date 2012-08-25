# Registration Schema
 
# --- !Ups

CREATE SEQUENCE bigtable_sequence;
CREATE TABLE bigtable (
    rowkey    VARCHAR(255) NOT NULL,
    column    VARCHAR(255) NOT NULL,
    version   integer DEFAULT nextval('bigtable_sequence') NOT NULL,
    buffer    BYTEA NOT NULL,
    PRIMARY KEY (rowkey,column,version)
);

CREATE TABLE 
 
# --- !Downs
 
DROP TABLE bigtable;
