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

import com.querydsl.core.BooleanBuilder;

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
	boolean existsMessage(String messageId);
	boolean existsIdenticalMessage(EbMSBaseMessage message);

	Optional<EbMSMessageContent> getMessageContent(String messageId);
	Optional<EbMSMessageContentMTOM> getMessageContentMTOM(String messageId);
	Optional<EbMSMessageContext> getMessageContext(String messageId);
	Optional<EbMSMessageContext> getMessageContextByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);
	Optional<Document> getDocument(String messageId);
	Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId);
	Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);
	Optional<Instant> getPersistTime(String messageId);
	Optional<EbMSAction> getMessageAction(String messageId);

	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status);
	List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr);

	long insertMessage(Instant timestamp, Instant persistTime, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments, EbMSMessageStatus status);
	long insertDuplicateMessage(Instant timestamp, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments);

	long updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus);

	long deleteAttachments(String messageId);

	static BooleanBuilder applyFilter(QEbmsMessage table, EbMSMessageContext messageContext, BooleanBuilder builder)
	{
		if (messageContext != null)
		{
			if (messageContext.getCpaId() != null)
				builder.and(table.cpaId.eq(messageContext.getCpaId()));
			if (messageContext.getFromParty() != null)
			{
				if (messageContext.getFromParty().getPartyId() != null)
					builder.and(table.fromPartyId.eq(messageContext.getFromParty().getPartyId()));
				if (messageContext.getFromParty().getRole() != null)
					builder.and(table.fromRole.eq(messageContext.getFromParty().getRole()));
			}
			if (messageContext.getToParty() != null)
			{
				if (messageContext.getToParty().getPartyId() != null)
					builder.and(table.toPartyId.eq(messageContext.getToParty().getPartyId()));
				if (messageContext.getToParty().getRole() != null)
					builder.and(table.toRole.eq(messageContext.getToParty().getRole()));
			}
			if (messageContext.getService() != null)
				builder.and(table.service.eq(messageContext.getService()));
			if (messageContext.getAction() != null)
				builder.and(table.action.eq(messageContext.getAction()));
			if (messageContext.getConversationId() != null)
				builder.and(table.conversationId.eq(messageContext.getConversationId()));
			if (messageContext.getMessageId() != null)
				builder.and(table.messageId.eq(messageContext.getMessageId()));
			if (messageContext.getRefToMessageId() != null)
				builder.and(table.refToMessageId.eq(messageContext.getRefToMessageId()));
			if (messageContext.getMessageStatus() != null)
				builder.and(table.status.eq(messageContext.getMessageStatus()));
		}
		return builder;
	}
}
