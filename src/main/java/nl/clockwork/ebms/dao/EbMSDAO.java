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

import java.util.GregorianCalendar;
import java.util.List;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.ebxml.MessageHeader;

public interface EbMSDAO
{
	boolean existsCPA(String cpaId) throws DAOException;
	CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException;
	List<String> getCPAIds() throws DAOException;
	boolean insertCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	boolean updateCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	boolean deleteCPA(String cpaId) throws DAOException;
	
	boolean existsMessage(String messageId) throws DAOException;
	Long getEbMSMessageId(String messageId) throws DAOException;
	Long getEbMSMessageResponseId(String messageId) throws DAOException;
	EbMSBaseMessage getEbMSMessageResponse(String messageId) throws DAOException;
	MessageHeader getMessageHeader(String messageId) throws DAOException;
	EbMSBaseMessage getMessage(long id) throws DAOException;
	EbMSMessageStatus getMessageStatus(String messageId) throws DAOException;

	List<EbMSSendEvent> selectEventsForSending(GregorianCalendar timestamp) throws DAOException;
	void deleteEventsForSending(GregorianCalendar timestamp, String messageId) throws DAOException;
	void deleteExpiredEvents(GregorianCalendar timestamp, String messageId) throws DAOException;

	void insertMessage(EbMSMessage message, List<EbMSSendEvent> sendEvents) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status, EbMSMessageError messageError, EbMSSendEvent sendEvent) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status, EbMSAcknowledgment acknowledgment, EbMSSendEvent sendEvent) throws DAOException;
	void insertMessage(EbMSMessageError messageError, EbMSMessageStatus status) throws DAOException;
	void insertMessage(EbMSAcknowledgment acknowledgment, EbMSMessageStatus status) throws DAOException;
	void insertSendEvent(long id) throws DAOException;

	List<String> getReceivedMessageIds(EbMSMessageContext messageContext) throws DAOException;
	List<String> getReceivedMessageIds(EbMSMessageContext messageContext, int maxNr) throws DAOException;
	EbMSBaseMessage getMessage(String messageId) throws DAOException;
	void processReceivedMessage(String messageId) throws DAOException;
	void processReceivedMessages(List<String> messageIds) throws DAOException;
}
