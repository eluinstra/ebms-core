CREATE TABLE cpa
(
	cpa_id						VARCHAR(256)		NOT NULL UNIQUE,
	cpa								CLOB						NOT NULL
);

CREATE TABLE url_mapping
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	UNIQUE (id,cpa_id)
);

CREATE TABLE ebms_message
(
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	persist_time			TIMESTAMP				NULL,
	from_party_id			VARCHAR(256)		NOT NULL,
	from_role					VARCHAR(256)		NULL,
	to_party_id				VARCHAR(256)		NOT NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						CLOB						NULL,
	status						SMALLINT				NULL,
	status_time				TIMESTAMP				NULL,
	PRIMARY KEY (message_id)
);

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id);

CREATE TABLE ebms_attachment
(
	message_id				VARCHAR(256)		NOT NULL,
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BLOB						NOT NULL,
	FOREIGN KEY (message_id) REFERENCES ebms_message (message_id)
);

CREATE TABLE delivery_task
(
	cpa_id							VARCHAR(256)		NOT NULL,
	send_channel_id			VARCHAR(256)		NULL,
	receive_channel_id	VARCHAR(256)		NOT NULL,
	message_id					VARCHAR(256)		NOT NULL UNIQUE,
	time_to_live				TIMESTAMP				NULL,
	time_stamp					TIMESTAMP				NOT NULL,
	is_confidential			BOOLEAN					NOT NULL,
	retries							SMALLINT				DEFAULT 0 NOT NULL,
	server_id						VARCHAR(256)		NULL
);

CREATE INDEX i_delivery_task ON delivery_task (time_stamp);

CREATE TABLE delivery_log
(
	message_id				VARCHAR(256)		NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	uri								VARCHAR(256)		NULL,
	status						SMALLINT				NOT NULL,
	error_message			CLOB						NULL
);

CREATE INDEX i_delivery_log ON delivery_log (message_id);

CREATE TABLE message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				SMALLINT				NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	processed					SMALLINT				DEFAULT 0 NOT NULL
);

CREATE INDEX i_message_event ON message_event (time_stamp);
