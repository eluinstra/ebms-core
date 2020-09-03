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
package nl.clockwork.ebms.processor;

import java.time.Instant;
import java.util.Collections;

import javax.xml.transform.TransformerException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.util.StreamUtils;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.ValidationException;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class DuplicateMessageHandler
{
  @NonNull
  EbMSDAO ebMSDAO;
  @NonNull
  CPAManager cpaManager;
  @NonNull
	EventManager eventManager;
  @NonNull
	EbMSMessageValidator messageValidator;
	boolean storeDuplicateMessage;
	boolean storeDuplicateMessageAttachments;

	public EbMSDocument handleMessage(final Instant timestamp, EbMSDocument document, final EbMSMessage message) throws EbMSProcessingException
	{
		val messageHeader = message.getMessageHeader();
		if (isIdenticalMessage(message))
		{
			log.warn("Message " + messageHeader.getMessageData().getMessageId() + " is duplicate!");
			if (messageValidator.isSyncReply(message))
			{
				if (storeDuplicateMessage)
					ebMSDAO.insertDuplicateMessage(timestamp,document.getMessage(),message,storeDuplicateMessageAttachments ? message.getAttachments() : Collections.emptyList());
				val result = ebMSDAO.getEbMSDocumentByRefToMessageId(
						messageHeader.getCPAId(),
						messageHeader.getMessageData().getMessageId(),
						EbMSAction.MESSAGE_ERROR,
						EbMSAction.ACKNOWLEDGMENT);
				StreamUtils.ifNotPresent(result,() -> log.warn("No response found for duplicate message " + messageHeader.getMessageData().getMessageId() + "!"));
				return result.orElse(null);
			}
			else
			{
				if (storeDuplicateMessage)
					ebMSDAO.insertDuplicateMessage(timestamp,document.getMessage(),message,storeDuplicateMessageAttachments ? message.getAttachments() : Collections.emptyList());
				val messageProperties = ebMSDAO.getEbMSMessagePropertiesByRefToMessageId(
						messageHeader.getCPAId(),
						messageHeader.getMessageData().getMessageId(),
						EbMSAction.MESSAGE_ERROR,
						EbMSAction.ACKNOWLEDGMENT);
				StreamUtils.ifNotPresent(messageProperties,() -> log.warn("No response found for duplicate message " + messageHeader.getMessageData().getMessageId() + "!"));
				val service = CPAUtils.toString(CPAUtils.createEbMSMessageService());
				val sendDeliveryChannel =	cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),messageHeader.getTo().getPartyId(),messageHeader.getTo().getRole(),service,null)
						.orElse(null);
				val receiveDeliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,null)
						.orElse(null);
				if (receiveDeliveryChannel != null && messageProperties.isPresent())
					eventManager.createEvent(messageHeader.getCPAId(),sendDeliveryChannel,receiveDeliveryChannel,messageProperties.get().getMessageId(),messageHeader.getMessageData().getTimeToLive(),messageProperties.get().getTimestamp(),false);
				if (receiveDeliveryChannel == null && messageProperties.isPresent())
					try
					{
						val result = ebMSDAO.getDocument(messageProperties.get().getMessageId());
						throw new ValidationException(DOMUtils.toString(result.get()));
					}
					catch (TransformerException e)
					{
						throw new EbMSProcessingException("Error creating response message for MessageId " + messageHeader.getMessageData().getMessageId() + "!",e);
					}
				return null;
			}
		}
		else
			throw new EbMSProcessingException("MessageId " + messageHeader.getMessageData().getMessageId() + " already used!");
	}

	public void handleMessageError(final Instant timestamp, EbMSDocument responseDocument, final EbMSMessageError responseMessage) throws EbMSProcessingException
	{
		if (isIdenticalMessage(responseMessage))
		{
			log.warn("MessageError " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
			if (storeDuplicateMessage)
				ebMSDAO.insertDuplicateMessage(timestamp,responseDocument.getMessage(),responseMessage,Collections.emptyList());
		}
		else
			throw new EbMSProcessingException("MessageId " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " already used!");
	}
	
	public void handleAcknowledgment(final Instant timestamp, EbMSDocument responseDocument, final EbMSAcknowledgment responseMessage) throws EbMSProcessingException
	{
		if (isIdenticalMessage(responseMessage))
		{
			log.warn("Acknowledgment " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
			if (storeDuplicateMessage)
				ebMSDAO.insertDuplicateMessage(timestamp,responseDocument.getMessage(),responseMessage,Collections.emptyList());
		}
		else
			throw new EbMSProcessingException("MessageId " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " already used!");
	}
	
	private boolean isIdenticalMessage(EbMSBaseMessage message)
	{
		return ebMSDAO.existsIdenticalMessage(message);
	}
}
