--
-- Copyright 2011 Clockwork
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

DELETE FROM ebms_attachment WHERE message_nr > 0;
DELETE FROM ebms_message WHERE message_nr > 0;

DROP INDEX i_ebms_ref_to_message;
ALTER TABLE ebms_attachment DROP CONSTRAINT uc_ebms_attachment;

ALTER TABLE message_event DROP COLUMN message_nr CASCADE CONSTRAINTS;
ALTER TABLE delivery_log DROP COLUMN message_nr CASCADE CONSTRAINTS;
ALTER TABLE delivery_task DROP COLUMN message_nr CASCADE CONSTRAINTS;
ALTER TABLE ebms_attachment DROP COLUMN message_nr CASCADE CONSTRAINTS;
ALTER TABLE ebms_message DROP COLUMN message_nr CASCADE CONSTRAINTS;

ALTER TABLE ebms_message ADD PRIMARY KEY (message_id);
ALTER TABLE ebms_attachment ADD FOREIGN KEY (message_id) REFERENCES ebms_message (message_id);
ALTER TABLE delivery_task ADD FOREIGN KEY (message_id) REFERENCES ebms_message (message_id);
ALTER TABLE delivery_log ADD FOREIGN KEY (message_id) REFERENCES ebms_message (message_id);
ALTER TABLE message_event ADD FOREIGN KEY (message_id) REFERENCES ebms_message (message_id);

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id);
ALTER TABLE ebms_attachment ADD CONSTRAINT uc_ebms_attachment UNIQUE (message_id,order_nr);
