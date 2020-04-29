RENAME TABLE url TO url_mapping; 

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL UNIQUE,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL
);

ALTER TABLE client_certificate ADD CONSTRAINT uc_client_certificate_id UNIQUE (source(255));

ALTER TABLE ebms_event CHANGE COLUMN channel_id receive_channel_id VARCHAR(256) NOT NULL;
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) NOT NULL DEFAULT 'DUMMY';
