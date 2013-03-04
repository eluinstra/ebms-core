ALTER TABLE ebms_attachment ADD content_id VARCHAR(256) NULL;

UPDATE ebms_attachment SET content_id = 'cid:1';

ALTER TABLE ebms_attachment ALTER content_id SET NOT NULL;
