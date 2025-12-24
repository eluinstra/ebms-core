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

ALTER TABLE ebms_attachment ADD COLUMN message_id VARCHAR(256) NULL;
UPDATE ebms_attachment SET message_id = (SELECT message_id FROM ebms_message WHERE id = ebms_message_id);
ALTER TABLE ebms_attachment MODIFY COLUMN message_id VARCHAR(256) NOT NULL;

SET @constraint_name = (SELECT CONSTRAINT_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'ebms_attachment'
AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @s = concat('ALTER TABLE ebms_attachment DROP FOREIGN KEY ', @constraint_name);
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE ebms_message MODIFY id INT NOT NULL;
ALTER TABLE ebms_message DROP PRIMARY KEY;
ALTER TABLE ebms_message DROP COLUMN id;
ALTER TABLE ebms_message DROP COLUMN message_nr;

ALTER TABLE ebms_attachment DROP COLUMN ebms_message_id;

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id);
ALTER TABLE ebms_attachment ADD CONSTRAINT uc_ebms_attachment UNIQUE (message_id,order_nr);
