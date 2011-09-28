ALTER TABLE ebms_message ADD CONSTRAINT unique_ebms_message_conversation_id UNIQUE (conversation_id, cpa_id, sequence_nr);

ALTER TABLE ebms_channel ADD CONSTRAINT unique_ebms_channel_cpa_id_action_id UNIQUE (cpa_id, action_id);
