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
package nl.clockwork.ebms.service;


import java.io.IOException;
import java.time.Instant;
import java.util.List;
import javax.xml.bind.JAXBException;
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
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.DeliveryManager;
import nl.clockwork.ebms.delivery.task.DeliveryTaskManager;
import nl.clockwork.ebms.event.MessageEventDAO;
import nl.clockwork.ebms.event.MessageEventType;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageProperties;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.service.model.MTOMMessage;
import nl.clockwork.ebms.service.model.MTOMMessageRequest;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageMapper;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.service.model.MessageRequestProperties;
import nl.clockwork.ebms.service.model.MessageStatus;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.LoggingUtils;
import nl.clockwork.ebms.util.LoggingUtils.Status;
import nl.clockwork.ebms.util.StreamUtils;
import nl.clockwork.ebms.validation.MessagePropertiesValidator;
import org.slf4j.MDC;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@AllArgsConstructor
class EbMSMessageServiceHandler
{
	@NonNull
	DeliveryManager deliveryManager;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	MessageEventDAO messageEventDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSMessageFactory ebMSMessageFactory;
	@NonNull
	DeliveryTaskManager deliveryTaskManager;
	@NonNull
	MessagePropertiesValidator messagePropertiesValidator;
	@NonNull
	EbMSSignatureGenerator signatureGenerator;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	public void ping(String cpaId, String fromPartyId, String toPartyId)
	{
		log.debug("Ping {}", cpaId);
		messagePropertiesValidator.validate(cpaId, fromPartyId, toPartyId);
		val request = ebMSMessageFactory.createEbMSPing(cpaId, fromPartyId, toPartyId);
		val response = deliveryManager.sendMessage(request);
		if (response.isPresent())
		{
			if (!EbMSAction.PONG.getAction().equals(response.get().getMessageHeader().getAction()))
				throw new EbMSProcessingException("No valid response received!");
		}
		else
			throw new EbMSProcessingException("No response received!");
	}

