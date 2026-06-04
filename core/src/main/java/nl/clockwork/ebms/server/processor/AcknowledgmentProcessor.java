/*
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
package nl.clockwork.ebms.server.processor;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.SOAPException;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskManager;
import nl.clockwork.ebms.common.EbMSMessageStatus;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPAUtils;
import nl.clockwork.ebms.common.dao.EbMSDAO;
import nl.clockwork.ebms.common.event.MessageEventListener;
import nl.clockwork.ebms.common.message.EbMSMessageFactory;
import nl.clockwork.ebms.common.message.EbMSMessageUtils;
import nl.clockwork.ebms.common.model.EbMSAcknowledgment;
import nl.clockwork.ebms.common.model.EbMSDocument;
import nl.clockwork.ebms.common.model.EbMSMessage;
import nl.clockwork.ebms.common.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.common.validation.ValidatorException;
import nl.clockwork.ebms.server.validation.DuplicateMessageException;
import nl.clockwork.ebms.server.validation.EbMSMessageValidator;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.xml.sax.SAXException;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class AcknowledgmentProcessor
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	DeliveryTaskManager deliveryTaskManager;
	@NonNull
	EbMSMessageValidator messageValidator;
	@NonNull
	DuplicateMessageHandler duplicateMessageHandler;
	@NonNull
	EbMSMessageFactory ebMSMessageFactory;
	@NonNull
	EbMSSignatureGenerator signatureGenerator;
	@NonNull
	MessageEventListener messageEventListener;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	public EbMSDocument processAcknowledgment(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message, final boolean isSyncReply)
			throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val acknowledgment = createAcknowledgment(timestamp, message);
		val acknowledgmentDocument = EbMSMessageUtils.getEbMSDocument(acknowledgment);
		signatureGenerator.generate(message.getAckRequested(), acknowledgmentDocument, acknowledgment);
		Runnable storeMessages = () ->
		{
			storeMessages(timestamp, messageDocument, message, acknowledgmentDocument, acknowledgment);
			storeDeliveryTask(acknowledgment, isSyncReply);
			messageEventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
		};
		ebMSDAO.executeTransaction(storeMessages);
		return acknowledgmentDocument;
	}

	private void storeMessages(
			Instant timestamp,
			EbMSDocument messageDocument,
			EbMSMessage message,
			EbMSDocument acknowledgmentDocument,
			EbMSAcknowledgment acknowledgment)
	{
		val messageHeader = message.getMessageHeader();
		val deliveryChannel = cpaManager
				.getReceiveDeliveryChannel(
						messageHeader.getCPAId(),
						messageHeader.getTo().getPartyId(),
						messageHeader.getTo().getRole(),
						message.getMessageHeader().getService(),
						messageHeader.getAction())
				.orElseThrow(
						() -> StreamUtils.illegalStateException(
								"ReceiveDeliveryChannel",
								messageHeader.getCPAId(),
								messageHeader.getTo().getPartyId(),
								messageHeader.getTo().getRole(),
								message.getMessageHeader().getService(),
								messageHeader.getAction()));
		val persistTime = CPAUtils.getPersistTime(messageHeader.getMessageData().getTimestamp(), deliveryChannel);
		ebMSDAO.insertMessage(timestamp, persistTime, messageDocument.getMessage(), message, message.getAttachments(), EbMSMessageStatus.RECEIVED);
		ebMSDAO.insertMessage(timestamp, persistTime, acknowledgmentDocument.getMessage(), acknowledgment, Collections.emptyList(), null);
	}

	private void storeDeliveryTask(EbMSAcknowledgment acknowledgment, boolean isSyncReply)
	{
		MessageHeader messageHeader = acknowledgment.getMessageHeader();
		val sendDeliveryChannel = cpaManager
				.getReceiveDeliveryChannel(
						messageHeader.getCPAId(),
						messageHeader.getFrom().getPartyId(),
						messageHeader.getFrom().getRole(),
						messageHeader.getService(),
						messageHeader.getAction())
				.orElseThrow(
						() -> StreamUtils.illegalStateException(
								"SendDeliveryChannel",
								messageHeader.getCPAId(),
								messageHeader.getFrom().getPartyId(),
								messageHeader.getFrom().getRole(),
								messageHeader.getService(),
								messageHeader.getAction()));
		val receiveDeliveryChannel = cpaManager
				.getReceiveDeliveryChannel(
						messageHeader.getCPAId(),
						messageHeader.getTo().getPartyId(),
						messageHeader.getTo().getRole(),
						messageHeader.getService(),
						messageHeader.getAction())
				.orElseThrow(
						() -> StreamUtils.illegalStateException(
								"ReceiveDeliveryChannel",
								messageHeader.getCPAId(),
								messageHeader.getTo().getPartyId(),
								messageHeader.getTo().getRole(),
								messageHeader.getService(),
								messageHeader.getAction()));
		if (!isSyncReply)
			deliveryTaskManager.insertTask(
					deliveryTaskManager.createNewTask(
							messageHeader.getCPAId(),
							sendDeliveryChannel.getChannelId(),
							receiveDeliveryChannel.getChannelId(),
							messageHeader.getMessageData().getMessageId(),
							messageHeader.getMessageData().getTimeToLive(),
							messageHeader.getMessageData().getTimestamp(),
							false));
	}

	public void processAcknowledgment(Instant timestamp, EbMSDocument acknowledgmentDocument, EbMSMessage requestMessage, EbMSAcknowledgment acknowledgment)
			throws XPathExpressionException, JAXBException, ParserConfigurationException, SAXException, IOException
	{
		try
		{
			messageValidator.validateAcknowledgment(acknowledgmentDocument, requestMessage, acknowledgment);
			storeAcknowledgment(timestamp, acknowledgmentDocument, acknowledgment);
		}
		catch (DuplicateMessageException e)
		{
			duplicateMessageHandler.handleAcknowledgment(acknowledgment);
		}
		catch (ValidatorException e)
		{
			val persistTime = ebMSDAO.getPersistTime(acknowledgment.getMessageHeader().getMessageData().getRefToMessageId());
			ebMSDAO.insertMessage(timestamp, persistTime.orElse(null), acknowledgmentDocument.getMessage(), acknowledgment, Collections.emptyList(), null);
			log.warn("Unable to process Acknowledgment " + acknowledgment.getMessageHeader().getMessageData().getMessageId(), e);
		}
	}

	public EbMSAcknowledgment createAcknowledgment(final Instant timestamp, final EbMSMessage message)
	{
		return ebMSMessageFactory.createEbMSAcknowledgment(message, timestamp);
	}

	public void storeAcknowledgment(final Instant timestamp, final EbMSDocument acknowledgmentDocument, final EbMSAcknowledgment acknowledgment)
			throws EbMSProcessingException
	{
		val responseMessageHeader = acknowledgment.getMessageHeader();
		val persistTime = ebMSDAO.getPersistTime(responseMessageHeader.getMessageData().getRefToMessageId());
		Runnable storeMessage = () ->
		{
			ebMSDAO.insertMessage(timestamp, persistTime.orElse(null), acknowledgmentDocument.getMessage(), acknowledgment, Collections.emptyList(), null);
			if (ebMSDAO.updateMessage(responseMessageHeader.getMessageData().getRefToMessageId(), EbMSMessageStatus.CREATED, EbMSMessageStatus.DELIVERED) > 0)
			{
				messageEventListener.onMessageDelivered(responseMessageHeader.getMessageData().getRefToMessageId());
				if (deleteEbMSAttachmentsOnMessageProcessed)
					ebMSDAO.deleteAttachments(responseMessageHeader.getMessageData().getRefToMessageId());
			}
		};
		ebMSDAO.executeTransaction(storeMessage);
	}
}
