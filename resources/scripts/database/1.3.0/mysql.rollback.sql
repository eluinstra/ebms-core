ALTER TABLE ebms_send_event MODIFY time TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00';

ALTER TABLE ebms_send_event MODIFY status_time TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE ebms_send_event DROP COLUMN error_message;

ALTER TABLE ebms_attachment MODIFY content BLOB NOT NULL;

ALTER TABLE ebms_message DROP CONSTRAINT uc_ebms_message_id;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id(255));

ALTER TABLE ebms_message DROP COLUMN message_nr;
