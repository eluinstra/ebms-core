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
ALTER TABLE ebms_attachment ADD COLUMN message_id VARCHAR(256) NOT NULL;

DECLARE @ConstraintName nvarchar(200)
SELECT @ConstraintName = Name FROM SYS.DEFAULT_CONSTRAINTS
WHERE PARENT_OBJECT_ID = OBJECT_ID('ebms_message')
AND PARENT_COLUMN_ID = (SELECT column_id FROM sys.columns
                        WHERE NAME = N'id'
                        AND object_id = OBJECT_ID(N'ebms_message'))
IF @ConstraintName IS NOT NULL
EXEC('ALTER TABLE ebms_message DROP CONSTRAINT ' + @ConstraintName)

DECLARE @ConstraintName1 nvarchar(200)
SELECT @ConstraintName1 = Name FROM SYS.DEFAULT_CONSTRAINTS
WHERE PARENT_OBJECT_ID = OBJECT_ID('ebms_message')
AND PARENT_COLUMN_ID = (SELECT column_id FROM sys.columns
                        WHERE NAME = N'message_nr'
                        AND object_id = OBJECT_ID(N'ebms_message'))
IF @ConstraintName1 IS NOT NULL
EXEC('ALTER TABLE ebms_message DROP CONSTRAINT ' + @ConstraintName1)

ALTER TABLE ebms_attachment DROP COLUMN ebms_message_id;

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id);
ALTER TABLE ebms_attachment ADD CONSTRAINT uc_ebms_attachment UNIQUE (message_id,order_nr);
