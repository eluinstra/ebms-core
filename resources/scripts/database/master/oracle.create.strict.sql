CREATE TABLE cpa
(
	cpa_id						VARCHAR(256)		NOT NULL PRIMARY KEY,
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
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
);

CREATE TABLE ebms_message
(
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				NUMBER(5)				DEFAULT 0 NOT NULL,
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
	status						NUMBER(5)				NULL,
	status_time				TIMESTAMP				NULL,
	PRIMARY KEY (message_id,message_nr),
	FOREIGN KEY (cpa_id) REFERENCES cpa(cpa_id)
);

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id,message_nr);

CREATE TABLE ebms_attachment
(
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				NUMBER(5)				NOT NULL,
	order_nr					NUMBER(5)				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BLOB						NOT NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message (message_id,message_nr)
);

ALTER TABLE ebms_attachment ADD CONSTRAINT uc_ebms_attachment UNIQUE (message_id,message_nr,order_nr);

CREATE TABLE ebms_event
(
	cpa_id							VARCHAR(256)		NOT NULL,
	send_channel_id			VARCHAR(256)		NULL,
	receive_channel_id	VARCHAR(256)		NOT NULL,
	message_id					VARCHAR(256)		NOT NULL UNIQUE,
	message_nr					NUMBER(5)				DEFAULT 0 NOT NULL,
	time_to_live				TIMESTAMP				NULL,
	time_stamp					TIMESTAMP				NOT NULL,
	is_confidential			NUMBER(1)				NOT NULL,
	retries							NUMBER(5)				DEFAULT 0 NOT NULL,
	server_id						VARCHAR(256)		NULL,
	FOREIGN KEY (cpa_id) REFERENCES cpa(cpa_id),
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message (message_id,message_nr)
);

CREATE INDEX i_ebms_event ON ebms_event (time_stamp);

CREATE TABLE ebms_event_log
(
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				NUMBER(5)				DEFAULT 0 NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	uri								VARCHAR(256)		NULL,
	status						NUMBER(5)				NOT NULL,
	error_message			CLOB						NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message (message_id,message_nr)
);

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	message_nr				NUMBER(5)				DEFAULT 0 NOT NULL,
	event_type				NUMBER(5)				NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	processed					NUMBER(5)				DEFAULT 0 NOT NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message (message_id,message_nr)
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
