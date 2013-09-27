RENAME TABLE ebms_send_event TO ebms_event;

ALTER TABLE ebms_event ADD type NUMBER NULL;

UPDATE ebms_event SET type = 0;

ALTER TABLE ebms_event MODIFY type NUMBER NOT NULL;
