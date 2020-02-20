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
import java.util.Date;
import java.util.Optional;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javatuples.Pair;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.StreamUtils;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.DuplicateMessageException;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.validation.XSDValidator;

public class EbMSMessageProcessor
{
  protected transient Log logger = LogFactory.getLog(getClass());
  protected DeliveryManager deliveryManager;
  protected EventListener eventListener;
	protected EbMSDAO ebMSDAO;
	protected CPAManager cpaManager;
	protected EbMSMessageFactory ebMSMessageFactory;
	protected EventManager eventManager;
	protected EbMSSignatureGenerator signatureGenerator;
	protected XSDValidator xsdValidator;
	protected EbMSMessageValidator messageValidator;
	protected DuplicateMessageHandler duplicateMessageHandler;
	protected boolean deleteEbMSAttachmentsOnMessageProcessed;

	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			Date timestamp = new Date();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document);
			MessageHeader messageHeader = message.getMessageHeader();
			if (!cpaManager.existsCPA(messageHeader.getCPAId()))
				throw new EbMSProcessingException("CPA " + messageHeader.getCPAId() + " not found!");
			if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
			{
				return process(timestamp,message);
			}
			else if (EbMSAction.MESSAGE_ERROR.action().equals(messageHeader.getAction()))
			{
				Document request = ebMSDAO.getDocument(messageHeader.getMessageData().getRefToMessageId())
						.orElseThrow(() -> StreamUtils.illegalStateException("Document",messageHeader.getMessageData().getRefToMessageId()));
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException(
							"No async ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
				processMessageError(messageHeader.getCPAId(),timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.ACKNOWLEDGMENT.action().equals(messageHeader.getAction()))
			{
				Document request = ebMSDAO.getDocument(message.getAcknowledgment().getRefToMessageId())
						.orElseThrow(() -> StreamUtils.illegalStateException("Document",message.getAcknowledgment().getRefToMessageId()));
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException(
							"No async Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
				processAcknowledgment(messageHeader.getCPAId(),timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.STATUS_REQUEST.action().equals(messageHeader.getAction()))
			{
				EbMSMessage response = processStatusRequest(messageHeader.getCPAId(),timestamp,message);
				if (messageValidator.isSyncReply(message))
					return EbMSMessageUtils.getEbMSDocument(response);
				else
				{
					MessageHeader responseMessageHeader = response.getMessageHeader();
					CacheablePartyId toPartyId = new CacheablePartyId(responseMessageHeader.getTo().getPartyId());
					String service = CPAUtils.toString(responseMessageHeader.getService());
					String uri = cpaManager.getUri(
							responseMessageHeader.getCPAId(),
							toPartyId,
							responseMessageHeader.getTo().getRole(),
							service,
							responseMessageHeader.getAction());
					deliveryManager.sendResponseMessage(uri,response);
					return null;
				}
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(messageHeader.getAction()))
			{
				try
				{
					messageValidator.validateStatusResponse(message,timestamp);
					deliveryManager.handleResponseMessage(message);
				}
				catch (ValidatorException e)
				{
					logger.warn("Unable to process StatusResponse " + messageHeader.getMessageData().getMessageId(),e);
				}
				return null;
			}
			else if (EbMSAction.PING.action().equals(messageHeader.getAction()))
			{
				EbMSMessage response = processPing(messageHeader.getCPAId(),timestamp,message);
				if (messageValidator.isSyncReply(message))
					return EbMSMessageUtils.getEbMSDocument(response);
				else
				{
					MessageHeader responseMessageHeader = response.getMessageHeader();
					CacheablePartyId toPartyId = new CacheablePartyId(responseMessageHeader.getTo().getPartyId());
					String service = CPAUtils.toString(responseMessageHeader.getService());
					String uri = cpaManager.getUri(
							responseMessageHeader.getCPAId(),
							toPartyId,
							responseMessageHeader.getTo().getRole(),
							service,
							responseMessageHeader.getAction());
					deliveryManager.sendResponseMessage(uri,response);
					return null;
				}
			}
			else if (EbMSAction.PONG.action().equals(messageHeader.getAction()))
			{
				try
				{
					messageValidator.validatePong(message,timestamp);
					deliveryManager.handleResponseMessage(message);
				}
				catch (ValidatorException e)
				{
					logger.warn("Unable to process Pong " + messageHeader.getMessageData().getMessageId(),e);
				}
				return null;
			}
			else
				throw new EbMSProcessingException(
						"Unable to process message! Service=" + messageHeader.getService() + " and Action=" + messageHeader.getAction());
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
	
	public void processResponse(EbMSDocument request, EbMSDocument response) throws EbMSProcessorException
	{
		try
		{
			final EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
			MessageHeader requestMessageHeader = requestMessage.getMessageHeader();
			if (requestMessage.getAckRequested() != null && requestMessage.getSyncReply() != null && response == null)
				throw new EbMSProcessingException("No response received for message " + requestMessageHeader.getMessageData().getMessageId());
			
			if (response != null)
			{
				xsdValidator.validate(response.getMessage());
				Date timestamp = new Date();
				final EbMSMessage responseMessage = EbMSMessageUtils.getEbMSMessage(response);
				MessageHeader responseMessageHeader = responseMessage.getMessageHeader();
				if (Constants.EBMS_SERVICE_URI.equals(responseMessageHeader.getService().getValue()))
				{
					if (EbMSAction.MESSAGE_ERROR.action().equals(responseMessageHeader.getAction()))
					{
						if (!messageValidator.isSyncReply(requestMessage))
							throw new EbMSProcessingException(
									"No sync ErrorMessage expected for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processMessageError(requestMessageHeader.getCPAId(),timestamp,requestMessage,responseMessage);
					}
					else if (EbMSAction.ACKNOWLEDGMENT.action().equals(responseMessageHeader.getAction()))
					{
						if (requestMessage.getAckRequested() == null || !messageValidator.isSyncReply(requestMessage))
							throw new EbMSProcessingException(
									"No sync Acknowledgment expected for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processAcknowledgment(requestMessageHeader.getCPAId(),timestamp,requestMessage,responseMessage);
					}
					else
						throw new EbMSProcessingException(
								"Unexpected response received for message " + requestMessageHeader.getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
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
	
	private void processMessageError(String cpaId, final Date timestamp, final EbMSMessage requestMessage, final EbMSMessage responseMessage) throws EbMSProcessingException, ValidatorException
	{
		try
		{
			messageValidator.validateMessageError(requestMessage,responseMessage,timestamp);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						MessageHeader responseMessageHeader = responseMessage.getMessageHeader();
						Optional<Date> persistTime = ebMSDAO.getPersistTime(responseMessageHeader.getMessageData().getRefToMessageId());
						ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),responseMessage,null);
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
			duplicateMessageHandler.handleMessageError(timestamp,responseMessage);
		}
		catch (ValidationException e)
		{
			Optional<Date> persistTime = ebMSDAO.getPersistTime(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
			ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),responseMessage,null);
			logger.warn("Unable to process MessageError " + responseMessage.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	private void processAcknowledgment(String cpaId, final Date timestamp, final EbMSMessage requestMessage, final EbMSMessage responseMessage) throws EbMSProcessingException
	{
		try
		{
			messageValidator.validateAcknowledgment(requestMessage,responseMessage,timestamp);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						MessageHeader responseMessageHeader = responseMessage.getMessageHeader();
						Optional<Date> persistTime = ebMSDAO.getPersistTime(responseMessageHeader.getMessageData().getRefToMessageId());
						ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),responseMessage,null);
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
			duplicateMessageHandler.handleAcknowledgment(timestamp,responseMessage);
		}
		catch (ValidatorException e)
		{
			Optional<Date> persistTime = ebMSDAO.getPersistTime(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
			ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),responseMessage,null);
			logger.warn("Unable to process Acknowledgment " + responseMessage.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	protected EbMSDocument process(final Date timestamp, final EbMSMessage message) throws DAOException, ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, EbMSProcessorException
	{
		final MessageHeader messageHeader = message.getMessageHeader();
		try
		{
			messageValidator.validateMessage(message,timestamp);
			if (message.getAckRequested() == null)
			{
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp,null,message,EbMSMessageStatus.RECEIVED);
							eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
						}
					}
				);
				return null;
			}
			else
			{
				final EbMSMessage acknowledgment = ebMSMessageFactory.createEbMSAcknowledgment(messageHeader.getCPAId(),message,timestamp);
				signatureGenerator.generate(message.getAckRequested(),acknowledgment);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							{
								CacheablePartyId toPartyId = new CacheablePartyId(message.getMessageHeader().getTo().getPartyId());
								String service = CPAUtils.toString(message.getMessageHeader().getService());
								DeliveryChannel deliveryChannel =
										cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
										.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()));
								Date persistTime = CPAUtils.getPersistTime(messageHeader.getMessageData().getTimestamp(),deliveryChannel);
								ebMSDAO.insertMessage(timestamp,persistTime,message,EbMSMessageStatus.RECEIVED);
								ebMSDAO.insertMessage(timestamp,persistTime,acknowledgment,null);
							}
							{
								CacheablePartyId toPartyId = new CacheablePartyId(acknowledgment.getMessageHeader().getTo().getPartyId());
								String service = CPAUtils.toString(acknowledgment.getMessageHeader().getService());
								DeliveryChannel deliveryChannel =
										cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,acknowledgment.getMessageHeader().getTo().getRole(),service,acknowledgment.getMessageHeader().getAction())
										.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,acknowledgment.getMessageHeader().getTo().getRole(),service,acknowledgment.getMessageHeader().getAction()));
								if (!messageValidator.isSyncReply(message))
									eventManager.createEvent(
											messageHeader.getCPAId(),
											deliveryChannel,
											acknowledgment.getMessageHeader().getMessageData().getMessageId(),
											acknowledgment.getMessageHeader().getMessageData().getTimeToLive(),
											acknowledgment.getMessageHeader().getMessageData().getTimestamp(),
											false);
							}
							eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
						}
					}
				);
				return messageValidator.isSyncReply(message) ? new EbMSDocument(acknowledgment.getContentId(),acknowledgment.getMessage()) : null;
			}
		}
		catch (DuplicateMessageException e)
		{
			return duplicateMessageHandler.handleMessage(timestamp,message);
		}
		catch (final EbMSValidationException e)
		{
			logger.warn("Message " + message.getMessageHeader().getMessageData().getMessageId() + " invalid.\n" + e.getMessage());
			ErrorList errorList = EbMSMessageUtils.createErrorList();
			errorList.getError().add(e.getError());
			final EbMSMessage messageError = ebMSMessageFactory.createEbMSMessageError(messageHeader.getCPAId(),message,errorList,timestamp);
			Document document = EbMSMessageUtils.createSOAPMessage(messageError);
			messageError.setMessage(document);
			ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							{
								CacheablePartyId toPartyId = new CacheablePartyId(message.getMessageHeader().getTo().getPartyId());
								String service = CPAUtils.toString(message.getMessageHeader().getService());
								final DeliveryChannel deliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
										.orElse(null);
								Date persistTime = deliveryChannel != null ? CPAUtils.getPersistTime(timestamp,deliveryChannel) : null;
								ebMSDAO.insertMessage(timestamp,persistTime,message,EbMSMessageStatus.FAILED);
								ebMSDAO.insertMessage(timestamp,persistTime,messageError,null);
							}
							if (!messageValidator.isSyncReply(message))
							{
								String service = CPAUtils.toString(messageError.getMessageHeader().getService());
								final DeliveryChannel deliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageError.getMessageHeader().getTo().getPartyId()),messageError.getMessageHeader().getTo().getRole(),service,messageError.getMessageHeader().getAction())
										.orElse(null);
								if (deliveryChannel == null)
									throw new EbMSProcessingException(e.getMessage());
								eventManager.createEvent(
										messageHeader.getCPAId(),
										deliveryChannel,
										messageError.getMessageHeader().getMessageData().getMessageId(),
										messageError.getMessageHeader().getMessageData().getTimeToLive(),
										messageError.getMessageHeader().getMessageData().getTimestamp(),
										false);
							}
						}
					}
			);
			return messageValidator.isSyncReply(message) ? new EbMSDocument(messageError.getContentId(),messageError.getMessage()) : null;
		}
	}

	protected EbMSMessage processStatusRequest(String cpaId, final Date timestamp, final EbMSMessage message) throws ValidatorException, DatatypeConfigurationException, JAXBException, EbMSProcessorException
	{
		messageValidator.validateStatusRequest(message,timestamp);
		Pair<EbMSMessageStatus,Date> result = ebMSDAO.getMessageContext(message.getStatusRequest().getRefToMessageId())
				.map(mc -> createEbMSMessageStatus(message,mc))
				.get();
		return ebMSMessageFactory.createEbMSStatusResponse(cpaId,message,result.getValue0(),result.getValue1()); 
	}
	
	private Pair<EbMSMessageStatus,Date> createEbMSMessageStatus(EbMSMessage message, EbMSMessageContext messageContext)
	{
		if (messageContext == null || Constants.EBMS_SERVICE_URI.equals(messageContext.getService()))
			return new Pair<EbMSMessageStatus,Date>(EbMSMessageStatus.NOT_RECOGNIZED,null);
		else if (!messageContext.getCpaId().equals(message.getMessageHeader().getCPAId()))
			return new Pair<EbMSMessageStatus,Date>(EbMSMessageStatus.UNAUTHORIZED,null);
		else
		{
			return ebMSDAO.getMessageStatus(message.getStatusRequest().getRefToMessageId())
					.map(s -> createEbMSMessageStatus(messageContext.getTimestamp(),s))
					.get();
		}
	}

	private Pair<EbMSMessageStatus,Date> createEbMSMessageStatus(Date timestamp, EbMSMessageStatus status)
	{
		if (status != null
				&& (MessageStatusType.RECEIVED.equals(status.statusCode())
						|| MessageStatusType.PROCESSED.equals(status.statusCode())
						|| MessageStatusType.FORWARDED.equals(status.statusCode())))
			return new Pair<EbMSMessageStatus,Date>(status,timestamp);
		else
			return new Pair<EbMSMessageStatus,Date>(EbMSMessageStatus.NOT_RECOGNIZED,null);
	}

	protected EbMSMessage processPing(String cpaId, final Date timestamp, final EbMSMessage message) throws ValidatorException, EbMSProcessorException
	{
		messageValidator.validatePing(message,timestamp);
		return ebMSMessageFactory.createEbMSPong(cpaId,message);
	}
	
	public void setDeliveryManager(DeliveryManager deliveryManager)
	{
		this.deliveryManager = deliveryManager;
	}
	
	public void setEventListener(EventListener eventListener)
	{
		this.eventListener = eventListener;
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setEbMSMessageFactory(EbMSMessageFactory ebMSMessageFactory)
	{
		this.ebMSMessageFactory = ebMSMessageFactory;
	}

	public void setEventManager(EventManager eventManager)
	{
		this.eventManager = eventManager;
	}

	public void setSignatureGenerator(EbMSSignatureGenerator signatureGenerator)
	{
		this.signatureGenerator = signatureGenerator;
	}
	
	public void setXsdValidator(XSDValidator xsdValidator)
	{
		this.xsdValidator = xsdValidator;
	}

	public void setMessageValidator(EbMSMessageValidator messageValidator)
	{
		this.messageValidator = messageValidator;
	}

	public void setDuplicateMessageHandler(DuplicateMessageHandler duplicateMessageHandler)
	{
		this.duplicateMessageHandler = duplicateMessageHandler;
	}

	public void setDeleteEbMSAttachmentsOnMessageProcessed(boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
	}
}
