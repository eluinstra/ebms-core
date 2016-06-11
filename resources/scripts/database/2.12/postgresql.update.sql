UPDATE TABLE cpa DROP COLUMN url;

UPDATE TABLE ebms_message ADD COLUMN sequence_nr INTEGER NULL;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

UPDATE TABLE ebms_event ADD COLUMN is_ordered BOOLEAN DEFAULT 0 NOT NULL;
