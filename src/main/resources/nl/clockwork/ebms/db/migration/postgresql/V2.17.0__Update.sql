ALTER TABLE url RENAME TO url_mapping; 

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						BYTEA					NOT NULL,
	destination				BYTEA					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	UNIQUE(id,cpa_id)
);

ALTER TABLE ebms_event RENAME COLUMN channel_id TO receive_channel_id;
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) NULL;
