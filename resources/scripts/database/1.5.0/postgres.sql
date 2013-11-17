UPDATE ebms_message SET service = array_to_string(string_to_array(service_type,service),':');

ALTER TABLE ebms_message DROP COLUMN service_type;
