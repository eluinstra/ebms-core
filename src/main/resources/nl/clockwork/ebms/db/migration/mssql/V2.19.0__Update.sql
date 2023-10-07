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

DELETE FROM ebms_attachment WHERE ebms_message_id IN (SELECT id FROM ebms_message WHERE message_nr > 0);
DELETE FROM ebms_message WHERE message_nr > 0;

ALTER TABLE ebms_message DROP CONSTRAINT uc_ebms_message_id;
DROP INDEX i_ebms_ref_to_message ON ebms_message;

ALTER TABLE ebms_attachment ADD message_id VARCHAR(256) NULL;
