ALTER TABLE ebms_message DROP COLUMN time_to_live;

ALTER TABLE ebms_message ADD original BLOB NULL;
