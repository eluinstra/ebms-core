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

CREATE TABLE cpa
(
	cpa_id						VARCHAR(255)		NOT NULL,
	cpa								MEDIUMTEXT			NOT NULL,
	url								VARCHAR(255)		NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE cpa ADD CONSTRAINT uc_cpa_id UNIQUE (cpa_id);

CREATE TABLE ebms_message
(
	id								INTEGER					AUTO_INCREMENT PRIMARY KEY,
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(255)		NOT NULL,
	conversation_id		VARCHAR(255)		NOT NULL,
	message_id				VARCHAR(255)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	ref_to_message_id	VARCHAR(255)		NULL,
	time_to_live			TIMESTAMP				NULL,
	from_party_id			VARCHAR(255)		NOT NULL,
	from_role					VARCHAR(255)		NULL,
	to_party_id				VARCHAR(255)		NOT NULL,
	to_role						VARCHAR(255)		NULL,
	service						VARCHAR(255)		NOT NULL,
	action						VARCHAR(255)		NOT NULL,
	content						TEXT						NULL,
	status						SMALLINT				NULL,
	status_time				TIMESTAMP				NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id,message_nr);

CREATE INDEX i_ebms_message ON ebms_message (cpa_id,status,message_nr);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INTEGER					NOT NULL,
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(255)		NULL,
	content_id 				VARCHAR(255) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						LONGBLOB 				NOT NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE ebms_event
(
	cpa_id						VARCHAR(255)		NOT NULL,
	channel_id				VARCHAR(255)		NOT NULL,
	message_id				VARCHAR(255)		NOT NULL,
	time_to_live			TIMESTAMP				NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	retries						SMALLINT				DEFAULT 0 NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE ebms_event ADD CONSTRAINT uc_ebms_event UNIQUE (message_id);
CREATE INDEX i_ebms_event ON ebms_event (time_stamp);

CREATE TABLE ebms_event_log
(
	message_id				VARCHAR(255)		NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	uri								VARCHAR(255)		NULL,
	status						SMALLINT				NOT NULL,
	error_message			TEXT						NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX i_ebms_event_log ON ebms_event (message_id);
