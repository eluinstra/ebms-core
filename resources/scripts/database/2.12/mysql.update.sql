UPDATE TABLE cpa DROP COLUMN url;

UPDATE TABLE ebms_message ADD COLUMN sequence_nr INTEGER NULL;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL,
	destination				VARCHAR(256)		NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE url ADD CONSTRAINT uc_url_source UNIQUE (source(255));

UPDATE TABLE ebms_event ADD COLUMN is_ordered TINYINT(1) DEFAULT 0 NOT NULL;

