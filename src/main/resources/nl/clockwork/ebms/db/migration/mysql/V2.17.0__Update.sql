RENAME TABLE url TO url_mapping; 

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
);

ALTER TABLE client_certificate ADD CONSTRAINT uc_client_certificate_id UNIQUE (source(255));

ALTER TABLE ebms_event CHANGE COLUMN channel_id receive_channel_id VARCHAR(256) NOT NULL;
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) NULL;
