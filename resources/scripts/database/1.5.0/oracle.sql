UPDATE ebms_message SET service = decode(service_type,null,'',service_type || ':') || service;

ALTER TABLE ebms_message DROP COLUMN service_type;
