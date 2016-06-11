UPDATE TABLE cpa DROP COLUMN url;

UPDATE TABLE ebms_message ADD COLUMN sequence_nr NUMBER(8) NULL;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

UPDATE TABLE ebms_event ADD COLUMN is_ordered NUMBER(1) DEFAULT 0 NOT NULL;
