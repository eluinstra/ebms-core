CREATE TEMPORARY TABLE ebms_tmp AS
	SELECT min(id) AS id
	FROM ebms_message
	GROUP BY message_id;

DELETE FROM ebms_send_event
WHERE ebms_message_id NOT IN (
	SELECT id
	FROM ebms_tmp
);

DELETE FROM ebms_attachment
WHERE ebms_message_id NOT IN (
	SELECT id
	FROM ebms_tmp
);

DELETE FROM ebms_message
WHERE id NOT IN (
	SELECT id
	FROM ebms_tmp
);

DROP TABLE ebms_tmp;

-- DROP INDEX i_message_id ON ebms_message;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id(255));

UPDATE ebms_message
SET status=5
WHERE status=1
OR status=2;
