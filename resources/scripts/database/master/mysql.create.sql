CREATE TABLE cpa
(
	cpa_id						VARCHAR(256)		NOT NULL,
	cpa								MEDIUMTEXT			NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE cpa ADD CONSTRAINT uc_cpa_id UNIQUE (cpa_id(255));

CREATE TABLE url_mapping
(
	source						VARCHAR(256)		NOT NULL,
	destination				VARCHAR(256)		NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE url_mapping ADD CONSTRAINT uc_url_source UNIQUE (source(255));

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ebms_message
(
	id								INTEGER					AUTO_INCREMENT PRIMARY KEY,
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	persist_time			TIMESTAMP				NULL,
	from_party_id			VARCHAR(256)		NOT NULL,
	from_role					VARCHAR(256)		NULL,
	to_party_id				VARCHAR(256)		NOT NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						TEXT						NULL,
	status						SMALLINT				NULL,
	status_time				TIMESTAMP				NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id(255),message_nr);

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id(255),message_nr);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INTEGER					NOT NULL,
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						LONGBLOB 				NOT NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ebms_event
(
	cpa_id							VARCHAR(256)		NOT NULL,
	send_channel_id			VARCHAR(256)		NULL,
	receive_channel_id	VARCHAR(256)		NOT NULL,
	message_id					VARCHAR(256)		NOT NULL,
	time_to_live				TIMESTAMP				NULL,
	time_stamp					TIMESTAMP				NOT NULL,
	is_confidential			TINYINT(1)			NOT NULL,
	retries							SMALLINT				DEFAULT 0 NOT NULL,
	server_id						VARCHAR(256)		NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ebms_event ADD CONSTRAINT uc_ebms_event UNIQUE (message_id(255));
CREATE INDEX i_ebms_event ON ebms_event (time_stamp);

CREATE TABLE ebms_event_log
(
	message_id				VARCHAR(256)		NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	uri								VARCHAR(256)		NULL,
	status						SMALLINT				NOT NULL,
	error_message			TEXT						NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX i_ebms_event_log ON ebms_event (message_id(255));

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				SMALLINT				NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	processed					SMALLINT				DEFAULT 0 NOT NULL
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