	public String sendMessage(MessageRequest messageRequest)
			throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		log.debug("SendMessage");
		messagePropertiesValidator.validate(messageRequest.getProperties());
		val message = ebMSMessageFactory.createEbMSMessage(messageRequest);
		if (LoggingUtils.mdc == Status.ENABLED)
			MDC.setContextMap(LoggingUtils.getPropertyMap(message.getMessageHeader()));
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document, message);
		storeMessage(document.getMessage(), message);
		val result = message.getMessageHeader().getMessageData().getMessageId();
		log.info("Created message {}", result);
		return result;
	}

	public String sendMessageMTOM(MTOMMessageRequest messageRequest)
			throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		log.debug("SendMessage");
		messagePropertiesValidator.validate(messageRequest.getProperties());
		val message = ebMSMessageFactory.createEbMSMessageMTOM(messageRequest);
		if (LoggingUtils.mdc == Status.ENABLED)
			MDC.setContextMap(LoggingUtils.getPropertyMap(message.getMessageHeader()));
		val document = EbMSMessageUtils.getEbMSDocument(message);
		signatureGenerator.generate(document, message);
		storeMessage(document.getMessage(), message);
		String result = message.getMessageHeader().getMessageData().getMessageId();
		log.info("Created message {}", result);
		return result;
	}

	public String resendMessage(String messageId)
	{
		log.debug("ResendMessage {}", messageId);
		return ebMSDAO.getMessage(messageId).map(p ->
		{
			try
			{
				val messageRequest = MessageMapper.INSTANCE.toMessage(p);
				resetMessage(messageRequest.getProperties());
				val message = ebMSMessageFactory.createEbMSMessage(messageRequest);
				if (LoggingUtils.mdc == Status.ENABLED)
					MDC.setContextMap(LoggingUtils.getPropertyMap(message.getMessageHeader()));
				val document = EbMSMessageUtils.getEbMSDocument(message);
				signatureGenerator.generate(document, message);
				storeMessage(document.getMessage(), message);
				val newMessageId = message.getMessageHeader().getMessageData().getMessageId();
				log.info("Created message {}", newMessageId);
				return newMessageId;
			}
			catch (SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerFactoryConfigurationError
					| TransformerException e)
			{
				throw new EbMSProcessorException(e);
			}
		}).orElseThrow(() -> new NotFoundException("Not found message " + messageId));
	}

	public List<String> getUnprocessedMessageIds(MessageFilter messageFilter, Integer maxNr)
	{
		log.debug("GetMessageIds");
		return maxNr == null || maxNr == 0
				? ebMSDAO.getMessageIds(messageFilter, EbMSMessageStatus.RECEIVED)
				: ebMSDAO.getMessageIds(messageFilter, EbMSMessageStatus.RECEIVED, maxNr);
	}

	public Message getMessage(final String messageId, Boolean process)
	{
		log.debug("GetMessage {}", messageId);
		if (process != null && process)
			processMessage(messageId);
		return ebMSDAO.getMessage(messageId).orElseThrow(NotFoundException::new);
	}

	public MTOMMessage getMessageMTOM(String messageId, Boolean process)
	{
		log.debug("GetMessage {}", messageId);
		if (process != null && process)
			processMessage(messageId);
		return ebMSDAO.getMTOMMessage(messageId).orElseThrow(NotFoundException::new);
	}

	public void processMessage(final String messageId)
	{
		log.debug("ProcessMessage {}", messageId);
		Runnable runnable = () ->
		{
			if (ebMSDAO.updateMessage(messageId, EbMSMessageStatus.RECEIVED, EbMSMessageStatus.PROCESSED) > 0 && deleteEbMSAttachmentsOnMessageProcessed)
				ebMSDAO.deleteAttachments(messageId);
		};
		ebMSDAO.executeTransaction(runnable);
		log.info("Processed message {}", messageId);
	}

	public MessageStatus getMessageStatus(String messageId)
	{
		log.debug("GetMessageStatus {}", messageId);
		return ebMSDAO.getEbMSMessageProperties(messageId)
				.map(p -> getMessageStatus(messageId, p))
				.orElseThrow(() -> new NotFoundException("No message found with messageId " + messageId + "!"));
	}

	private MessageStatus getMessageStatus(String messageId, EbMSMessageProperties messageProperties) throws EbMSProcessorException
	{
		if (EbMSAction.EBMS_SERVICE_URI.equals(messageProperties.getService()))
			throw new EbMSMessageServiceException("Found MSH message " + messageId);
		else
		{
			val fromPartyId = messageProperties.getFromParty().getPartyId();
			val toPartyId = messageProperties.getToParty().getPartyId();
			val request = ebMSMessageFactory.createEbMSStatusRequest(messageProperties.getCpaId(), fromPartyId, toPartyId, messageId);
			val response = deliveryManager.sendMessage(request);
			return response.map(r -> (createMessageStatus(r))).orElseThrow(() -> new EbMSMessageServiceException("No response received!"));
		}
	}

	private MessageStatus createMessageStatus(EbMSBaseMessage message)
	{
		if (message instanceof EbMSStatusResponse)
		{
			val timestamp =
					((EbMSStatusResponse)message).getStatusResponse().getTimestamp() == null ? null : ((EbMSStatusResponse)message).getStatusResponse().getTimestamp();
			val messageStatus = ((EbMSStatusResponse)message).getStatusResponse().getMessageStatus();
			val status = EbMSMessageStatus.get(messageStatus).orElseThrow(() -> new NotFoundException("No EbMSMessageStatus found for " + messageStatus));
			return new MessageStatus(timestamp, status);
		}
		else
			throw new EbMSMessageServiceException("No valid response received!");
	}

	public List<MessageEvent> getUnprocessedMessageEvents(MessageFilter messageFilter, MessageEventType[] eventTypes, Integer maxNr)
			throws EbMSMessageServiceException
	{
		log.debug("GetMessageEvents");
		return maxNr == null || maxNr == 0
				? messageEventDAO.getEbMSMessageEvents(messageFilter, eventTypes)
				: messageEventDAO.getEbMSMessageEvents(messageFilter, eventTypes, maxNr);
	}

	public void processMessageEvent(final String messageId)
	{
		log.debug("ProcessMessageEvent {}", messageId);
		Runnable processMessage = () ->
		{
			messageEventDAO.processEbMSMessageEvent(messageId);
			processMessage(messageId);
		};
		ebMSDAO.executeTransaction(processMessage);
	}

	private void resetMessage(@NonNull MessageRequestProperties messageRequestProperties)
	{
		// properties.setConversationId(null);
		messageRequestProperties.setMessageId(null);
	}

	private void storeMessage(Document document, EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			val timestamp = Instant.now();
			val messageHeader = message.getMessageHeader();
			val service = CPAUtils.toString(messageHeader.getService());
			val sendDeliveryChannel = cpaManager
					.getSendDeliveryChannel(
							messageHeader.getCPAId(),
							messageHeader.getFrom().getPartyId(),
							messageHeader.getFrom().getRole(),
							service,
							messageHeader.getAction())
					.orElseThrow(
							() -> StreamUtils.illegalStateException(
									"SendDeliveryChannel",
									messageHeader.getCPAId(),
									messageHeader.getFrom().getPartyId(),
									messageHeader.getFrom().getRole(),
									service,
									messageHeader.getAction()));
			val receiveDeliveryChannel = cpaManager
					.getReceiveDeliveryChannel(
							messageHeader.getCPAId(),
							messageHeader.getTo().getPartyId(),
							messageHeader.getTo().getRole(),
							service,
							messageHeader.getAction())
					.orElseThrow(
							() -> StreamUtils.illegalStateException(
									"ReceiveDeliveryChannel",
									messageHeader.getCPAId(),
									messageHeader.getTo().getPartyId(),
									messageHeader.getTo().getRole(),
									service,
									messageHeader.getAction()));
			val persistTime = CPAUtils.getPersistTime(timestamp, receiveDeliveryChannel);
			val confidential = cpaManager.isSendingConfidential(
					messageHeader.getCPAId(),
					messageHeader.getFrom().getPartyId(),
					messageHeader.getFrom().getRole(),
					service,
					messageHeader.getAction());
			Runnable runnable = () ->
			{
				ebMSDAO.insertMessage(timestamp, persistTime, document, message, message.getAttachments(), EbMSMessageStatus.CREATED);
				deliveryTaskManager.insertTask(
						deliveryTaskManager.createNewTask(
								messageHeader.getCPAId(),
								sendDeliveryChannel.getChannelId(),
								receiveDeliveryChannel.getChannelId(),
								messageHeader.getMessageData().getMessageId(),
								messageHeader.getMessageData().getTimeToLive(),
								messageHeader.getMessageData().getTimestamp(),
								confidential));
			};
			ebMSDAO.executeTransaction(runnable);
		}
		catch (IllegalStateException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
}
