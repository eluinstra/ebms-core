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
import nl.clockwork.ebms.Constants.EbMSEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSEvent;

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
	String getMessageIdByRefToMessageId(String refToMessageId, Service service, String...actions) throws DAOException;
	EbMSMessageContext getMessageContextByRefToMessageId(String refToMessageId, Service service, String...actions) throws DAOException;
	EbMSDocument getDocument(String refToMessageId, Service service, String...actions) throws DAOException;
	MessageHeader getMessageHeader(String messageId) throws DAOException;
	EbMSDocument getDocument(String messageId) throws DAOException;
	EbMSMessageStatus getMessageStatus(String messageId) throws DAOException;

	List<EbMSEvent> getLatestEventsByEbMSMessageIdBefore(Date timestamp, EbMSEventStatus status) throws DAOException;
	void updateEvent(Date timestamp, String messageId, EbMSEventStatus status, String errorMessage) throws DAOException;
	void deleteEventsBefore(Date timestamp, String messageId, EbMSEventStatus status) throws DAOException;

	long insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException;
	long insertDuplicateMessage(Date timestamp, EbMSMessage message) throws DAOException;
	void updateMessageStatus(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;
	void insertEvent(String messageId, EbMSEventType type, String uri) throws DAOException;
	void insertEvent(EbMSEvent event) throws DAOException;
	void insertEvents(List<EbMSEvent> events) throws DAOException;
	void deleteEvents(String messageId, EbMSEventStatus status) throws DAOException;

	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException;
	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException;
	EbMSMessageContent getMessageContent(String messageId) throws DAOException;
	void updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;
	void updateMessages(List<String> messageIds, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;

}
