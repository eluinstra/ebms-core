EXEC sp_rename 'url', 'url_mapping';

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL UNIQUE,
	source						IMAGE					NOT NULL,
	destination				IMAGE					NOT NULL
);

EXEC sp_rename 'ebms_event.channel_id', 'receive_channel_id', 'COLUMN';
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) NOT NULL DEFAULT 'DUMMY';
