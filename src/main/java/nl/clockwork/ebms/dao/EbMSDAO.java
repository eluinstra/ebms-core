/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.dao;

import java.util.Date;
import java.util.List;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.Service;

public interface EbMSDAO
{
	void executeTransaction(DAOTransactionCallback callback);

	boolean existsCPA(String cpaId) throws DAOException;
	CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException;
	List<String> getCPAIds() throws DAOException;
	boolean insertCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	boolean updateCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	boolean deleteCPA(String cpaId) throws DAOException;
	
	boolean existsMessage(String messageId) throws DAOException;
	boolean existsMessage(String messageId, Service service, String...actions) throws DAOException;
	Long getMessageId(String messageId) throws DAOException;
	Long getMessageId(String messageId, Service service, String...actions) throws DAOException;
	EbMSMessage getMessage(String messageId, Service service, String...actions) throws DAOException;
	MessageHeader getMessageHeader(String messageId) throws DAOException;
	EbMSMessage getMessage(long id) throws DAOException;
	EbMSMessageStatus getMessageStatus(String messageId) throws DAOException;

	List<EbMSSendEvent> selectEventsForSending(Date timestamp) throws DAOException;
	void updateSendEvent(Date timestamp, Long id, EbMSEventStatus status) throws DAOException;
	void deleteEventsBefore(Date timestamp, Long id, EbMSEventStatus status) throws DAOException;

	long insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException;
	void updateMessageStatus(Long id, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;
	void insertSendEvent(long id) throws DAOException;
	void insertSendEvent(long id, EbMSSendEvent sendEvent) throws DAOException;
	void insertSendEvents(long id, List<EbMSSendEvent> sendEvents) throws DAOException;
	void deleteSendEvents(Long id, EbMSEventStatus status) throws DAOException;

	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException;
	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException;
	EbMSMessage getMessage(String messageId) throws DAOException;
	void updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;
	void updateMessages(List<String> messageIds, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;

}
