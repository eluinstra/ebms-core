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

import java.util.Collections;
import java.util.Date;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.validation.EbMSMessageValidator;

@Builder(setterPrefix = "set")
@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class DuplicateMessageHandler
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

	public EbMSDocument handleMessage(final Date timestamp, EbMSDocument document, final EbMSMessage message) throws EbMSProcessingException
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
				StreamUtils.ifNotPresent(result, () -> log.warn("No response found for duplicate message " + messageHeader.getMessageData().getMessageId() + "!"));
				return result.orElse(null);
			}
			else
			{
				if (storeDuplicateMessage)
					ebMSDAO.insertDuplicateMessage(timestamp,document.getMessage(),message,storeDuplicateMessageAttachments ? message.getAttachments() : Collections.emptyList());
				val context = ebMSDAO.getMessageContextByRefToMessageId(
						messageHeader.getCPAId(),
						messageHeader.getMessageData().getMessageId(),
						EbMSAction.MESSAGE_ERROR,
						EbMSAction.ACKNOWLEDGMENT);
				val toPartyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
				val fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
				val service = CPAUtils.toString(CPAUtils.createEbMSMessageService());
				val sendDeliveryChannel =
						cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,null)
						//.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service));
						.orElse(null);
				val receiveDeliveryChannel =
						cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,null)
						//.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service));
						.orElse(null);
				if (context.isPresent())
					eventManager.createEvent(messageHeader.getCPAId(),sendDeliveryChannel,receiveDeliveryChannel,context.get().getMessageId(),messageHeader.getMessageData().getTimeToLive(),context.get().getTimestamp(),false);
				else
					log.warn("No response found for duplicate message " + messageHeader.getMessageData().getMessageId() + "!");
				return null;
			}
		}
		else
			throw new EbMSProcessingException("MessageId " + messageHeader.getMessageData().getMessageId() + " already used!");
	}

	public void handleMessageError(final Date timestamp, EbMSDocument responseDocument, final EbMSMessageError responseMessage) throws EbMSProcessingException
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
	
	public void handleAcknowledgment(final Date timestamp, EbMSDocument responseDocument, final EbMSAcknowledgment responseMessage) throws EbMSProcessingException
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
