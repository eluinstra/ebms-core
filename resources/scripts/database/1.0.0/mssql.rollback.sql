ALTER TABLE ebms_message DROP COLUMN time_to_live;

ALTER TABLE ebms_message ADD original BLOB NULL;

ALTER TABLE ebms_attachment ALTER COLUMN name VARCHAR(256) NOT NULL;
