CREATE TABLE cpa
(
	cpa_id						VARCHAR(256)		NOT NULL UNIQUE,
	cpa								CLOB						NOT NULL
);

CREATE TABLE ebms_message
(
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				NUMBER(5)				NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				NUMBER(5)				DEFAULT 0 NOT NULL,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	from_role					VARCHAR(256)		NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						CLOB						NULL,
	status						NUMBER(5)				NULL,
	status_time				TIMESTAMP				NULL,
	PRIMARY KEY (message_id,message_nr)
);

CREATE INDEX i_ebms_message ON ebms_message (cpa_id,status,message_nr);

CREATE TABLE ebms_attachment
(
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				NUMBER(5)				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BLOB						NOT NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message(message_id,message_nr)
);

CREATE TABLE ebms_event
(
	message_id				VARCHAR(256)		NOT NULL,
	time							TIMESTAMP				DEFAULT SYSDATE NOT NULL,
	type							NUMBER(5)				NOT NULL,
	status						NUMBER(5)				DEFAULT 0 NOT NULL,
	status_time				TIMESTAMP				DEFAULT SYSDATE NOT NULL,
	uri								VARCHAR(256)		NULL,
	error_message			CLOB						NULL,
	CONSTRAINT uc_ebms_event UNIQUE (message_id,time)
);

CREATE INDEX i_ebms_event ON ebms_event (status);
