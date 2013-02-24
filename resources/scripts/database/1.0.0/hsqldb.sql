ALTER TABLE ebms_message ADD time_to_live TIMESTAMP NULL;

ALTER TABLE ebms_attachment DROP COLUMN original;
