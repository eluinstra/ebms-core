CREATE TABLE cpa
(
	cpa_id						VARCHAR(256)		NOT NULL UNIQUE,
	cpa								TEXT						NOT NULL,
	url								VARCHAR(256)		NULL
);

CREATE TABLE ebms_message
(
	id								INT							IDENTITY(1,1)	PRIMARY KEY,
	time_stamp				DATETIME				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				SMALLINT				NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			DATETIME				NULL,
	from_role					VARCHAR(256)		NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						TEXT						NULL,
	status						SMALLINT				NULL,
	status_time				DATETIME				NULL
);

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id,message_nr);

CREATE INDEX i_ebms_message ON ebms_message_queue (cpa_id,status,message_nr);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INT							NOT NULL FOREIGN KEY REFERENCES ebms_message(id),
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						IMAGE						NOT NULL
);

ALTER TABLE ebms_attachment ADD CONSTRAINT uc_ebms_attachment UNIQUE (ebms_message_id,order_nr);

CREATE TABLE ebms_event
(
	cpa_id						VARCHAR(256)		NOT NULL,
	channel_id				VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	time_to_live			DATETIME				NULL,
	time_stamp				DATETIME				NOT NULL,
	retries						SMALLINT				DEFAULT 0 NOT NULL
);

CREATE TABLE ebms_event_log
(
	message_id				VARCHAR(256)		NOT NULL,
	time_stamp				DATETIME				NOT NULL,
	uri								VARCHAR(256)		NULL,
	status						SMALLINT				NOT NULL,
	error_message			TEXT						NULL
);
