UPDATE ebms_message SET service = CONCAT_WS(':',service_type,service);

ALTER TABLE ebms_message DROP COLUMN service_type;
