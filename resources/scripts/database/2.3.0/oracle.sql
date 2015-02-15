CREATE TABLE url
(
	old_url 					VARCHAR(256)		NOT NULL UNIQUE,
	new_url						VARCHAR(256)		NOT NULL
);

ALTER TABLE ebms_attachment ADD COLUMN order_nr NUMBER(5);

UPDATE ebms_attachment SET order_nr = (SELECT ROW_NUMBER() OVER(PARTITION BY message_id, message_nr ORDER BY message_id) AS order_nr FROM ebms_attachment);

ALTER TABLE ebms_attachment MODIFY COLUMN order_nr NUMBER(5) NOT NULL;

UPDATE ebms_event SET type = -1 WHERE type = 0;

UPDATE ebms_event SET type = 0 WHERE type > 1;

UPDATE ebms_event SET type = 1 WHERE type = -1;
