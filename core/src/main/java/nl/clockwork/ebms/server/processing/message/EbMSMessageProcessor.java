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
package nl.clockwork.ebms.server.processing.message;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.SOAPException;
import java.io.IOException;
import java.time.Instant;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.dao.EbMSDAO;
import nl.clockwork.ebms.common.event.MessageEventListener;
import nl.clockwork.ebms.common.message.EbMSMessageUtils;
import nl.clockwork.ebms.common.model.EbMSAcknowledgment;
import nl.clockwork.ebms.common.model.EbMSDocument;
import nl.clockwork.ebms.common.model.EbMSMessage;
import nl.clockwork.ebms.common.model.EbMSMessageError;
import nl.clockwork.ebms.common.model.EbMSMessageResponse;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.EbMSValidationException;
import nl.clockwork.ebms.common.util.LoggingUtils;
import nl.clockwork.ebms.common.util.LoggingUtils.Status;
import nl.clockwork.ebms.common.util.ValidationException;
import nl.clockwork.ebms.common.util.ValidatorException;
import nl.clockwork.ebms.common.util.XSDValidator;
import nl.clockwork.ebms.server.processing.EbMSProcessingException;
import nl.clockwork.ebms.server.processing.EbMSProcessorException;
import nl.clockwork.ebms.server.processing.acknowledgment.AcknowledgmentProcessor;
import nl.clockwork.ebms.server.processing.duplicate.DuplicateMessageHandler;
import nl.clockwork.ebms.server.processing.error.MessageErrorProcessor;
import nl.clockwork.ebms.server.validation.DuplicateMessageException;
import nl.clockwork.ebms.server.validation.EbMSMessageValidator;
import org.slf4j.MDC;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSMessageProcessor
{
	@NonNull
	MessageEventListener messageEventListener;
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
	@NonNull
	MessageErrorProcessor messageErrorProcessor;
	@NonNull
	AcknowledgmentProcessor acknowledgmentProcessor;

	@Builder
	public EbMSMessageProcessor(
			@NonNull MessageEventListener messageEventListener,
			@NonNull EbMSDAO ebMSDAO,
			@NonNull CPAManager cpaManager,
			@NonNull EbMSMessageValidator messageValidator,
			@NonNull DuplicateMessageHandler duplicateMessageHandler,
			boolean deleteEbMSAttachmentsOnMessageProcessed,
			MessageErrorProcessor messageErrorProcessor,
			AcknowledgmentProcessor acknowledgmentProcessor)
	{
		super();
		this.messageEventListener = messageEventListener;
		this.ebMSDAO = ebMSDAO;
		this.cpaManager = cpaManager;
		this.messageValidator = messageValidator;
		this.duplicateMessageHandler = duplicateMessageHandler;
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
		this.messageErrorProcessor = messageErrorProcessor;
		this.acknowledgmentProcessor = acknowledgmentProcessor;
	}

	public EbMSDocument processRequest(Instant timestamp, EbMSDocument document, EbMSMessage message)
			throws ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException,
			TransformerFactoryConfigurationError, TransformerException, XPathExpressionException, EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			if (LoggingUtils.mdc == Status.ENABLED)
				LoggingUtils.getPropertyMap(message.getMessageHeader()).forEach(MDC::put);
			val cpaId = message.getMessageHeader().getCPAId();
			if (!cpaManager.existsCPA(cpaId))
				throw new ValidationException("CPA " + cpaId + " not found!");
			return processMessage(timestamp, document, message);
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
			if (LoggingUtils.mdc == Status.ENABLED)
				LoggingUtils.getProperties().forEach(MDC::remove);
		}
	}

	public void processResponse(EbMSDocument request, EbMSDocument response) throws EbMSProcessorException
	{
		try
		{
			val message = EbMSMessageUtils.getEbMSMessage(request);
			val requestMessageHeader = message.getMessageHeader();
			if (!(message instanceof EbMSMessage requestMessage))
			{
				if (response != null)
					throwUnexpectedResponse(requestMessageHeader.getMessageData().getMessageId());
				return;
			}

			validateExpectedResponse(requestMessage, requestMessageHeader.getMessageData().getMessageId(), response);
			if (response == null)
			{
				handleNoResponse(requestMessage);
				return;
			}

			processSyncResponse(requestMessage, response);
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

	private void validateExpectedResponse(EbMSMessage requestMessage, String messageId, EbMSDocument response)
	{
		if (requestMessage.getAckRequested() != null && requestMessage.getSyncReply() != null && response == null)
			throw new EbMSProcessingException("No response received for message " + messageId);
	}

	private void handleNoResponse(EbMSMessage requestMessage)
	{
		if (requestMessage.getAckRequested() == null && requestMessage.getSyncReply() != null)
			processMessage(requestMessage);
	}

	private void processSyncResponse(EbMSMessage requestMessage, EbMSDocument response)
			throws ValidationException, JAXBException, SAXException, IOException, TransformerException, XPathExpressionException, ParserConfigurationException
	{
		xsdValidator.validate(response.getMessage());
		val timestamp = Instant.now();
		val responseMessage = EbMSMessageUtils.getEbMSMessage(response);
		if (responseMessage instanceof EbMSMessageError messageError)
		{
			processSyncMessageErrorResponse(requestMessage, response, timestamp, messageError);
			return;
		}
		if (responseMessage instanceof EbMSAcknowledgment acknowledgment)
		{
			processSyncAcknowledgmentResponse(requestMessage, response, timestamp, acknowledgment);
			return;
		}

		throwUnexpectedResponse(requestMessage.getMessageHeader().getMessageData().getMessageId());
	}

	private void processSyncMessageErrorResponse(EbMSMessage requestMessage, EbMSDocument response, Instant timestamp, EbMSMessageError messageError)
			throws TransformerException
	{
		if (!messageValidator.isSyncReply(requestMessage))
			throw new EbMSProcessingException("No sync ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
		messageErrorProcessor.processMessageError(timestamp, response, requestMessage, messageError);
	}

	private void processSyncAcknowledgmentResponse(EbMSMessage requestMessage, EbMSDocument response, Instant timestamp, EbMSAcknowledgment acknowledgment)
			throws XPathExpressionException, JAXBException, ParserConfigurationException, SAXException, IOException
	{
		if (requestMessage.getAckRequested() == null || !messageValidator.isSyncReply(requestMessage))
			throw new EbMSProcessingException("No sync Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
		acknowledgmentProcessor.processAcknowledgment(timestamp, response, requestMessage, acknowledgment);
	}

	private void throwUnexpectedResponse(String messageId)
	{
		throw new EbMSProcessingException("Unexpected response received for message " + messageId);
	}

	private EbMSDocument processMessage(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message)
			throws ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException,
			TransformerFactoryConfigurationError, TransformerException, XPathExpressionException, EbMSProcessorException
	{
		try
		{
			messageValidator.validateAndDecryptMessage(messageDocument, message, timestamp);
			if (message.getAckRequested() == null)
			{
				storeMessage(timestamp, messageDocument, message);
				return null;
			}
			else
			{
				boolean syncReply = messageValidator.isSyncReply(message);
				val acknowledgmentDocument = acknowledgmentProcessor.processAcknowledgment(timestamp, messageDocument, message, syncReply);
				return syncReply ? acknowledgmentDocument : null;
			}
		}
		catch (DuplicateMessageException e)
		{
			return duplicateMessageHandler.handleMessage(message);
		}
		catch (final EbMSValidationException e)
		{
			log.warn("Invalid message " + message.getMessageHeader().getMessageData().getMessageId() + "\n" + e.getMessage());
			boolean syncReply = messageValidator.isSyncReply(message);
			val messageErrorDocument = messageErrorProcessor.processMessageError(timestamp, messageDocument, message, syncReply, e);
			return syncReply ? messageErrorDocument : null;
		}
	}

	private void storeMessage(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message)
	{
		Runnable storeMessage = () ->
		{
			ebMSDAO.insertMessage(timestamp, null, messageDocument.getMessage(), message, message.getAttachments(), EbMSMessageStatus.RECEIVED);
			messageEventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
		};
		ebMSDAO.executeTransaction(storeMessage);
	}

	private void processMessage(final EbMSMessage message)
	{
		val messageHeader = message.getMessageHeader();
		Runnable updateMessage = () ->
		{
			if (ebMSDAO.updateMessage(messageHeader.getMessageData().getMessageId(), EbMSMessageStatus.CREATED, EbMSMessageStatus.DELIVERED) > 0)
			{
				messageEventListener.onMessageDelivered(messageHeader.getMessageData().getMessageId());
				if (deleteEbMSAttachmentsOnMessageProcessed)
					ebMSDAO.deleteAttachments(messageHeader.getMessageData().getMessageId());
			}
		};
		ebMSDAO.executeTransaction(updateMessage);
	}

	private EbMSMessage getRequestMessage(EbMSMessageResponse messageResponse) throws EbMSProcessingException
	{
		val request = ebMSDAO.getDocument(messageResponse.getMessageHeader().getMessageData().getRefToMessageId());
		val requestMessage = request.map(r ->
		{
			try
			{
				return (EbMSMessage)EbMSMessageUtils.getEbMSMessage(r);
			}
			catch (XPathExpressionException | JAXBException | ParserConfigurationException | SAXException | IOException e)
			{
				throw new EbMSProcessingException(e);
			}
		});
		return requestMessage.orElseThrow(
				() -> new EbMSProcessingException("No EbMSMessage found for messageResponse " + messageResponse.getMessageHeader().getMessageData().getMessageId()));
	}
}
