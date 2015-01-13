CREATE INDEX i_ebms_message ON ebms_message (cpa_id(255),status,message_nr);

CREATE INDEX i_ebms_event ON ebms_event (status);
