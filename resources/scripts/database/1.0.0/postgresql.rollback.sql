ALTER TABLE ebms_message DROP COLUMN time_to_live;

ALTER TABLE ebms_message ADD original BYTEA NULL;

ALTER TABLE ebms_attachment ALTER name SET NOT NULL;
