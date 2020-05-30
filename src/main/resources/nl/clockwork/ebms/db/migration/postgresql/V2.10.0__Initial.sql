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
	cpa								TEXT						NOT NULL,
	url								VARCHAR(256)		NULL
);

CREATE TABLE ebms_message
(
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL DEFAULT 0,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	from_party_id			VARCHAR(256)		NOT NULL,
	from_role					VARCHAR(256)		NULL,
	to_party_id				VARCHAR(256)		NOT NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	content						TEXT						NULL,
	status						SMALLINT				NULL,
	status_time				TIMESTAMP				NULL,
	PRIMARY KEY (message_id,message_nr)
);

CREATE INDEX i_ebms_message ON ebms_message (cpa_id,status,message_nr);

CREATE TABLE ebms_attachment
(
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				SMALLINT				NOT NULL,
	order_nr					SMALLINT				NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BYTEA						NOT NULL,
	FOREIGN KEY (message_id,message_nr) REFERENCES ebms_message (message_id,message_nr)
);

CREATE TABLE ebms_event
(
	cpa_id						VARCHAR(256)		NOT NULL,
	channel_id				VARCHAR(256)		NOT NULL,
	message_id				VARCHAR(256)		NOT NULL UNIQUE,
	time_to_live			TIMESTAMP				NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	retries						SMALLINT				DEFAULT 0 NOT NULL
);

CREATE INDEX i_ebms_event ON ebms_event (time_stamp);

CREATE TABLE ebms_event_log
(
	message_id				VARCHAR(256)		NOT NULL,
	time_stamp				TIMESTAMP				NOT NULL,
	uri								VARCHAR(256)		NULL,
	status						SMALLINT				NOT NULL,
	error_message			TEXT						NULL
);

CREATE INDEX i_ebms_event_log ON ebms_event_log (message_id);
