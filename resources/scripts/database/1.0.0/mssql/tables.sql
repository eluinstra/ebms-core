ALTER TABLE message
ADD
	nr_retries								INT							DEFAULT 0 NOT NULL,
	processing_classification	INT							NULL
;
