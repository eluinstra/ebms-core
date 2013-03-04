ALTER TABLE ebms_attachment ADD content_id VARCHAR(256) NULL;

UPDATE ebms_attachment SET content_id = 'cid:1';

ALTER TABLE ebms_attachment MODIFY content_id VARCHAR(256) NOT NULL;
