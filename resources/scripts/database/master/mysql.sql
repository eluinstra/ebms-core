CREATE TABLE cpa
(
	cpa_id						VARCHAR(128)		NOT NULL UNIQUE,
	cpa								TEXT						NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ebms_message
(
	id								INTEGER					AUTO_INCREMENT PRIMARY KEY,
--	parent_id					INTEGER					NULL REFERENCES ebms_message(id),
--	creation_time			TIMESTAMP				NOT NULL DEFAULT '0000-00-00 00:00:00',
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				INTEGER					NULL,
	message_id				VARCHAR(256)		NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL DEFAULT '0000-00-00 00:00:00',
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	from_role					VARCHAR(256)		NULL,
	to_role						VARCHAR(256)		NULL,
	service_type			VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	signature					TEXT						NULL,
	message_header		TEXT						NOT NULL,
	sync_reply				TEXT						NULL,
	message_order			TEXT						NULL,
	ack_requested			TEXT						NULL,
	content						TEXT						NULL,
	status						INTEGER					NULL,
	status_time				TIMESTAMP				NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id(255));

CREATE TABLE ebms_attachment
(
	ebms_message_id		INTEGER					NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BLOB						NOT NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ebms_send_event
(
	ebms_message_id		INTEGER					NOT NULL REFERENCES ebms_message(id),
	time							TIMESTAMP				NOT NULL DEFAULT '0000-00-00 00:00:00',
	status						INTEGER					NOT NULL DEFAULT 0,
	status_time				TIMESTAMP				NOT NULL DEFAULT NOW(),
--	http_status_code	INTEGER					NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id),
	UNIQUE (ebms_message_id,time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
