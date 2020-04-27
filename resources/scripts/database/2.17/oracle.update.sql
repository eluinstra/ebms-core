CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL UNIQUE,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL
);

ALTER TABLE ebms_event RENAME COLUMN channel_id TO receive_channel_id;
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) DEFAULT 'DUMMY' NOT NULL;
