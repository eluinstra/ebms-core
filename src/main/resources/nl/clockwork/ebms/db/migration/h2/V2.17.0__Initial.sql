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
	cpa_id						VARCHAR(256)		NOT NULL UNIQUE,
	cpa								CLOB						NOT NULL
);

CREATE TABLE url_mapping
(
	source						VARCHAR(256)		NOT NULL UNIQUE,
	destination				VARCHAR(256)		NOT NULL
);

CREATE TABLE certificate_mapping
(
	id								VARCHAR(256)	NOT NULL,
	source						BLOB					NOT NULL,
	destination				BLOB					NOT NULL,
	cpa_id						VARCHAR(256)	NULL
);

CREATE TABLE ebms_message
(
	id								BIGINT					AUTO_INCREMENT PRIMARY KEY,
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				DEFAULT 0 NOT NULL,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	persist_time			TIMESTAMP				NULL,
	from_party_id			VARCHAR(256)		NOT NULL,
	from_role					VARCHAR(256)		NULL,
	to_party_id				VARCHAR(256)		NOT NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						CLOB						NULL,
	status						TINYINT					NULL,
	status_time				TIMESTAMP				NULL
);

CREATE INDEX i_ebms_message ON ebms_message (message_id,message_nr);

CREATE INDEX i_ebms_ref_to_message ON ebms_message (ref_to_message_id,message_nr);

CREATE TABLE ebms_attachment
(
	ebms_message_id		BIGINT					NOT NULL,
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BLOB						NOT NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message (id)
);

CREATE TABLE ebms_event
(
	cpa_id							VARCHAR(256)		NOT NULL,
	send_channel_id			VARCHAR(256)		NULL,
	receive_channel_id	VARCHAR(256)		NOT NULL,
	message_id					VARCHAR(256)		NOT NULL UNIQUE,
	time_to_live				TIMESTAMP				NULL,
	time_stamp					TIMESTAMP				NOT NULL,
	is_confidential			BOOLEAN					NOT NULL,
	retries							SMALLINT				DEFAULT 0 NOT NULL,
	server_id						VARCHAR(256)		NULL
);

CREATE INDEX i_ebms_event ON ebms_event (time_stamp);

CREATE TABLE ebms_event_log
(
	message_id				VARCHAR(256)		NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	uri								VARCHAR(256)		NULL,
	status						TINYINT					NOT NULL,
	error_message			CLOB						NULL
);

CREATE INDEX i_ebms_event_log ON ebms_event_log (message_id);

CREATE TABLE ebms_message_event
(
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	event_type				TINYINT					NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	processed					BOOLEAN					DEFAULT 0 NOT NULL
);

CREATE INDEX i_ebms_message_event ON ebms_message_event (time_stamp);
