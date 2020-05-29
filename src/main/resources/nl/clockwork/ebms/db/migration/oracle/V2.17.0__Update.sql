RENAME TABLE url TO url_mapping; 

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
);

ALTER TABLE ebms_event RENAME COLUMN channel_id TO receive_channel_id;
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) NULL;
