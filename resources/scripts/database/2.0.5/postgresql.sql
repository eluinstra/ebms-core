DROP INDEX i_message_id;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id);

UPDATE ebms_message
SET status=5
WHERE status=1
OR status=2;