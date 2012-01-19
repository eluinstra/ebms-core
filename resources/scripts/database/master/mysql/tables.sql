CREATE TABLE cpa
(
	cpa_id						VARCHAR(128)		NOT NULL UNIQUE,
	cpa								TEXT						NOT NULL
);

CREATE TABLE ebms_message
(
	id								INT							AUTO_INCREMENT PRIMARY KEY,
--	parent_id					INT							NULL FOREIGN,
	time_stamp				TIMESTAMP				NOT NULL DEFAULT '0000-00-00 00:00:00',
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
	original					BLOB						NULL,
	signature					TEXT						NULL,
	message_header		TEXT						NOT NULL,
	sync_reply				TEXT						NULL,
	message_order			TEXT						NULL,
	ack_requested			TEXT						NULL,
	content						TEXT						NULL,
	status						INT							NULL,
	status_time				TIMESTAMP				NULL
--	FOREIGN KEY (parent_id) REFERENCES ebms_message(id)
);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INT							NOT NULL,
	name							VARCHAR(128)		NOT NULL,
	content_type			VARCHAR(64)			NOT NULL,
	content						BLOB						NOT NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id) ON DELETE CASCADE
);

CREATE TABLE ebms_send_event
(
--	id								INT							AUTO_INCREMENT PRIMARY KEY,
	ebms_message_id		INT							NOT NULL,
	time							TIMESTAMP				NOT NULL DEFAULT '0000-00-00 00:00:00',
	status						INT							NOT NULL DEFAULT 0,
	status_time				TIMESTAMP				NOT NULL DEFAULT NOW(),
--	http_status_code	INT							NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id) ON DELETE CASCADE
);