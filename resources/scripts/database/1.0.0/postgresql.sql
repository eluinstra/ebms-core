ALTER TABLE ebms_message ADD time_to_live TIMESTAMP NULL;

ALTER TABLE ebms_message DROP COLUMN original;

ALTER TABLE ebms_attachment ALTER name DROP NOT NULL;
