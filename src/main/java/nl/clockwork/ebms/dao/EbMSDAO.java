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
package nl.clockwork.ebms.dao;

import java.util.Date;
import java.util.List;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.w3c.dom.Document;

public interface EbMSDAO
{
	void executeTransaction(DAOTransactionCallback callback);

	boolean existsCPA(String cpaId) throws DAOException;
	CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException;
	List<String> getCPAIds() throws DAOException;
	void insertCPA(CollaborationProtocolAgreement cpa, String url) throws DAOException;
	int updateCPA(CollaborationProtocolAgreement cpa, String url) throws DAOException;
	int deleteCPA(String cpaId) throws DAOException;
	
	String getUrl(String cpaId);
	int updateUrl(String cpaId, String url);

	boolean existsMessage(String messageId) throws DAOException;
	EbMSMessageContent getMessageContent(String messageId) throws DAOException;
	EbMSMessageContext getMessageContext(String messageId) throws DAOException;
	EbMSMessageContext getMessageContextByRefToMessageId(String refToMessageId, Service service, String...actions) throws DAOException;
	Document getDocument(String messageId) throws DAOException;
	EbMSDocument getEbMSDocumentIfUnsent(String messageId) throws DAOException;
	EbMSDocument getEbMSDocumentByRefToMessageId(String refToMessageId, Service service, String...actions) throws DAOException;
	EbMSMessageStatus getMessageStatus(String messageId) throws DAOException;

	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException;
	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException;

	void insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException;
	void insertDuplicateMessage(Date timestamp, EbMSMessage message) throws DAOException;
	int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;
	void updateMessages(List<String> messageIds, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;

	List<EbMSEvent> getEventsBefore(Date timestamp) throws DAOException;
	void insertEvent(EbMSEvent event) throws DAOException;
	void updateEvent(EbMSEvent event) throws DAOException;
	void deleteEvent(String messageId) throws DAOException;
	void insertEventLog(String messageId, Date timestamp, String uri, EbMSEventStatus status, String errorMessage) throws DAOException;

}
