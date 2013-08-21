ALTER TABLE ebms_send_event DROP COLUMN error_message;

ALTER TABLE ebms_message DROP CONSTRAINT uc_ebms_message_id;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id(255));

ALTER TABLE ebms_message DROP COLUMN message_nr;
