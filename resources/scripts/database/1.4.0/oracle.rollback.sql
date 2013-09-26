ALTER TABLE ebms_event DROP COLUMN type;

ALTER TABLE ebms_event RENAME TO ebms_send_event;