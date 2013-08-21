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

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSSendEvent;

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
	Long getMessageId(String messageId) throws DAOException;
	Long getMessageId(String refToMessageId, Service service, String...actions) throws DAOException;
	EbMSMessage getMessage(String refToMessageId, Service service, String...actions) throws DAOException;
	MessageHeader getMessageHeader(String messageId) throws DAOException;
	EbMSMessage getMessage(long id) throws DAOException;
	EbMSMessageStatus getMessageStatus(String messageId) throws DAOException;

	List<EbMSSendEvent> getLatestEventsByEbMSMessageIdBefore(Date timestamp, EbMSEventStatus status) throws DAOException;
	void updateSendEvent(Date timestamp, Long ebMSMessageId, EbMSEventStatus status, String errorMessage) throws DAOException;
	void deleteEventsBefore(Date timestamp, Long ebMSMessageId, EbMSEventStatus status) throws DAOException;

	long insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException;
	long insertDuplicateMessage(Date timestamp, EbMSMessage message) throws DAOException;
	void updateMessageStatus(Long ebMSMessageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;
	void insertSendEvent(long ebMSMessageId) throws DAOException;
	void insertSendEvent(EbMSSendEvent sendEvent) throws DAOException;
	void insertSendEvents(List<EbMSSendEvent> sendEvents) throws DAOException;
	void deleteSendEvents(Long ebMSMessageId, EbMSEventStatus status) throws DAOException;

	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException;
	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException;
	EbMSMessage getMessage(String messageId) throws DAOException;
	void updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;
	void updateMessages(List<String> messageIds, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;


}
