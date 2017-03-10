UPDATE TABLE cpa DROP COLUMN url;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				SMALLINT				NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
