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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Document;

import com.querydsl.core.types.dsl.BooleanExpression;

import nl.clockwork.ebms.Action;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.querydsl.model.QEbmsMessage;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;

public interface EbMSDAO
{
	void executeTransaction(Action action);

	boolean existsMessage(String messageId);
	boolean existsIdenticalMessage(EbMSBaseMessage message);

	Optional<EbMSMessageContent> getMessageContent(String messageId);
	Optional<EbMSMessageContentMTOM> getMessageContentMTOM(String messageId);
	Optional<EbMSMessageContext> getMessageContext(String messageId);
	Optional<EbMSMessageContext> getMessageContextByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);
	Optional<Document> getDocument(String messageId);
	Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId);
	Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);
	Optional<EbMSMessageStatus> getMessageStatus(String messageId);
	Optional<Instant> getPersistTime(String messageId);
	Optional<EbMSAction> getMessageAction(String messageId);

	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status);
	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr);

	void insertMessage(Instant timestamp, Instant persistTime, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments, EbMSMessageStatus status);
	void insertDuplicateMessage(Instant timestamp, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments);

	int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus);

	void deleteAttachments(String messageId);

	static BooleanExpression applyFilter(QEbmsMessage messageTable, EbMSMessageContext messageContext, BooleanExpression whereClause)
	{
		if (messageContext != null)
		{
			if (messageContext.getCpaId() != null)
				whereClause.and(messageTable.cpaId.eq(messageContext.getCpaId()));
			if (messageContext.getFromParty() != null)
			{
				if (messageContext.getFromParty().getPartyId() != null)
					whereClause.and(messageTable.fromPartyId.eq(messageContext.getFromParty().getPartyId()));
				if (messageContext.getFromParty().getRole() != null)
					whereClause.and(messageTable.fromRole.eq(messageContext.getFromParty().getRole()));
			}
			if (messageContext.getToParty() != null)
			{
				if (messageContext.getToParty().getPartyId() != null)
					whereClause.and(messageTable.toPartyId.eq(messageContext.getToParty().getPartyId()));
				if (messageContext.getToParty().getRole() != null)
					whereClause.and(messageTable.toRole.eq(messageContext.getToParty().getRole()));
			}
			if (messageContext.getService() != null)
				whereClause.and(messageTable.service.eq(messageContext.getService()));
			if (messageContext.getAction() != null)
				whereClause.and(messageTable.action.eq(messageContext.getAction()));
			if (messageContext.getConversationId() != null)
				whereClause.and(messageTable.conversationId.eq(messageContext.getConversationId()));
			if (messageContext.getMessageId() != null)
				whereClause.and(messageTable.messageId.eq(messageContext.getMessageId()));
			if (messageContext.getRefToMessageId() != null)
				whereClause.and(messageTable.refToMessageId.eq(messageContext.getRefToMessageId()));
			if (messageContext.getMessageStatus() != null)
				whereClause.and(messageTable.status.eq(messageContext.getMessageStatus()));
		}
		return whereClause;
	}
}
