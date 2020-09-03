/**
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.task;

import java.time.Instant;
import java.util.List;

interface SendTaskDAO
{
	List<SendTask> getTasksBefore(Instant timestamp, String serverId);
	List<SendTask> getTasksBefore(Instant timestamp, String serverId, int maxNr);
	long insertTask(SendTask task, String serverId);
	long insertLog(String messageId, Instant timestamp, String uri, SendTaskStatus status, String errorMessage);
	long updateTask(SendTask task);
	long deleteTask(String messageId);
}