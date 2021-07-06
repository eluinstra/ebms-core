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

import java.io.IOException;
import java.time.Instant;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.MDC;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSMessageResponse;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.util.LoggingUtils;
import nl.clockwork.ebms.util.LoggingUtils.Status;
import nl.clockwork.ebms.validation.DuplicateMessageException;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.validation.XSDValidator;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSMessageProcessor
{
  @NonNull
  EventListener eventListener;
  @NonNull
	EbMSDAO ebMSDAO;
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	DuplicateMessageHandler duplicateMessageHandler;
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	XSDValidator xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/msg-header-2_0.xsd");
	MessageErrorProcessor messageErrorProcessor;
	AcknowledgmentProcessor acknowledgmentProcessor;
	StatusResponseProcessor statusResponseProcessor;
	PongProcessor pongProcessor;

	@Builder
	public EbMSMessageProcessor(@NonNull DeliveryManager deliveryManager, @NonNull EventListener eventListener, @NonNull EbMSDAO ebMSDAO, @NonNull CPAManager cpaManager, @NonNull EbMSMessageFactory ebMSMessageFactory, @NonNull EventManager eventManager, @NonNull EbMSSignatureGenerator signatureGenerator, @NonNull EbMSMessageValidator messageValidator, @NonNull DuplicateMessageHandler duplicateMessageHandler, boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		super();
		this.eventListener = eventListener;
		this.ebMSDAO = ebMSDAO;
		this.cpaManager = cpaManager;
		this.messageValidator = messageValidator;
		this.duplicateMessageHandler = duplicateMessageHandler;
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
		this.messageErrorProcessor = MessageErrorProcessor.builder()
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.eventManager(eventManager)
				.messageValidator(messageValidator)
				.duplicateMessageHandler(duplicateMessageHandler)
				.ebMSMessageFactory(ebMSMessageFactory)
				.signatureGenerator(signatureGenerator)
				.eventListener(eventListener)
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
		this.acknowledgmentProcessor = AcknowledgmentProcessor.builder()
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.eventManager(eventManager)
				.messageValidator(messageValidator)
				.duplicateMessageHandler(duplicateMessageHandler)
				.ebMSMessageFactory(ebMSMessageFactory)
				.signatureGenerator(signatureGenerator)
				.eventListener(eventListener)
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
		this.statusResponseProcessor = StatusResponseProcessor.builder()
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.messageValidator(messageValidator)
				.ebMSMessageFactory(ebMSMessageFactory)
				.deliveryManager(deliveryManager)
				.build();
		this.pongProcessor = PongProcessor.builder()
				.cpaManager(cpaManager)
				.messageValidator(messageValidator)
				.ebMSMessageFactory(ebMSMessageFactory)
				.deliveryManager(deliveryManager)
				.build();
	}

	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			val timestamp = Instant.now();
			val message = EbMSMessageUtils.getEbMSMessage(document);
			if (LoggingUtils.mdc == Status.ENABLED)
				MDC.setContextMap(LoggingUtils.getPropertyMap(message.getMessageHeader()));
			val cpaId = message.getMessageHeader().getCPAId();
			if (!cpaManager.existsCPA(cpaId))
				throw new ValidationException("CPA " + cpaId + " not found!");
			return processRequest(timestamp,document,message);
		}
		catch (JAXBException | SAXException | IOException | SOAPException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (XPathExpressionException | ParserConfigurationException | DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
		finally
		{
			MDC.clear();
		}
	}

	private EbMSDocument processRequest(Instant timestamp, EbMSDocument document, EbMSBaseMessage message) throws DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, XPathExpressionException
	{
		if (message instanceof EbMSMessage)
			return processMessage(timestamp,document,(EbMSMessage)message);
		else if (message instanceof EbMSMessageError)
		{
			val messageError = (EbMSMessageError)message;
			val requestMessage = getRequestMessage(messageError);
			if (requestMessage.getSyncReply() != null)
				throw new EbMSProcessingException("No async ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
			messageErrorProcessor.processMessageError(timestamp,document,requestMessage,messageError);
			return null;
		}
		else if (message instanceof EbMSAcknowledgment)
		{
			val acknowledgment = (EbMSAcknowledgment)message;
			val requestMessage = getRequestMessage(acknowledgment);
			if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() != null)
				throw new EbMSProcessingException("No async Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
			acknowledgmentProcessor.processAcknowledgment(timestamp,document,requestMessage,acknowledgment);
			return null;
		}
		else if (message instanceof EbMSStatusRequest)
		{
			return processStatusRequest(timestamp,(EbMSStatusRequest)message);
		}
		else if (message instanceof EbMSStatusResponse)
		{
			statusResponseProcessor.processStatusResponse(timestamp,(EbMSStatusResponse)message);
			return null;
		}
		else if (message instanceof EbMSPing)
		{
			return processPing(timestamp,(EbMSPing)message);
		}
		else if (message instanceof EbMSPong)
		{
			pongProcessor.processPong(timestamp,(EbMSPong)message);
			return null;
		}
		else
			throw new EbMSProcessingException(
					"Unable to process message!" +
					"\nCPAId=" + message.getMessageHeader().getCPAId() +
					"\nand MessageId=" + message.getMessageHeader().getMessageData().getMessageId() +
					"\nand Service=" + message.getMessageHeader().getService() + 
					"\nand Action=" + message.getMessageHeader().getAction());
	}

	public void processResponse(EbMSDocument request, EbMSDocument response) throws EbMSProcessorException
	{
		try
		{
			val message = EbMSMessageUtils.getEbMSMessage(request);
			val requestMessageHeader = message.getMessageHeader();
			if (message instanceof EbMSMessage)
			{
				val requestMessage = (EbMSMessage)message;
				if (requestMessage.getAckRequested() != null && requestMessage.getSyncReply() != null && response == null)
					throw new EbMSProcessingException("No response received for message " + requestMessageHeader.getMessageData().getMessageId());
				
				if (response != null)
				{
					xsdValidator.validate(response.getMessage());
					val timestamp = Instant.now();
					val responseMessage = EbMSMessageUtils.getEbMSMessage(response);
					if (responseMessage instanceof EbMSMessageError)
					{
						if (!messageValidator.isSyncReply(requestMessage))
							throw new EbMSProcessingException(
									"No sync ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						messageErrorProcessor.processMessageError(timestamp,response,requestMessage,(EbMSMessageError)responseMessage);
					}
					else if (responseMessage instanceof EbMSAcknowledgment)
					{
						if (requestMessage.getAckRequested() == null || !messageValidator.isSyncReply(requestMessage))
							throw new EbMSProcessingException(
									"No sync Acknowledgment expected for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						acknowledgmentProcessor.processAcknowledgment(timestamp,response,requestMessage,(EbMSAcknowledgment)responseMessage);
					}
					else
						throw new EbMSProcessingException(
								"Unexpected response received for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
				}
				else if (requestMessage.getAckRequested() == null && requestMessage.getSyncReply() != null)
				{
					processMessage(requestMessage);
				}
			}
			else if (response != null)
				throw new EbMSProcessingException(
						"Unexpected response received for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
		}
		catch (ValidationException | JAXBException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (XPathExpressionException | ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private EbMSDocument processMessage(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message) throws ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, EbMSProcessorException
	{
		try
		{
			messageValidator.validateAndDecryptMessage(messageDocument,message,timestamp);
			if (message.getAckRequested() == null)
			{
				storeMessage(timestamp,messageDocument,message);
				return null;
			}
			else
			{
				val acknowledgmentDocument = acknowledgmentProcessor.processAcknowledgment(timestamp,messageDocument,message,messageValidator.isSyncReply(message));
				return messageValidator.isSyncReply(message) ? acknowledgmentDocument : null;
			}
		}
		catch (DuplicateMessageException e)
		{
			return duplicateMessageHandler.handleMessage(timestamp,messageDocument,message);
		}
		catch (final EbMSValidationException e)
		{
			log.warn("Message " + message.getMessageHeader().getMessageData().getMessageId() + " invalid.\n" + e.getMessage());
			val messageErrorDocument = messageErrorProcessor.processMessageError(timestamp,messageDocument,message,messageValidator.isSyncReply(message),e);
			return messageValidator.isSyncReply(message) ? messageErrorDocument : null;
		}
	}

	private void storeMessage(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message)
	{
		Runnable runnable = () ->
		{
			ebMSDAO.insertMessage(timestamp,null,messageDocument.getMessage(),message,message.getAttachments(),EbMSMessageStatus.RECEIVED);
			eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
		};
		ebMSDAO.executeTransaction(runnable);
	}

	private void processMessage(final EbMSMessage message)
	{
		val messageHeader = message.getMessageHeader();
		Runnable runnable = () ->
		{
			if (ebMSDAO.updateMessage(
					messageHeader .getMessageData().getMessageId(),
					EbMSMessageStatus.CREATED,
					EbMSMessageStatus.DELIVERED) > 0)
			{
				eventListener.onMessageDelivered(messageHeader.getMessageData().getMessageId());
				if (deleteEbMSAttachmentsOnMessageProcessed)
					ebMSDAO.deleteAttachments(messageHeader.getMessageData().getMessageId());
			}
		};
		ebMSDAO.executeTransaction(runnable);
	}

	private EbMSDocument processStatusRequest(Instant timestamp, EbMSStatusRequest statusRequest) throws DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		messageValidator.validate(statusRequest,timestamp);
		val statusResponse = statusResponseProcessor.createStatusResponse(statusRequest,timestamp);
		if (messageValidator.isSyncReply(statusRequest))
			return EbMSMessageUtils.getEbMSDocument(statusResponse);
		else
		{
			statusResponseProcessor.sendStatusResponse(statusResponse);
			return null;
		}
	}

	private EbMSDocument processPing(Instant timestamp, EbMSPing ping) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		messageValidator.validate(ping,timestamp);
		val pong = pongProcessor.createPong(ping,timestamp);
		if (messageValidator.isSyncReply(ping))
			return EbMSMessageUtils.getEbMSDocument(pong);
		else
		{
			pongProcessor.sendPong(pong);
			return null;
		}
	}

	private EbMSMessage getRequestMessage(EbMSMessageResponse messageResponse) throws EbMSProcessingException
	{
		val request = ebMSDAO.getDocument(messageResponse.getMessageHeader().getMessageData().getRefToMessageId());
		val requestMessage = request.map(r -> {
			try
			{
				return (EbMSMessage)EbMSMessageUtils.getEbMSMessage(r);
			}
			catch (XPathExpressionException | JAXBException | ParserConfigurationException | SAXException | IOException e)
			{
				throw new EbMSProcessingException(e);
			}
		});
		return requestMessage
				.orElseThrow(() -> new EbMSProcessingException("No EbMSMessage found for messageResponse " + messageResponse.getMessageHeader().getMessageData().getMessageId()));
	}
}
