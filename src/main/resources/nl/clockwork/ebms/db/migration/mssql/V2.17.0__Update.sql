EXEC sp_rename 'url', 'url_mapping';

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						IMAGE					NOT NULL,
	destination				IMAGE					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
);

EXEC sp_rename 'ebms_event.channel_id', 'receive_channel_id', 'COLUMN';
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) NULL;
