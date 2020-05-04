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
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageAttachment;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSMessageEvent;
import nl.clockwork.ebms.model.MessageStatus;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.EbMSMessageContextValidator;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

@Builder(setterPrefix = "set")
@CommonsLog
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@AllArgsConstructor
@SuppressWarnings("deprecation")
public class EbMSMessageServiceImpl implements EbMSMessageService
{
  @NonNull
	DeliveryManager deliveryManager;
  @NonNull
	EbMSDAO ebMSDAO;
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageFactory ebMSMessageFactory;
  @NonNull
	EventManager eventManager;
  @NonNull
	EbMSMessageContextValidator ebMSMessageContextValidator;
  @NonNull
	EbMSSignatureGenerator signatureGenerator;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Override
	public void ping(String cpaId, Party fromParty, Party toParty) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(cpaId,fromParty,toParty);
			val request = ebMSMessageFactory.createEbMSPing(cpaId,fromParty,toParty);
			val response = deliveryManager.sendMessage(request);
			if (response.isPresent())
			{
				if (!EbMSAction.PONG.getAction().equals(response.get().getMessageHeader().getAction()))
					throw new EbMSMessageServiceException("No valid response received!");
			}
			else
				throw new EbMSMessageServiceException("No response received!");
		}
		catch (ValidationException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String sendMessage(EbMSMessageContent messageContent) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(messageContent.getContext());
			val message = ebMSMessageFactory.createEbMSMessage(messageContent);
			val document = EbMSMessageUtils.getEbMSDocument(message);
			signatureGenerator.generate(document,message);
			storeMessage(document.getMessage(),message);
			val messageId = message.getMessageHeader().getMessageData().getMessageId();
			log.info("Sending message " + messageId);
			return messageId;
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException | SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String sendMessageWithAttachments(EbMSMessageAttachment message) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(message.getContext());
			val result = ebMSMessageFactory.createEbMSMessage(message.toContent());
			val document = EbMSMessageUtils.getEbMSDocument(result);
			signatureGenerator.generate(document,result);
			storeMessage(document.getMessage(),result);
			val messageId = result.getMessageHeader().getMessageData().getMessageId();
			log.info("Sending message " + messageId);
			return messageId;
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException | SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			return ebMSDAO.getMessageContent(messageId).map(mc ->
			{
				try
				{
					resetMessage(mc.getContext());
					val message = ebMSMessageFactory.createEbMSMessage(mc);
					val document = EbMSMessageUtils.getEbMSDocument(message);
					signatureGenerator.generate(document,message);
					storeMessage(document.getMessage(),message);
					val newMessageId = message.getMessageHeader().getMessageData().getMessageId();
					log.info("Sending message " + newMessageId);
					return newMessageId;
				}
				catch (SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerFactoryConfigurationError | TransformerException e)
				{
					throw new EbMSProcessorException(e);
				}
			}).orElseThrow(() -> new EbMSMessageServiceException("Message not found!"));
		}
		catch (DAOException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			if (maxNr == null || maxNr == 0)
				return ebMSDAO.getMessageIds(messageContext,EbMSMessageStatus.RECEIVED);
			else
				return ebMSDAO.getMessageIds(messageContext,EbMSMessageStatus.RECEIVED,maxNr);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public EbMSMessageContent getMessage(final String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			if (process != null && process)
				processMessage(messageId);
			return ebMSDAO.getMessageContent(messageId).orElse(null);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessage(final String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.executeTransaction(new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction() throws DAOException
				{
					if (ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED) > 0)
					{
						if (deleteEbMSAttachmentsOnMessageProcessed)
							ebMSDAO.deleteAttachments(messageId);
						log.info("Message " + messageId + " processed");
					}
				}
			});
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			return ebMSDAO.getMessageContext(messageId).map(mc -> getMessageStatus(messageId,mc)).orElseThrow(() -> new EbMSMessageServiceException("No message found with messageId " + messageId + "!"));
		}
		catch (EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	private MessageStatus getMessageStatus(String messageId, EbMSMessageContext messageContext) throws EbMSProcessorException
	{
		if (Constants.EBMS_SERVICE_URI.equals(messageContext.getService()))
			throw new EbMSMessageServiceException("Message with messageId " + messageId + " is an EbMS service message!");
		else
		{
			val fromParty = cpaManager.getFromParty(messageContext.getCpaId(),messageContext.getFromRole(),messageContext.getService(),messageContext.getAction()).orElseThrow(() -> StreamUtils.illegalStateException("FromParty",messageContext.getCpaId(),messageContext.getFromRole(),messageContext.getService(),messageContext.getAction()));
			val toParty = cpaManager.getToParty(messageContext.getCpaId(),messageContext.getToRole(),messageContext.getService(),messageContext.getAction()).orElseThrow(() -> StreamUtils.illegalStateException("ToParty",messageContext.getCpaId(),messageContext.getToRole(),messageContext.getService(),messageContext.getAction()));
			val request = ebMSMessageFactory.createEbMSStatusRequest(messageContext.getCpaId(),fromParty,toParty,messageId);
			val response = deliveryManager.sendMessage(request);
			return response.map(r -> (createMessageStatus(r))).orElseThrow(() -> new EbMSMessageServiceException("No response received!"));
		}
	}

	private MessageStatus createMessageStatus(EbMSMessage message)
	{
		if (EbMSAction.STATUS_RESPONSE.getAction().equals(message.getMessageHeader().getAction()) && message.getStatusResponse() != null)
		{
			val timestamp = message.getStatusResponse().getTimestamp() == null ? null : message.getStatusResponse().getTimestamp();
			val status = EbMSMessageStatus.get(message.getStatusResponse().getMessageStatus());
			return new MessageStatus(timestamp,status);
		}
		else
			throw new EbMSMessageServiceException("No valid response received!");
	}

	@Override
	public MessageStatus getMessageStatus(String cpaId, Party fromParty, Party toParty, String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(cpaId,fromParty,toParty);
			val request = ebMSMessageFactory.createEbMSStatusRequest(cpaId,fromParty,toParty,messageId);
			val response = deliveryManager.sendMessage(request);
			return response.map(r -> createMessageStatus(r)).orElseThrow(() -> new EbMSMessageServiceException("No response received!"));
		}
		catch (ValidationException | DAOException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public List<EbMSMessageEvent> getMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] eventTypes, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			if (maxNr == null || maxNr == 0)
				return ebMSDAO.getEbMSMessageEvents(messageContext,eventTypes);
			else
				return ebMSDAO.getEbMSMessageEvents(messageContext,eventTypes,maxNr);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessageEvent(final String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.executeTransaction(new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction() throws DAOException
				{
					ebMSDAO.processEbMSMessageEvent(messageId);
					processMessage(messageId);
				}
			});
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	private void resetMessage(EbMSMessageContext context)
	{
		// context.setConversationId(null);
		context.setMessageId(null);
		context.setTimestamp(null);
	}

	void storeMessage(Document document, EbMSMessage message) throws EbMSProcessorException
	{
		ebMSDAO.executeTransaction(new DAOTransactionCallback()
		{
			@Override
			public void doInTransaction()
			{
				try
				{
					val timestamp = new Date();
					val messageHeader = message.getMessageHeader();
					val fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
					val toPartyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
					val service = CPAUtils.toString(messageHeader.getService());
					val sendDeliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()).orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
					val receiveDeliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()).orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()));
					val persistTime = CPAUtils.getPersistTime(timestamp,receiveDeliveryChannel);
					val confidential = cpaManager.isConfidential(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction());
					ebMSDAO.insertMessage(timestamp,persistTime,document,message,EbMSMessageStatus.SENDING);
					eventManager.createEvent(messageHeader.getCPAId(),sendDeliveryChannel,receiveDeliveryChannel,messageHeader.getMessageData().getMessageId(),messageHeader.getMessageData().getTimeToLive(),messageHeader.getMessageData().getTimestamp(),confidential);
				}
				catch (IllegalStateException | DAOException | TransformerFactoryConfigurationError e)
				{
					throw new EbMSProcessorException();
				}
			}
		});
	}
}
