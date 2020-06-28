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

RENAME TABLE url TO url_mapping; 

CREATE TABLE certificate_mapping
(
	id								VARCHAR(255)	NOT NULL,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL,
	cpa_id						VARCHAR(255)	NULL,
	CONSTRAINT uc_certificate_mapping UNIQUE(id,cpa_id)
);

ALTER TABLE certificate_mapping ADD CONSTRAINT uc_certificate_mapping_id UNIQUE (id);

ALTER TABLE ebms_event CHANGE COLUMN channel_id receive_channel_id VARCHAR(255) NOT NULL;
ALTER TABLE ebms_event ADD send_channel_id VARCHAR(255) NULL;

DROP INDEX i_ebms_message;
CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id,message_nr);

RENAME TABLE ebms_attachment TO ebms_attachment_old

CREATE TABLE ebms_attachment
(
	message_id				VARCHAR(255)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(255)		NULL,
	content_id 				VARCHAR(255) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						LONGBLOB 				NOT NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message (message_id,message_nr)
)
SELECT m.message_id, m.message_nr, a.order_nr, a.name, a.content_id, a.content_type, a.content
FROM ebms_message m, ebms_attachment_old a
WHERE m.id = a.message_id
ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ebms_message DROP CONSTRAINT uc_ebms_message_id;
ALTER TABLE ebms_message DROP PRIMARY KEY;
ALTER TABLE ebms_message DROP COLUMN id;
ALTER TABLE ebms_message ADD PRIMARY KEY (message_id,message_nr)
