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
package nl.clockwork.ebms.event.processor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import nl.clockwork.ebms.Action;
import nl.clockwork.ebms.EbMSAction;

public interface EbMSEventDAO
{
	void executeTransaction(Action action);

	Optional<EbMSAction> getMessageAction(String messageId);
	List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId);
	List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId, int maxNr);
	void insertEvent(EbMSEvent event, String serverId);
	void insertEventLog(String messageId, Instant timestamp, String uri, EbMSEventStatus status, String errorMessage);
	void updateEvent(EbMSEvent event);
	void deleteEvent(String messageId);
}