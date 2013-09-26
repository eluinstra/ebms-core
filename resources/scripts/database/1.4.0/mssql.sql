ALTER TABLE ebms_send_event RENAME TO ebms_event;

ALTER TABLE ebms_event ADD type INT NOT NULL;
