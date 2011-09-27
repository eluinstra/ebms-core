ALTER TABLE ebms_message ADD CONSTRAINT unique_ebms_message_conversation_id UNIQUE (conversation_id, cpa_id, sequence_nr);
