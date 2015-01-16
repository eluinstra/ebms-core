CREATE TABLE url
(
	old_url 					VARCHAR(256)		NOT NULL UNIQUE,
	new_url						VARCHAR(256)		NOT NULL
);

UPDATE ebms_event SET type = 1 where type > 1;
