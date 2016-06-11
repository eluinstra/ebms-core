UPDATE TABLE cpa DROP COLUMN url;

UPDATE TABLE ebms_message ADD COLUMN sequence_nr INT NULL;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

UPDATE TABLE ebms_event ADD COLUMN is_ordered BIT DEFAULT 0 NOT NULL;
