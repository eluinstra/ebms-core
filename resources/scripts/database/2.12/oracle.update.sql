UPDATE TABLE cpa DROP COLUMN url;

UPDATE TABLE ebms_message ADD COLUMN sequence_nr NUMBER(8) NULL;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

UPDATE TABLE ebms_event ADD COLUMN is_ordered NUMBER(1) DEFAULT 0 NOT NULL;

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				NUMBER(5)				NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
