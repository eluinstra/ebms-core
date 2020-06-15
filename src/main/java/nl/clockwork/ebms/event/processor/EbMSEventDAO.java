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
import nl.clockwork.ebms.dao.DAOException;

public interface EbMSEventDAO
{
	void executeTransaction(Action action) throws DAOException;

	Optional<EbMSAction> getMessageAction(String messageId) throws DAOException;
	List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId) throws DAOException;
	List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId, int maxNr) throws DAOException;
	void insertEvent(EbMSEvent event, String serverId) throws DAOException;
	void insertEventLog(String messageId, Instant timestamp, String uri, EbMSEventStatus status, String errorMessage) throws DAOException;
	void updateEvent(EbMSEvent event) throws DAOException;
	void deleteEvent(String messageId) throws DAOException;
}