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

import io.vavr.Tuple2;
import lombok.val;
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
	void executeTransaction(Runnable runnable);

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

	String insertMessage(Instant timestamp, Instant persistTime, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments, EbMSMessageStatus status);
	Tuple2<String,Integer> insertDuplicateMessage(Instant timestamp, Document document, EbMSBaseMessage message, List<EbMSAttachment> attachments);

	int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus);

	int deleteAttachments(String messageId);

	public static String getMessageContextFilter(EbMSMessageContext messageContext, List<Object> parameters)
	{
		val result = new StringBuffer();
		if (messageContext != null)
		{
			if (messageContext.getCpaId() != null)
			{
				parameters.add(messageContext.getCpaId());
				result.append(" and ebms_message.cpa_id = ?");
			}
			if (messageContext.getFromParty() != null)
			{
				if (messageContext.getFromParty().getPartyId() != null)
				{
					parameters.add(messageContext.getFromParty().getPartyId());
					result.append(" and ebms_message.from_party_id = ?");
				}
				if (messageContext.getFromParty().getRole() != null)
				{
					parameters.add(messageContext.getFromParty().getRole());
					result.append(" and ebms_message.from_role = ?");
				}
			}
			if (messageContext.getToParty() != null)
			{
				if (messageContext.getToParty().getPartyId() != null)
				{
					parameters.add(messageContext.getToParty().getPartyId());
					result.append(" and ebms_message.to_party_id = ?");
				}
				if (messageContext.getToParty().getRole() != null)
				{
					parameters.add(messageContext.getToParty().getRole());
					result.append(" and ebms_message.to_role = ?");
				}
			}
			if (messageContext.getService() != null)
			{
				parameters.add(messageContext.getService());
				result.append(" and ebms_message.service = ?");
			}
			if (messageContext.getAction() != null)
			{
				parameters.add(messageContext.getAction());
				result.append(" and ebms_message.action = ?");
			}
			if (messageContext.getConversationId() != null)
			{
				parameters.add(messageContext.getConversationId());
				result.append(" and ebms_message.conversation_id = ?");
			}
			if (messageContext.getMessageId() != null)
			{
				parameters.add(messageContext.getMessageId());
				result.append(" and ebms_message.message_id = ?");
			}
			if (messageContext.getRefToMessageId() != null)
			{
				parameters.add(messageContext.getRefToMessageId());
				result.append(" and ebms_message.ref_to_message_id = ?");
			}
			if (messageContext.getMessageStatus() != null)
			{
				parameters.add(messageContext.getMessageStatus().getId());
				result.append(" and ebms_message.status = ?");
			}
		}
		return result.toString();
	}
}
