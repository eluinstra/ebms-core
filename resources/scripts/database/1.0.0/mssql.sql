ALTER TABLE ebms_message ADD time_to_live DATETIME NULL;

ALTER TABLE ebms_message DROP COLUMN original;
