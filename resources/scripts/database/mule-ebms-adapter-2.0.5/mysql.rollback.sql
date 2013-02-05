UPDATE ebms_message
SET status=2
WHERE status=5;

ALTER TABLE ebms_message DROP INDEX uc_ebms_message_id;

CREATE INDEX i_message_id ON ebms_message(message_id);
