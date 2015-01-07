CREATE TABLE url
(
	old_url 					VARCHAR(256)		NOT NULL UNIQUE,
	new_url						VARCHAR(256)		NOT NULL
);

CREATE INDEX i_ebms_message ON ebms_message_queue (cpa_id,status,message_nr);

UPDATE ebms_event SET type = 1 where type > 1;
