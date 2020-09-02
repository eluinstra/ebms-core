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
package nl.clockwork.ebms.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageProperties;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MTOMMessage;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageMapper;
import nl.clockwork.ebms.service.model.MessageProperties;
import nl.clockwork.ebms.service.model.MTOMMessageRequest;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.service.model.MessageStatus;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.StreamUtils;
import nl.clockwork.ebms.validation.MessagePropertiesValidator;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@AllArgsConstructor
@Transactional(transactionManager = "dataSourceTransactionManager")
class EbMSMessageServiceHandler
{
  @NonNull
	DeliveryManager deliveryManager;
  @NonNull
	EbMSDAO ebMSDAO;
  @NonNull
	EbMSMessageEventDAO ebMSMessageEventDAO;
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageFactory ebMSMessageFactory;
  @NonNull
	EventManager eventManager;
  @NonNull
	MessagePropertiesValidator messagePropertiesValidator;
  @NonNull
	EbMSSignatureGenerator signatureGenerator;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	public void ping(String cpaId, String fromPartyId, String toPartyId) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("Ping " + cpaId);
			messagePropertiesValidator.validate(cpaId,fromPartyId,toPartyId);
			val request = ebMSMessageFactory.createEbMSPing(cpaId,fromPartyId,toPartyId);
			val response = deliveryManager.sendMessage(request);
			if (response.isPresent())
			{
				if (!EbMSAction.PONG.getAction().equals(response.get().getMessageHeader().getAction()))
					throw new EbMSProcessingException("No valid response received!");
			}
			else
				throw new EbMSProcessingException("No response received!");
		}
		catch (Exception e)
		{
			log.error("Ping " + cpaId,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public String sendMessage(MessageRequest messageRequest) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("SendMessage");
			messagePropertiesValidator.validate(messageRequest.getProperties());
			val ebMSMessage = ebMSMessageFactory.createEbMSMessage(messageRequest);
			val document = EbMSMessageUtils.getEbMSDocument(ebMSMessage);
			signatureGenerator.generate(document,ebMSMessage);
			storeMessage(document.getMessage(),ebMSMessage);
			val result = ebMSMessage.getMessageHeader().getMessageData().getMessageId();
			log.info("Message " + result + " created");
			return result;
		}
		catch (Exception e)
		{
			log.error("SendMessage " + messageRequest,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public String sendMessageMTOM(MTOMMessageRequest message) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("SendMessage");
			messagePropertiesValidator.validate(message.getProperties());
			val ebMSMessage = ebMSMessageFactory.createEbMSMessageMTOM(message);
			val document = EbMSMessageUtils.getEbMSDocument(ebMSMessage);
			signatureGenerator.generate(document,ebMSMessage);
			storeMessage(document.getMessage(),ebMSMessage);
			String result = ebMSMessage.getMessageHeader().getMessageData().getMessageId();
			log.info("Message " + result + " created");
			return result;
		}
		catch (Exception e)
		{
			log.error("SendMessage " + message,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("ResendMessage " + messageId);
			return ebMSDAO.getMessage(messageId).map(mc ->
			{
				try
				{
					resetMessage(mc.getProperties());
					val message = ebMSMessageFactory.createEbMSMessage(MessageMapper.INSTANCE.toMessage(mc));
					val document = EbMSMessageUtils.getEbMSDocument(message);
					signatureGenerator.generate(document,message);
					storeMessage(document.getMessage(),message);
					val newMessageId = message.getMessageHeader().getMessageData().getMessageId();
					log.info("Message " + newMessageId + " created");
					return newMessageId;
				}
				catch (SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerFactoryConfigurationError | TransformerException e)
				{
					throw new EbMSProcessorException(e);
				}
			}).orElseThrow(() -> new EbMSMessageServiceException("Message not found!"));
		}
		catch (Exception e)
		{
			log.error("ResendMessage " + messageId);
			throw new EbMSMessageServiceException(e);
		}
	}

	public List<String> getUnprocessedMessageIds(MessageFilter messageFilter, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("GetMessageIds");
			return maxNr == null || maxNr == 0 ? ebMSDAO.getMessageIds(messageFilter,EbMSMessageStatus.RECEIVED) : ebMSDAO.getMessageIds(messageFilter,EbMSMessageStatus.RECEIVED,maxNr);
		}
		catch (Exception e)
		{
			log.error("GetMessageIds " + messageFilter,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public Message getMessage(final String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("GetMessage " + messageId);
			if (process != null && process)
				processMessage(messageId);
			return ebMSDAO.getMessage(messageId).orElse(null);
		}
		catch (Exception e)
		{
			log.error("GetMessage " + messageId,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public MTOMMessage getMessageMTOM(String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("GetMessage " + messageId);
			if (process != null && process)
				processMessage(messageId);
			return ebMSDAO.getMTOMMessage(messageId).orElse(null);
		}
		catch (Exception e)
		{
			log.error("GetMessage " + messageId,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public void processMessage(final String messageId) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("ProcessMessage " + messageId);
			if (ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED) > 0)
			{
				if (deleteEbMSAttachmentsOnMessageProcessed)
					ebMSDAO.deleteAttachments(messageId);
				log.info("Message " + messageId + " processed");
			}
		}
		catch (Exception e)
		{
			log.error("ProcessMessage " + messageId,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("GetMessageStatus " + messageId);
			return ebMSDAO.getEbMSMessageProperties(messageId)
					.map(p -> getMessageStatus(messageId,p))
					.orElseThrow(() -> new EbMSMessageServiceException("No message found with messageId " + messageId + "!"));
		}
		catch (Exception e)
		{
			log.error("GetMessageStatus " + messageId,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	private MessageStatus getMessageStatus(String messageId, EbMSMessageProperties messageProperties) throws EbMSProcessorException
	{
		if (EbMSAction.EBMS_SERVICE_URI.equals(messageProperties.getService()))
			throw new EbMSMessageServiceException("Message with messageId " + messageId + " is an EbMS service message!");
		else
		{
			val fromPartyId = messageProperties.getFromParty().getPartyId();
			val toPartyId = messageProperties.getToParty().getPartyId();
			val request = ebMSMessageFactory.createEbMSStatusRequest(messageProperties.getCpaId(),fromPartyId,toPartyId,messageId);
			val response = deliveryManager.sendMessage(request);
			return response.map(r -> (createMessageStatus(r))).orElseThrow(() -> new EbMSMessageServiceException("No response received!"));
		}
	}

	private MessageStatus createMessageStatus(EbMSBaseMessage message)
	{
		if (message instanceof EbMSStatusResponse)
		{
			val timestamp = ((EbMSStatusResponse)message).getStatusResponse().getTimestamp() == null ? null : ((EbMSStatusResponse)message).getStatusResponse().getTimestamp();
			val messageStatus = ((EbMSStatusResponse)message).getStatusResponse().getMessageStatus();
			val status = EbMSMessageStatus.get(messageStatus).orElseThrow(() -> new IllegalStateException("No EbMSMessageStatus found for " + messageStatus));
			return new MessageStatus(timestamp,status);
		}
		else
			throw new EbMSMessageServiceException("No valid response received!");
	}

	public List<MessageEvent> getUnprocessedMessageEvents(MessageFilter messageFilter, EbMSMessageEventType[] eventTypes, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("GetMessageEvents");
			return maxNr == null || maxNr == 0 ? ebMSMessageEventDAO.getEbMSMessageEvents(messageFilter,eventTypes) : ebMSMessageEventDAO.getEbMSMessageEvents(messageFilter,eventTypes,maxNr);
		}
		catch (Exception e)
		{
			log.error("GetMessageEvents" + messageFilter,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	public void processMessageEvent(final String messageId) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("ProcessMessageEvent " + messageId);
			ebMSMessageEventDAO.processEbMSMessageEvent(messageId);
			processMessage(messageId);
		}
		catch (Exception e)
		{
			log.error("ProcessMessageEvent " + messageId,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	private void resetMessage(MessageProperties context)
	{
		// context.setConversationId(null);
		context.setMessageId(null);
		context.setTimestamp(null);
	}

	private void storeMessage(Document document, EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			val timestamp = Instant.now();
			val messageHeader = message.getMessageHeader();
			val service = CPAUtils.toString(messageHeader.getService());
			val sendDeliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction())
							.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
			val receiveDeliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),messageHeader.getTo().getPartyId(),messageHeader.getTo().getRole(),service,messageHeader.getAction())
							.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),messageHeader.getTo().getPartyId(),messageHeader.getTo().getRole(),service,messageHeader.getAction()));
			val persistTime = CPAUtils.getPersistTime(timestamp,receiveDeliveryChannel);
			val confidential = cpaManager.isConfidential(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction());
			ebMSDAO.insertMessage(timestamp,persistTime,document,message,message.getAttachments(),EbMSMessageStatus.CREATED);
			eventManager.createEvent(messageHeader.getCPAId(),sendDeliveryChannel,receiveDeliveryChannel,messageHeader.getMessageData().getMessageId(),messageHeader.getMessageData().getTimeToLive(),messageHeader.getMessageData().getTimestamp(),confidential);
		}
		catch (IllegalStateException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
}
