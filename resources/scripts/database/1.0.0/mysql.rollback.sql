ALTER TABLE ebms_message DROP COLUMN time_to_live;

ALTER TABLE ebms_message ADD original BLOB NULL;

ALTER TABLE ebms_attachment MODIFY name VARCHAR(256) NOT NULL;
