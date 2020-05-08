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
package nl.clockwork.ebms.event.processor.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.event.processor.EbMSEvent;
import nl.clockwork.ebms.event.processor.EbMSEventStatus;

public interface EbMSEventDAO
{
	void executeTransaction(DAOTransactionCallback callback) throws DAOException;

	Optional<EbMSAction> getMessageAction(String messageId) throws DAOException;
	List<EbMSEvent> getEventsBefore(Instant timestamp) throws DAOException;
	List<EbMSEvent> getEventsBefore(Instant timestamp, int maxNr) throws DAOException;
	void insertEvent(EbMSEvent event) throws DAOException;
	void insertEventLog(String messageId, Instant timestamp, String uri, EbMSEventStatus status, String errorMessage) throws DAOException;
	void updateEvent(EbMSEvent event) throws DAOException;
	void deleteEvent(String messageId) throws DAOException;
}