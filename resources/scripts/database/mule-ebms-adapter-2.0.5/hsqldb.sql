DELETE FROM ebms_send_event
WHERE ebms_message_id NOT IN (
	SELECT min(id)
	FROM ebms_message
	GROUP BY message_id
);

DELETE FROM ebms_attachment
WHERE ebms_message_id NOT IN (
	SELECT min(id)
	FROM ebms_message
	GROUP BY message_id
);

DELETE FROM ebms_message
WHERE id NOT IN (
	SELECT min(id)
	FROM ebms_message
	GROUP BY message_id
);

DROP INDEX i_message_id;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id);

UPDATE ebms_message
SET status=5
WHERE status=1
OR status=2;
