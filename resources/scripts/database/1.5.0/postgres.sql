UPDATE ebms_message SET service = array_to_string(array[service_type,service],':');

ALTER TABLE ebms_message DROP COLUMN service_type;

UPDATE ebms_message SET status = 13 WHERE status = 12;

UPDATE ebms_message SET status = 12 WHERE status = 11;

UPDATE ebms_message SET status = 11 WHERE status = 10;

UPDATE ebms_message SET status = 10 WHERE service <> 'urn:oasis:names:tc:ebxml-msg:service' and status IS NULL;
