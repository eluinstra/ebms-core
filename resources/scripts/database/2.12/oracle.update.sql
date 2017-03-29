UPDATE TABLE cpa DROP COLUMN url;

CREATE TABLE url
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				NUMBER(5)				NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	processed					NUMBER(5)				DEFAULT 0 NOT NULL
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
