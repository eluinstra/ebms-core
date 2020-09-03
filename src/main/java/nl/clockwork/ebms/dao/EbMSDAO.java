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
import nl.clockwork.ebms.model.EbMSMessageProperties;
import nl.clockwork.ebms.model.QEbmsMessage;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MTOMMessage;
import nl.clockwork.ebms.service.model.MessageFilter;

public interface EbMSDAO
{
	boolean existsMessage(String messageId);
	boolean existsIdenticalMessage(EbMSBaseMessage message);

	Optional<Message> getMessage(String messageId);
	Optional<MTOMMessage> getMTOMMessage(String messageId);
	Optional<EbMSMessageProperties> getEbMSMessageProperties(String messageId);
	Optional<EbMSMessageProperties> getEbMSMessagePropertiesByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);
	Optional<Document> getDocument(String messageId);
	Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId);
	Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions);
	Optional<EbMSMessageStatus> getMessageStatus(String messageId);
	Optional<Instant> getPersistTime(String messageId);
	Optional<EbMSAction> getMessageAction(String messageId);

	List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status);
	List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status, int maxNr);

	long insertMessage(Instant timestamp, Instant persistTime, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments, EbMSMessageStatus status);
	long insertDuplicateMessage(Instant timestamp, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments);

	long updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus);

	long deleteAttachments(String messageId);

	static BooleanBuilder applyFilter(QEbmsMessage table, MessageFilter messageFilter, BooleanBuilder builder)
	{
		if (messageFilter != null)
		{
			if (messageFilter.getCpaId() != null)
				builder.and(table.cpaId.eq(messageFilter.getCpaId()));
			if (messageFilter.getFromParty() != null)
			{
				if (messageFilter.getFromParty().getPartyId() != null)
					builder.and(table.fromPartyId.eq(messageFilter.getFromParty().getPartyId()));
				if (messageFilter.getFromParty().getRole() != null)
					builder.and(table.fromRole.eq(messageFilter.getFromParty().getRole()));
			}
			if (messageFilter.getToParty() != null)
			{
				if (messageFilter.getToParty().getPartyId() != null)
					builder.and(table.toPartyId.eq(messageFilter.getToParty().getPartyId()));
				if (messageFilter.getToParty().getRole() != null)
					builder.and(table.toRole.eq(messageFilter.getToParty().getRole()));
			}
			if (messageFilter.getService() != null)
				builder.and(table.service.eq(messageFilter.getService()));
			if (messageFilter.getAction() != null)
				builder.and(table.action.eq(messageFilter.getAction()));
			if (messageFilter.getConversationId() != null)
				builder.and(table.conversationId.eq(messageFilter.getConversationId()));
			if (messageFilter.getMessageId() != null)
				builder.and(table.messageId.eq(messageFilter.getMessageId()));
			if (messageFilter.getRefToMessageId() != null)
				builder.and(table.refToMessageId.eq(messageFilter.getRefToMessageId()));
		}
		return builder;
	}
}
