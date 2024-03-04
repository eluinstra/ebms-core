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

EXEC sp_rename 'url', 'url_mapping';

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						IMAGE					NOT NULL,
	destination				IMAGE					NOT NULL,
	cpa_id						VARCHAR(256)	NULL,
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
);

EXEC sp_rename 'ebms_event.channel_id', 'receive_channel_id', 'COLUMN';
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(256) NULL;

DROP INDEX ebms_message.i_ebms_message;
CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id,message_nr);
