CREATE TABLE cpa
(
	id							INT							IDENTITY(1,1)	PRIMARY KEY,
	cpa_id					VARCHAR(128)		NOT NULL UNIQUE,
	cpa							TEXT						NOT NULL
);

CREATE TABLE ebms_message
(
	id								INT							IDENTITY(1,1)	PRIMARY KEY,
--	parent_id					INT							NULL FOREIGN KEY REFERENCES ebms_message(id),
	time_stamp				DATETIME				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				INT							NOT NULL,
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	message_type			INT							NOT NULL,
	message_original	IMAGE						NULL,
	message_header		TEXT						NOT NULL,
	message_ack_req		TEXT						NULL,
	message_manifest	TEXT						NULL,
--	message_ack				TEXT						NULL,
--	message_error			TEXT						NULL,
	ack_type					INT							NULL,
	ack_header				TEXT						NULL,
	ack_content				TEXT						NULL,
	status						INT							NOT NULL,
	status_date				DATETIME				NOT NULL,
	nr_retries				INT							DEFAULT 0 NOT NULL,
	next_retry_time		DATETIME				NULL
);

ALTER TABLE ebms_message ADD CONSTRAINT unique_ebms_message_conversation_id UNIQUE (conversation_id, cpa_id, sequence_nr);

CREATE TABLE ebms_attachment
(
	id								INT							IDENTITY(1,1)	PRIMARY KEY,
	ebms_message_id		INT							NOT NULL FOREIGN KEY REFERENCES ebms_message(id),
	name							VARCHAR(128)		NOT NULL,
	content_type			VARCHAR(64)			NOT NULL,
	content						IMAGE						NOT NULL
);

CREATE TABLE afleverbericht
(
	id								INT							IDENTITY(1,1)	PRIMARY KEY,
--	ebms_message_id	INT							NOT NULL FOREIGN KEY REFERENCES ebms_message(id),
--	message_id				INT							NULL FOREIGN KEY REFERENCES message(id)
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	message_date			DATETIME				NOT NULL,
	oin								VARCHAR(64)			NOT NULL,
	content						TEXT						NOT NULL,
	status						INT							NOT NULL,
	status_date				DATETIME				NOT NULL
);

CREATE TABLE message
(
	id								INT							IDENTITY(1,1)	PRIMARY KEY,
--	afleverbericht_id	INT							NULL FOREIGN KEY REFERENCES afleverbericht(id),
	time_stamp				DATETIME				NOT NULL,
	oin								VARCHAR(64)			NOT NULL,
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	message_date			DATETIME				NOT NULL,
	name							VARCHAR(128)		NOT NULL,
	content_type			VARCHAR(64)			NOT NULL,
	content						TEXT						NOT NULL,
	status						INT							NOT NULL,
	status_date				DATETIME				NOT NULL
	nr_retries				INT							DEFAULT 0 NOT NULL,
	processing_classification	INT			NULL
);

CREATE TABLE attachment
(
	id							INT							IDENTITY(1,1)	PRIMARY KEY,
	message_id			INT							NOT NULL FOREIGN KEY REFERENCES message(id),
	name						VARCHAR(128)		NOT NULL,
	content_type		VARCHAR(64)			NOT NULL,
	content					IMAGE						NOT NULL
);

CREATE TABLE organisation
(
	id							INT							IDENTITY(1,1)	PRIMARY KEY,
	name						VARCHAR(32)			NOT NULL UNIQUE,
	oin							VARCHAR(64)			NOT NULL UNIQUE,
	email_address		VARCHAR(128)		NOT NULL
);
