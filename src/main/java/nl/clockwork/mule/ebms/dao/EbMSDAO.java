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

import java.util.List;

import javax.activation.DataSource;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.model.EbMSAcknowledgment;
import nl.clockwork.mule.ebms.model.EbMSAttachment;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageError;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

public interface EbMSDAO
{
	CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException;

	boolean exists(String messageId) throws DAOException;
	long getIdByMessageId(String messageId) throws DAOException;
	MessageHeader getMessageHeader(long id) throws DAOException;
	MessageHeader getMessageHeader(String messageId) throws DAOException;
	List<DataSource> getAttachments(long messageId) throws DAOException;
	List<EbMSAttachment> getEbMSAttachments(long messageId) throws DAOException;
	EbMSBaseMessage getEbMSMessage(long messageId) throws DAOException;
	void insertMessage(EbMSMessage message) throws DAOException;
	void insertMessage(EbMSMessageError messageError, EbMSMessageStatus status) throws DAOException;
	void insertMessage(EbMSAcknowledgment acknowledgment, EbMSMessageStatus status) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status, EbMSMessageError messageError) throws DAOException;
	void insertMessage(EbMSMessage message, EbMSMessageStatus status, EbMSAcknowledgment acknowledgment) throws DAOException;
}
