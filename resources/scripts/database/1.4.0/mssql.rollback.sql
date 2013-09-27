ALTER TABLE ebms_event DROP COLUMN type;

sp_rename ebms_event, ebms_send_event;
