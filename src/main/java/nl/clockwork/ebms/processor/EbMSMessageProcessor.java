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
import java.util.ArrayList;
import java.util.Date;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.javatuples.Pair;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.DuplicateMessageException;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.validation.XSDValidator;

@Builder(setterPrefix = "set")
@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageProcessor
{
  @NonNull
  DeliveryManager deliveryManager;
  @NonNull
  EventListener eventListener;
  @NonNull
	EbMSDAO ebMSDAO;
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageFactory ebMSMessageFactory;
  @NonNull
	EventManager eventManager;
  @NonNull
	EbMSSignatureGenerator signatureGenerator;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	DuplicateMessageHandler duplicateMessageHandler;
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	XSDValidator xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/msg-header-2_0.xsd");

	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			val timestamp = new Date();
			val message = EbMSMessageUtils.getEbMSMessage(document);
			val cpaId = message.getMessageHeader().getCPAId();
			if (!cpaManager.existsCPA(cpaId))
				throw new EbMSProcessingException("CPA " + cpaId + " not found!");
			return processRequest(timestamp,document,message);
		}
		catch (ValidationException | JAXBException | SAXException | IOException | SOAPException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ValidatorException | XPathExpressionException | ParserConfigurationException | DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private EbMSDocument processRequest(Date timestamp, EbMSDocument document, EbMSBaseMessage message) throws DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, XPathExpressionException
	{
		if (message instanceof EbMSMessage)
			return processMessage(timestamp,document,(EbMSMessage)message);
		else if (message instanceof EbMSMessageError)
		{
			processMessageError(timestamp,document,(EbMSMessageError)message);
			return null;
		}
		else if (message instanceof EbMSAcknowledgment)
		{
			processAcknowledgment(timestamp,document,(EbMSAcknowledgment)message);
			return null;
		}
		else if (message instanceof EbMSStatusRequest)
		{
			return processStatusRequest(timestamp,(EbMSStatusRequest)message);
		}
		else if (message instanceof EbMSStatusResponse)
		{
			processStatusResponse(timestamp,(EbMSStatusResponse)message);
			return null;
		}
		else if (message instanceof EbMSPing)
		{
			return processPing(timestamp,(EbMSPing)message);
		}
		else if (message instanceof EbMSPong)
		{
			processPong(timestamp,(EbMSPong)message);
			return null;
		}
		else
			throw new EbMSProcessingException(
					"Unable to process message! Service=" + message.getMessageHeader().getService() + " and Action=" + message.getMessageHeader().getAction());
	}

	public void processResponse(EbMSDocument request, EbMSDocument response) throws EbMSProcessorException
	{
		try
		{
			val requestMessage = (EbMSMessage)EbMSMessageUtils.getEbMSMessage(request);
			val requestMessageHeader = requestMessage.getMessageHeader();
			if (requestMessage.getAckRequested() != null && requestMessage.getSyncReply() != null && response == null)
				throw new EbMSProcessingException("No response received for message " + requestMessageHeader.getMessageData().getMessageId());
			
			if (response != null)
			{
				xsdValidator.validate(response.getMessage());
				val timestamp = new Date();
				val responseMessage = EbMSMessageUtils.getEbMSMessage(response);
				if (responseMessage instanceof EbMSMessageError)
				{
					if (!messageValidator.isSyncReply(requestMessage))
						throw new EbMSProcessingException(
								"No sync ErrorMessage expected for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
					processMessageError(timestamp,response,requestMessage,(EbMSMessageError)responseMessage);
				}
				else if (responseMessage instanceof EbMSAcknowledgment)
				{
					if (requestMessage.getAckRequested() == null || !messageValidator.isSyncReply(requestMessage))
						throw new EbMSProcessingException(
								"No sync Acknowledgment expected for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
					processAcknowledgment(timestamp,response,requestMessage,(EbMSAcknowledgment)responseMessage);
				}
				else
					throw new EbMSProcessingException(
							"Unexpected response received for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
			}
			else if (requestMessage.getAckRequested() == null && requestMessage.getSyncReply() != null)
			{
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							if (ebMSDAO.updateMessage(
									requestMessageHeader.getMessageData().getMessageId(),
									EbMSMessageStatus.SENDING,
									EbMSMessageStatus.DELIVERED) > 0)
							{
								eventListener.onMessageDelivered(requestMessageHeader.getMessageData().getMessageId());
								if (deleteEbMSAttachmentsOnMessageProcessed)
									ebMSDAO.deleteAttachments(requestMessageHeader.getMessageData().getMessageId());
							}
						}
					}
				);
			}
		}
		catch (ValidationException | JAXBException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ValidatorException | XPathExpressionException | ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	private void processMessageError(Date timestamp, EbMSDocument messageErrorDocument, EbMSMessageError messageError) throws XPathExpressionException, JAXBException, ParserConfigurationException, SAXException, IOException
	{
		MessageHeader messageHeader = messageError.getMessageHeader();
		val request = ebMSDAO.getDocument(messageHeader .getMessageData().getRefToMessageId())
				.orElseThrow(() -> StreamUtils.illegalStateException("Document",messageHeader.getMessageData().getRefToMessageId()));
		val requestMessage = (EbMSMessage)EbMSMessageUtils.getEbMSMessage(request);
		if (requestMessage.getSyncReply() != null)
			throw new EbMSProcessingException(
					"No async ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
		processMessageError(timestamp,messageErrorDocument,requestMessage,messageError);
	}

	private void processMessageError(final Date timestamp, final EbMSDocument messageErrorDocument, final EbMSMessage message, final EbMSMessageError messageError) throws EbMSProcessingException, ValidatorException
	{
		try
		{
			messageValidator.validateMessageError(message,messageError,timestamp);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						val responseMessageHeader = messageError.getMessageHeader();
						val persistTime = ebMSDAO.getPersistTime(responseMessageHeader.getMessageData().getRefToMessageId());
						ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),messageErrorDocument.getMessage(),messageError,new ArrayList<>(),null);
						if (ebMSDAO.updateMessage(
								responseMessageHeader.getMessageData().getRefToMessageId(),
								EbMSMessageStatus.SENDING,
								EbMSMessageStatus.DELIVERY_FAILED) > 0)
						{
							eventListener.onMessageFailed(responseMessageHeader.getMessageData().getRefToMessageId());
							if (deleteEbMSAttachmentsOnMessageProcessed)
								ebMSDAO.deleteAttachments(responseMessageHeader.getMessageData().getRefToMessageId());
						}
					}
				}
			);
		}
		catch (DuplicateMessageException e)
		{
			duplicateMessageHandler.handleMessageError(timestamp,messageErrorDocument,messageError);
		}
		catch (ValidationException e)
		{
			val persistTime = ebMSDAO.getPersistTime(messageError.getMessageHeader().getMessageData().getRefToMessageId());
			ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),messageErrorDocument.getMessage(),messageError,new ArrayList<>(),null);
			log.warn("Unable to process MessageError " + messageError.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	private void processAcknowledgment(Date timestamp, EbMSDocument acknowledgmentDocument, EbMSAcknowledgment acknowledgment) throws XPathExpressionException, JAXBException, ParserConfigurationException, SAXException, IOException
	{
		val request = ebMSDAO.getDocument(acknowledgment.getAcknowledgment().getRefToMessageId())
				.orElseThrow(() -> StreamUtils.illegalStateException("Document",acknowledgment.getAcknowledgment().getRefToMessageId()));
		val requestMessage = (EbMSMessage)EbMSMessageUtils.getEbMSMessage(request);
		if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() != null)
			throw new EbMSProcessingException(
					"No async Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
		processAcknowledgment(timestamp,acknowledgmentDocument,requestMessage,acknowledgment);
	}

	private void processAcknowledgment(final Date timestamp, final EbMSDocument acknowledgmentDocument, final EbMSMessage message, final EbMSAcknowledgment acknowledgment) throws EbMSProcessingException
	{
		try
		{
			messageValidator.validateAcknowledgment(acknowledgmentDocument,message,acknowledgment,timestamp);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						val responseMessageHeader = acknowledgment.getMessageHeader();
						val persistTime = ebMSDAO.getPersistTime(responseMessageHeader.getMessageData().getRefToMessageId());
						ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),acknowledgmentDocument.getMessage(),acknowledgment,new ArrayList<>(),null);
						if (ebMSDAO.updateMessage(
								responseMessageHeader.getMessageData().getRefToMessageId(),
								EbMSMessageStatus.SENDING,
								EbMSMessageStatus.DELIVERED) > 0)
						{
							eventListener.onMessageDelivered(responseMessageHeader.getMessageData().getRefToMessageId());
							if (deleteEbMSAttachmentsOnMessageProcessed)
								ebMSDAO.deleteAttachments(responseMessageHeader.getMessageData().getRefToMessageId());
						}
					}
				}
			);
		}
		catch (DuplicateMessageException e)
		{
			duplicateMessageHandler.handleAcknowledgment(timestamp,acknowledgmentDocument,acknowledgment);
		}
		catch (ValidatorException e)
		{
			val persistTime = ebMSDAO.getPersistTime(acknowledgment.getMessageHeader().getMessageData().getRefToMessageId());
			ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),acknowledgmentDocument.getMessage(),acknowledgment,new ArrayList<>(),null);
			log.warn("Unable to process Acknowledgment " + acknowledgment.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	private EbMSDocument processStatusRequest(Date timestamp, EbMSStatusRequest statusRequest) throws DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val statusResponse = processStatusRequest(statusRequest,timestamp);
		if (messageValidator.isSyncReply(statusRequest))
			return EbMSMessageUtils.getEbMSDocument(statusResponse);
		else
		{
			val messageHeader = statusResponse.getMessageHeader();
			val toPartyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
			val service = CPAUtils.toString(messageHeader.getService());
			val uri = cpaManager.getUri(
					messageHeader.getCPAId(),
					toPartyId,
					messageHeader.getTo().getRole(),
					service,
					messageHeader.getAction());
			deliveryManager.sendResponseMessage(uri,statusResponse);
			return null;
		}
	}
	
	private void processStatusResponse(Date timestamp, EbMSStatusResponse statusResponse)
	{
		try
		{
			messageValidator.validate(statusResponse,timestamp);
			deliveryManager.handleResponseMessage(statusResponse);
		}
		catch (ValidatorException e)
		{
			log.warn("Unable to process StatusResponse " + statusResponse.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	private EbMSDocument processPing(Date timestamp, EbMSPing ping) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val response = processPing(ping,timestamp);
		if (messageValidator.isSyncReply(ping))
			return EbMSMessageUtils.getEbMSDocument(response);
		else
		{
			val responseMessageHeader = response.getMessageHeader();
			val toPartyId = new CacheablePartyId(responseMessageHeader.getTo().getPartyId());
			val service = CPAUtils.toString(responseMessageHeader.getService());
			val uri = cpaManager.getUri(
					responseMessageHeader.getCPAId(),
					toPartyId,
					responseMessageHeader.getTo().getRole(),
					service,
					responseMessageHeader.getAction());
			deliveryManager.sendResponseMessage(uri,response);
			return null;
		}
	}

	private void processPong(Date timestamp, EbMSPong pong)
	{
		try
		{
			messageValidator.validate(pong,timestamp);
			deliveryManager.handleResponseMessage(pong);
		}
		catch (ValidatorException e)
		{
			log.warn("Unable to process Pong " + pong.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	public EbMSDocument processMessage(final Date timestamp, final EbMSDocument messageDocument, final EbMSMessage message) throws DAOException, ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, EbMSProcessorException
	{
		val messageHeader = message.getMessageHeader();
		try
		{
			messageValidator.validateMessage(messageDocument,message,timestamp);
			if (message.getAckRequested() == null)
			{
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp,null,messageDocument.getMessage(),message,message.getAttachments(),EbMSMessageStatus.RECEIVED);
							eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
						}
					}
				);
				return null;
			}
			else
			{
				val acknowledgment = ebMSMessageFactory.createEbMSAcknowledgment(message,timestamp);
				val acknowledgmentDocument = EbMSMessageUtils.getEbMSDocument(acknowledgment);
				signatureGenerator.generate(message.getAckRequested(),acknowledgmentDocument,acknowledgment);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							{
								val toPartyId = new CacheablePartyId(message.getMessageHeader().getTo().getPartyId());
								val service = CPAUtils.toString(message.getMessageHeader().getService());
								val deliveryChannel =
										cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
										.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()));
								val persistTime = CPAUtils.getPersistTime(messageHeader.getMessageData().getTimestamp(),deliveryChannel);
								ebMSDAO.insertMessage(timestamp,persistTime,messageDocument.getMessage(),message,message.getAttachments(),EbMSMessageStatus.RECEIVED);
								ebMSDAO.insertMessage(timestamp,persistTime,acknowledgmentDocument.getMessage(),acknowledgment,new ArrayList<>(),null);
							}
							{
								val fromPartyId = new CacheablePartyId(acknowledgment.getMessageHeader().getFrom().getPartyId());
								val toPartyId = new CacheablePartyId(acknowledgment.getMessageHeader().getTo().getPartyId());
								val service = CPAUtils.toString(acknowledgment.getMessageHeader().getService());
								val sendDeliveryChannel =
										cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),fromPartyId,acknowledgment.getMessageHeader().getFrom().getRole(),service,acknowledgment.getMessageHeader().getAction())
										.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),fromPartyId,acknowledgment.getMessageHeader().getFrom().getRole(),service,acknowledgment.getMessageHeader().getAction()));
								val receiveDeliveryChannel =
										cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,acknowledgment.getMessageHeader().getTo().getRole(),service,acknowledgment.getMessageHeader().getAction())
										.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,acknowledgment.getMessageHeader().getTo().getRole(),service,acknowledgment.getMessageHeader().getAction()));
								if (!messageValidator.isSyncReply(message))
									eventManager.createEvent(
											messageHeader.getCPAId(),
											sendDeliveryChannel,
											receiveDeliveryChannel,
											acknowledgment.getMessageHeader().getMessageData().getMessageId(),
											acknowledgment.getMessageHeader().getMessageData().getTimeToLive(),
											acknowledgment.getMessageHeader().getMessageData().getTimestamp(),
											false);
							}
							eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
						}
					}
				);
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
			val errorList = EbMSMessageUtils.createErrorList();
			errorList.getError().add(e.getError());
			val messageError = ebMSMessageFactory.createEbMSMessageError(message,errorList,timestamp);
			val messageErrorDocument = EbMSMessageUtils.getEbMSDocument(messageError);
			signatureGenerator.generate(message.getAckRequested(),messageErrorDocument,messageError);
			ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							{
								val toPartyId = new CacheablePartyId(message.getMessageHeader().getTo().getPartyId());
								val service = CPAUtils.toString(message.getMessageHeader().getService());
								val deliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
										.orElse(null);
								val persistTime = deliveryChannel != null ? CPAUtils.getPersistTime(timestamp,deliveryChannel) : null;
								ebMSDAO.insertMessage(timestamp,persistTime,messageDocument.getMessage(),message,message.getAttachments(),EbMSMessageStatus.FAILED);
								ebMSDAO.insertMessage(timestamp,persistTime,messageErrorDocument.getMessage(),messageError,new ArrayList<>(),null);
							}
							if (!messageValidator.isSyncReply(message))
							{
								val service = CPAUtils.toString(messageError.getMessageHeader().getService());
								val sendDeliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageError.getMessageHeader().getFrom().getPartyId()),messageError.getMessageHeader().getFrom().getRole(),service,messageError.getMessageHeader().getAction())
										.orElseThrow(() -> new EbMSProcessingException(e.getMessage()));
								val receiveDeliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageError.getMessageHeader().getTo().getPartyId()),messageError.getMessageHeader().getTo().getRole(),service,messageError.getMessageHeader().getAction())
										.orElseThrow(() -> new EbMSProcessingException(e.getMessage()));
								eventManager.createEvent(
										messageHeader.getCPAId(),
										sendDeliveryChannel,
										receiveDeliveryChannel,
										messageError.getMessageHeader().getMessageData().getMessageId(),
										messageError.getMessageHeader().getMessageData().getTimeToLive(),
										messageError.getMessageHeader().getMessageData().getTimestamp(),
										false);
							}
						}
					}
			);
			return messageValidator.isSyncReply(message) ? messageErrorDocument : null;
		}
	}

	protected EbMSStatusResponse processStatusRequest(final EbMSStatusRequest statusRequest, final Date timestamp) throws ValidatorException, DatatypeConfigurationException, JAXBException, EbMSProcessorException
	{
		messageValidator.validate(statusRequest,timestamp);
		val mc = ebMSDAO.getMessageContext(statusRequest.getStatusRequest().getRefToMessageId()).orElse(null);
		val result = createEbMSMessageStatusAndTimestamp(statusRequest,mc);
		return ebMSMessageFactory.createEbMSStatusResponse(statusRequest,result.getValue0(),result.getValue1()); 
	}
	
	private Pair<EbMSMessageStatus,Date> createEbMSMessageStatusAndTimestamp(EbMSStatusRequest statusRequest, EbMSMessageContext messageContext)
	{
		if (messageContext == null || EbMSAction.EBMS_SERVICE_URI.equals(messageContext.getService()))
			return new Pair<EbMSMessageStatus,Date>(EbMSMessageStatus.NOT_RECOGNIZED,null);
		else if (!messageContext.getCpaId().equals(statusRequest.getMessageHeader().getCPAId()))
			return new Pair<EbMSMessageStatus,Date>(EbMSMessageStatus.UNAUTHORIZED,null);
		else
		{
			return ebMSDAO.getMessageStatus(statusRequest.getStatusRequest().getRefToMessageId())
					.map(s -> mapEbMSMessageStatusAndTimestamp(s,messageContext.getTimestamp()))
					.get();
		}
	}

	private Pair<EbMSMessageStatus,Date> mapEbMSMessageStatusAndTimestamp(EbMSMessageStatus status, Date timestamp)
	{
		if (status != null
				&& (MessageStatusType.RECEIVED.equals(status.getStatusCode())
						|| MessageStatusType.PROCESSED.equals(status.getStatusCode())
						|| MessageStatusType.FORWARDED.equals(status.getStatusCode())))
			return new Pair<EbMSMessageStatus,Date>(status,timestamp);
		else
			return new Pair<EbMSMessageStatus,Date>(EbMSMessageStatus.NOT_RECOGNIZED,null);
	}

	protected EbMSPong processPing(EbMSPing message, Date timestamp) throws ValidatorException, EbMSProcessorException
	{
		messageValidator.validate(message,timestamp);
		return ebMSMessageFactory.createEbMSPong(message);
	}
}
