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

DROP INDEX i_ebms_message;
CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id,message_nr);

EXEC sp_rename 'ebms_attachment', 'ebms_attachment_old';

CREATE TABLE ebms_attachment
(
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						IMAGE						NOT NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message (message_id,message_nr)
)
AS SELECT m.message_id, m.message_nr, a.order_nr, a.name, a.content_id, a.content_type, a.content
FROM ebms_message m, ebms_attachment_old a
WHERE m.id = a.message_id;

ALTER TABLE ebms_message DROP CONSTRAINT uc_ebms_message_id;
ALTER TABLE ebms_message DROP PRIMARY KEY;
ALTER TABLE ebms_message DROP COLUMN id;
ALTER TABLE ebms_message ADD PRIMARY KEY (message_id,message_nr)
