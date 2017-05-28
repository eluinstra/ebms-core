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
	cpa_id						VARCHAR(128)		NOT NULL UNIQUE,
	cpa								CLOB						NOT NULL
);

CREATE TABLE ebms_message
(
	id								INTEGER					GENERATED BY DEFAULT AS IDENTITY (START WITH 1) PRIMARY KEY,
	time_stamp				TIMESTAMP				NOT NULL,
	cpa_id						VARCHAR(256)		NOT NULL,
	conversation_id		VARCHAR(256)		NOT NULL,
	sequence_nr				INTEGER					NULL,
	message_id				VARCHAR(256)		NOT NULL,
	message_nr				INTEGER					DEFAULT 0 NOT NULL,
	ref_to_message_id	VARCHAR(256)		NULL,
	time_to_live			TIMESTAMP				NULL,
	from_role					VARCHAR(256)		NULL,
	to_role						VARCHAR(256)		NULL,
	service						VARCHAR(256)		NOT NULL,
	action						VARCHAR(256)		NOT NULL,
	signature					CLOB						NULL,
	message_header		CLOB						NOT NULL,
	sync_reply				CLOB						NULL,
	message_order			CLOB						NULL,
	ack_requested			CLOB						NULL,
	content						CLOB						NULL,
	status						INTEGER					NULL,
	status_time				TIMESTAMP				NULL
);

--ALTER TABLE ebms_message ADD CONSTRAINT uc_ebms_message_id UNIQUE (message_id,message_nr);

--CREATE INDEX i_ebms_message ON ebms_message (cpa_id,status,message_nr);

CREATE TABLE ebms_attachment
(
	ebms_message_id		INTEGER					NOT NULL,
	name							VARCHAR(256)		NULL,
	content_id 				VARCHAR(256) 		NOT NULL,
	content_type			VARCHAR(255)		NOT NULL,
	content						BLOB						NOT NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id)
);

--ALTER TABLE ebms_attachment ADD CONSTRAINT uc_ebms_attachment UNIQUE (ebms_message_id,content_id);

CREATE TABLE ebms_event
(
	ebms_message_id		INTEGER					NOT NULL,
	time							TIMESTAMP				DEFAULT NOW() NOT NULL,
	type							INTEGER					NOT NULL,
	status						INTEGER					DEFAULT 0 NOT NULL,
	status_time				TIMESTAMP				DEFAULT NOW() NOT NULL,
	error_message			CLOB						NULL,
	FOREIGN KEY (ebms_message_id) REFERENCES ebms_message(id)
);

--ALTER TABLE ebms_event ADD CONSTRAINT uc_ebms_event UNIQUE (ebms_message_id,time);

--CREATE INDEX i_ebms_event ON ebms_event (status);