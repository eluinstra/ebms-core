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

ALTER TABLE ebms_message RENAME COLUMN message_nr TO message_version;
ALTER TABLE ebms_attachment RENAME COLUMN message_nr TO message_version;
ALTER TABLE ebms_event RENAME COLUMN message_nr TO message_version;
ALTER TABLE ebms_event_log RENAME COLUMN message_nr TO message_version;
ALTER TABLE ebms_message_event RENAME COLUMN message_nr TO message_version;

ALTER TABLE ebms_message ADD sequence_nr NUMBER(8) NULL;
