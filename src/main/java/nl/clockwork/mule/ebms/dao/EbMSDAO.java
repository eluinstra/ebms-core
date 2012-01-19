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
package nl.clockwork.mule.ebms.dao;

import java.util.Date;
import java.util.List;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.model.EbMSAcknowledgment;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageError;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;

public interface EbMSDAO
{
	//boolean exists(String cpaId) throws DAOException;
	CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException;
	List<String> getCPAIds() throws DAOException;
	boolean insertCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	boolean updateCPA(CollaborationProtocolAgreement cpa) throws DAOException;
	boolean deleteCPA(String cpaId) throws DAOException;
	
	boolean exists(String messageId) throws DAOException;
	long getIdByMessageId(String messageId) throws DAOException;
	EbMSBaseMessage getEbMSMessage(long id) throws DAOException;
	EbMSMessageStatus getEbMSMessageStatus(String messageId, Date timestamp) throws DAOException;
	void insertMessage(EbMSMessage message) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status, EbMSMessageError messageError) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status, EbMSAcknowledgment acknowledgment) throws DAOException;
	void insertMessage(EbMSMessageError messageError, EbMSMessageStatus status) throws DAOException;
	void insertMessage(EbMSAcknowledgment acknowledgment, EbMSMessageStatus status) throws DAOException;

	List<String> getMessageIds() throws DAOException;
	List<String> getMessageIds(int maxNr) throws DAOException;
	EbMSBaseMessage getEbMSMessage(String messageId) throws DAOException;
	void processMessage(String messageId) throws DAOException;
	void processMessages(List<String> messageIds) throws DAOException;
}
