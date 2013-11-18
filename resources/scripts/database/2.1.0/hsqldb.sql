--UPDATE ebms_message SET service = CONCAT_WS(':',service_type,service);
UPDATE ebms_message SET service = CASE WHEN service_type is null THEN service ELSE CASE WHEN service is null THEN service_type ELSE service_type + ':' + service END END;

ALTER TABLE ebms_message DROP COLUMN service_type;

UPDATE ebms_message SET status = 13 WHERE status = 12;

UPDATE ebms_message SET status = 12 WHERE status = 11;

UPDATE ebms_message SET status = 11 WHERE status = 10;

UPDATE ebms_message SET status = 10 WHERE service <> 'urn:oasis:names:tc:ebxml-msg:service' and status IS NULL;

UPDATE ebms_event SET type = 4 where type = 0;

UPDATE ebms_event SET type = 0 where type = 1;
