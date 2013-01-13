CREATE TABLE cpa
(
	cpa_id						VARCHAR(128)		NOT NULL UNIQUE,
	cpa								TEXT						NOT NULL
);

CREATE TABLE ebms_message
(
	id								INT							IDENTITY(1,1)	PRIMARY KEY,
--	parent_id					INT							NULL FOREIGN KEY REFERENCES ebms_message(id),
	time_stamp				DATETIME				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				INT							NULL,
	message_id				VARCHAR(256)		NOT NULL,
	ref_to_message_id	VARCHAR(256)		NULL,
	from_role					VARCHAR(256)		NULL,
	to_role						VARCHAR(256)		NULL,
	service_type			VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	original					IMAGE						NULL,
	signature					TEXT						NULL,
	message_header		TEXT						NOT NULL,
	sync_reply				TEXT						NULL,
	message_order			TEXT						NULL,
	ack_requested			TEXT						NULL,
	content						TEXT						NULL,
	status						INT							NULL,
	status_time				DATETIME				NULL
);

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INT							NOT NULL FOREIGN KEY REFERENCES ebms_message(id),
	name							VARCHAR(256)		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						IMAGE						NOT NULL
);

CREATE TABLE ebms_send_event
(
	ebms_message_id		INT							NOT NULL FOREIGN KEY REFERENCES ebms_message(id),
	time							DATETIME				NOT NULL DEFAULT GETDATE(),
	status						INT							NOT NULL DEFAULT 0,
	status_time				DATETIME				NOT NULL DEFAULT GETDATE(),
--	http_status_code	INT							NULL,
	UNIQUE (ebms_message_id,time)
);
