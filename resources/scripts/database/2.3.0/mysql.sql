CREATE TABLE url
(
	old_url 					VARCHAR(256)		NOT NULL,
	new_url						VARCHAR(256)		NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE url ADD CONSTRAINT uc_old_url UNIQUE (old_url(255));

ALTER TABLE ebms_attachment ADD COLUMN order_nr SMALLINT;

UPDATE ebms_attachment SET order_nr = 0;

ALTER TABLE ebms_attachment ADD COLUMN order_nr SMALLINT NOT NULL;

UPDATE ebms_event SET type = -1 WHERE type = 0;

UPDATE ebms_event SET type = 0 WHERE type > 1;

UPDATE ebms_event SET type = 1 WHERE type = -1;
