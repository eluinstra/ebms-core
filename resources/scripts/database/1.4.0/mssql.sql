sp_rename ebms_send_event, ebms_event;

ALTER TABLE ebms_event ADD type INT NULL;

UPDATE ebms_event SET type = 0;

ALTER TABLE ebms_event ALTER COLUMN type INT NOT NULL;
