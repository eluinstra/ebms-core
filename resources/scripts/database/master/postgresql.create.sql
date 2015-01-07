CREATE TABLE cpa
(
	cpa_id						VARCHAR(256)		NOT NULL UNIQUE,
	cpa								TEXT						NOT NULL
);

CREATE TABLE url
(
	old_url 					VARCHAR(256)		NOT NULL UNIQUE,
	new_url						VARCHAR(256)		NOT NULL
);

CREATE TABLE ebms_message
(
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				SMALLINT				NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	from_role					VARCHAR(256)		NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						TEXT						NULL,
	status						SMALLINT				NULL,
	status_time				TIMESTAMP				NULL,
	PRIMARY KEY (message_id,message_nr)
);

CREATE TABLE ebms_attachment
(
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BYTEA						NOT NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message(message_id,message_nr)
);

CREATE TABLE ebms_event
(
	message_id				VARCHAR(256)		NOT NULL,
	time							TIMESTAMP				NOT NULL DEFAULT NOW(),
	type							SMALLINT				NOT NULL,
	status						SMALLINT				NOT NULL DEFAULT 0,
	status_time				TIMESTAMP				NOT NULL DEFAULT NOW(),
	uri								VARCHAR(256)		NULL,
	error_message			TEXT						NULL,
	CONSTRAINT uc_ebms_event UNIQUE (message_id,time)
);
