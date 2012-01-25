CREATE TABLE cpa
(
	cpa_id						VARCHAR(128)		NOT NULL UNIQUE,
	cpa								TEXT						NOT NULL
);

CREATE TABLE ebms_message
(
	id								SERIAL					PRIMARY KEY,
--	parent_id					INTEGER					NULL REFERENCES ebms_message(id),
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				INTEGER					NULL,
	message_id				VARCHAR(256)		NOT NULL,
	ref_to_message_id	VARCHAR(256)		NULL,
	from_role					VARCHAR(256)		NULL,
	to_role						VARCHAR(256)		NULL,
	service_type			VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	original					BYTEA						NULL,
	signature					TEXT						NULL,
	message_header		TEXT						NOT NULL,
	sync_reply				TEXT						NULL,
	message_order			TEXT						NULL,
	ack_requested			TEXT						NULL,
	content						TEXT						NULL,
	status						INTEGER					NULL,
	status_time				TIMESTAMP				NULL
);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INTEGER					NOT NULL REFERENCES ebms_message(id),
	name							VARCHAR(128)		NOT NULL,
	content_type			VARCHAR(64)			NOT NULL,
	content						BYTEA						NOT NULL
);

CREATE TABLE ebms_send_event
(
	ebms_message_id		INTEGER					NOT NULL REFERENCES ebms_message(id),
	time							TIMESTAMP				NOT NULL DEFAULT NOW(),
	status						INTEGER					NOT NULL DEFAULT 0,
	status_time				TIMESTAMP				NOT NULL DEFAULT NOW(),
--	http_status_code	INTEGER					NULL,
	UNIQUE (ebms_message_id,time)
);

COMMIT;

CREATE INDEX i_message_id ON ebms_message(message_id);