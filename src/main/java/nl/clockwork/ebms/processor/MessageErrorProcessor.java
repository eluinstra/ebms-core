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
package nl.clockwork.ebms.processor;


import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.task.DeliveryTaskManager;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.validation.DuplicateMessageException;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidationException;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.xml.sax.SAXException;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class MessageErrorProcessor
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

	public EbMSDocument processMessageError(
			final Instant timestamp,
			final EbMSDocument messageDocument,
			final EbMSMessage message,
			final boolean isSyncReply,
			final EbMSValidationException e) throws DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException,
			IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val messageError = createMessageError(timestamp, message, e);
		val result = EbMSMessageUtils.getEbMSDocument(messageError);
		val messageHeader = message.getMessageHeader();
		val service = CPAUtils.toString(messageError.getMessageHeader().getService());
		val sendDeliveryChannel =
				cpaManager
						.getSendDeliveryChannel(
								messageHeader.getCPAId(),
								messageError.getMessageHeader().getFrom().getPartyId(),
								messageError.getMessageHeader().getFrom().getRole(),
								service,
								messageError.getMessageHeader().getAction())
						.orElse(null);
		val receiveDeliveryChannel =
				cpaManager
						.getReceiveDeliveryChannel(
								messageHeader.getCPAId(),
								messageError.getMessageHeader().getTo().getPartyId(),
								messageError.getMessageHeader().getTo().getRole(),
								service,
								messageError.getMessageHeader().getAction())
						.orElse(null);
		Runnable runnable = () ->
		{
			storeMessages(timestamp, messageDocument, message, result, messageError);
			if (receiveDeliveryChannel != null)
				storeDeliveryTask(messageHeader.getCPAId(), sendDeliveryChannel, receiveDeliveryChannel, messageError, isSyncReply);
		};
		ebMSDAO.executeTransaction(runnable);
		if (!isSyncReply && receiveDeliveryChannel == null)
			throw new ValidationException(DOMUtils.toString(result.getMessage()));
		return result;
	}

	private
			void
			storeMessages(Instant timestamp, EbMSDocument messageDocument, EbMSMessage message, EbMSDocument messageErrorDocument, EbMSMessageError messageError)
	{
		val messageHeader = message.getMessageHeader();
		val service = CPAUtils.toString(message.getMessageHeader().getService());
		val deliveryChannel =
				cpaManager
						.getReceiveDeliveryChannel(
								messageHeader.getCPAId(),
								messageHeader.getTo().getPartyId(),
								messageHeader.getTo().getRole(),
								service,
								messageHeader.getAction())
						.orElse(null);
		val persistTime = deliveryChannel != null ? CPAUtils.getPersistTime(timestamp, deliveryChannel) : null;
		ebMSDAO.insertMessage(timestamp, persistTime, messageDocument.getMessage(), message, message.getAttachments(), EbMSMessageStatus.FAILED);
		ebMSDAO.insertMessage(timestamp, persistTime, messageErrorDocument.getMessage(), messageError, Collections.emptyList(), null);
	}

	private void storeDeliveryTask(
			String cpaId,
			DeliveryChannel sendDeliveryChannel,
			DeliveryChannel receiveDeliveryChannel,
			EbMSMessageError messageError,
			boolean isSyncReply)
	{
		if (!isSyncReply)
		{
			deliveryTaskManager.insertTask(
					deliveryTaskManager.createNewTask(
							cpaId,
							sendDeliveryChannel.getChannelId(),
							receiveDeliveryChannel.getChannelId(),
							messageError.getMessageHeader().getMessageData().getMessageId(),
							messageError.getMessageHeader().getMessageData().getTimeToLive(),
							messageError.getMessageHeader().getMessageData().getTimestamp(),
							false));
		}
	}

	public void processMessageError(Instant timestamp, EbMSDocument response, EbMSMessage requestMessage, EbMSMessageError messageError)
			throws TransformerException
	{
		try
		{
			messageValidator.validateMessageError(requestMessage, messageError, timestamp);
			storeMessageError(timestamp, response, requestMessage, messageError);
		}
		catch (DuplicateMessageException e)
		{
			duplicateMessageHandler.handleMessageError(timestamp, response, messageError);
		}
		catch (ValidationException e)
		{
			val persistTime = ebMSDAO.getPersistTime(messageError.getMessageHeader().getMessageData().getRefToMessageId());
			ebMSDAO.insertMessage(timestamp, persistTime.orElse(null), response.getMessage(), messageError, Collections.emptyList(), null);
			log.warn("Unable to process MessageError " + messageError.getMessageHeader().getMessageData().getMessageId(), e);
		}
	}

	public EbMSMessageError createMessageError(final Instant timestamp, final EbMSMessage message, final EbMSValidationException e)
			throws DatatypeConfigurationException, JAXBException
	{
		val errorList = EbMSMessageUtils.createErrorList();
		errorList.getError().add(e.getError());
		val messageError = ebMSMessageFactory.createEbMSMessageError(message, errorList, timestamp);
		return messageError;
	}

	public
			void
			storeMessageError(final Instant timestamp, final EbMSDocument messageErrorDocument, final EbMSMessage message, final EbMSMessageError messageError)
	{
		val responseMessageHeader = messageError.getMessageHeader();
		val persistTime = ebMSDAO.getPersistTime(responseMessageHeader.getMessageData().getRefToMessageId());
		Runnable runnable = () ->
		{
			ebMSDAO.insertMessage(timestamp, persistTime.orElse(null), messageErrorDocument.getMessage(), messageError, Collections.emptyList(), null);
			if (ebMSDAO.updateMessage(responseMessageHeader.getMessageData().getRefToMessageId(), EbMSMessageStatus.CREATED, EbMSMessageStatus.DELIVERY_FAILED) > 0)
			{
				messageEventListener.onMessageFailed(responseMessageHeader.getMessageData().getRefToMessageId());
				if (deleteEbMSAttachmentsOnMessageProcessed)
					ebMSDAO.deleteAttachments(responseMessageHeader.getMessageData().getRefToMessageId());
			}
		};
		ebMSDAO.executeTransaction(runnable);
	}
}
