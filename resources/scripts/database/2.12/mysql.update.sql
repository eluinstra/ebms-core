UPDATE TABLE cpa DROP COLUMN url;

UPDATE TABLE ebms_message ADD COLUMN sequence_nr INTEGER NULL;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL,
	destination				VARCHAR(256)		NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE url ADD CONSTRAINT uc_url_source UNIQUE (source(255));

UPDATE TABLE ebms_event ADD COLUMN is_ordered TINYINT(1) DEFAULT 0 NOT NULL;

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				SMALLINT				NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
