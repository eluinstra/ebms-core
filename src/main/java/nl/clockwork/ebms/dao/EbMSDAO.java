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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Document;

import nl.clockwork.ebms.Action;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;

public interface EbMSDAO
{
	void executeTransaction(Action action) throws DAOException;

	boolean existsMessage(String messageId) throws DAOException;
	boolean existsIdenticalMessage(EbMSBaseMessage message) throws DAOException;

	Optional<EbMSMessageContent> getMessageContent(String messageId) throws DAOException;
	Optional<EbMSMessageContentMTOM> getMessageContentMTOM(String messageId) throws DAOException;
	Optional<EbMSMessageContext> getMessageContext(String messageId) throws DAOException;
	Optional<EbMSMessageContext> getMessageContextByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions) throws DAOException;
	Optional<Document> getDocument(String messageId) throws DAOException;
	Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId) throws DAOException;
	Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions) throws DAOException;
	Optional<EbMSMessageStatus> getMessageStatus(String messageId) throws DAOException;
	Optional<Instant> getPersistTime(String messageId);

	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException;
	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException;

	void insertMessage(Instant timestamp, Instant persistTime, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments, EbMSMessageStatus status) throws DAOException;
	void insertDuplicateMessage(Instant timestamp, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments) throws DAOException;

	int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException;

	void deleteAttachments(String messageId);

	static Instant toInstant(Timestamp timestamp)
	{
		return timestamp != null ? timestamp.toInstant() : null;
	}
}
