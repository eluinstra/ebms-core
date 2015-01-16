CREATE TABLE url
(
	old_url 					VARCHAR(256)		NOT NULL,
	new_url						VARCHAR(256)		NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE url ADD CONSTRAINT uc_old_url UNIQUE (old_url(255));

UPDATE ebms_event SET type = 1 where type > 1;
