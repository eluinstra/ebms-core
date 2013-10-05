UPDATE ebms_message SET service = CASE service_type WHEN null THEN service ELSE service_type + ':' + service END;

ALTER TABLE ebms_message DROP COLUMN service_type;