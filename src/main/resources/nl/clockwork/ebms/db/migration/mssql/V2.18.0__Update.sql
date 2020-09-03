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

EXEC sp_rename 'ebms_event', 'send_task';
EXEC sp_rename N'i_ebms_event', N'i_send_task', N'INDEX';
EXEC sp_rename 'ebms_event_log', 'send_log';
EXEC sp_rename N'i_ebms_event_log', N'i_send_log', N'INDEX';
EXEC sp_rename 'ebms_message_event', 'message_event';
EXEC sp_rename N'i_ebms_message_event', N'i_message_event', N'INDEX';
