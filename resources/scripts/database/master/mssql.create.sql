CREATE TABLE cpa
(
	cpa_id						VARCHAR(256)		NOT NULL UNIQUE,
	cpa								TEXT						NOT NULL
);

CREATE TABLE url_mapping
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						IMAGE					NOT NULL,
	destination				IMAGE					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
);

CREATE TABLE ebms_message
(
	id								INT							IDENTITY(1,1)	PRIMARY KEY,
	time_stamp				DATETIME				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			DATETIME				NULL,
	persist_time			DATETIME				NULL,
	from_party_id			VARCHAR(256)		NOT NULL,
	from_role					VARCHAR(256)		NULL,
	to_party_id				VARCHAR(256)		NOT NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						TEXT						NULL,
	status						SMALLINT				NULL,
	status_time				DATETIME				NULL
);

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id,message_nr);

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id,message_nr);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INT							NOT NULL FOREIGN KEY REFERENCES ebms_message(id),
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						IMAGE						NOT NULL
);

CREATE TABLE ebms_event
(
	cpa_id							VARCHAR(256)		NOT NULL,
	send_channel_id			VARCHAR(256)		NULL,
	receive_channel_id	VARCHAR(256)		NOT NULL,
	message_id					VARCHAR(256)		NOT NULL UNIQUE,
	time_to_live				DATETIME				NULL,
	time_stamp					DATETIME				NOT NULL,
	is_confidential			BIT							NOT NULL,
	retries							SMALLINT				DEFAULT 0 NOT NULL,
	server_id						VARCHAR(256)		NULL
);

CREATE INDEX i_ebms_event ON ebms_event (time_stamp);

CREATE TABLE ebms_event_log
(
	message_id				VARCHAR(256)		NOT NULL,
	time_stamp				DATETIME				NOT NULL,
	uri								VARCHAR(256)		NULL,
	status						SMALLINT				NOT NULL,
	error_message			TEXT						NULL
);

CREATE INDEX i_ebms_event_log ON ebms_event_log (message_id);

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				SMALLINT				NOT NULL,
	time_stamp				DATETIME				NOT NULL,
	processed					SMALLINT				DEFAULT 0 NOT NULL
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
