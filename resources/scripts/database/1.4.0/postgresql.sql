ALTER TABLE ebms_send_event RENAME TO ebms_event;

ALTER TABLE ebms_event ADD type INTEGER NULL;

UPDATE ebms_event SET type = 0;

ALTER TABLE ebms_event ADD type DROP NOT NULL;
