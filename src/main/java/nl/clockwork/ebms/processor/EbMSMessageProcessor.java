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

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.EventListener;
import nl.clockwork.ebms.job.EventManager;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
	protected EbMSMessageValidator messageValidator;
	protected DuplicateMessageHandler duplicateMessageHandler;
	protected XSDValidator xsdValidator;

	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			Date timestamp = new Date();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document);
			if (!cpaManager.existsCPA(message.getMessageHeader().getCPAId()))
				throw new EbMSProcessingException("CPA " + message.getMessageHeader().getCPAId() + " not found!");
			if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			{
				return process(timestamp,message);
			}
			else if (EbMSAction.MESSAGE_ERROR.action().equals(message.getMessageHeader().getAction()))
			{
				Document request = ebMSDAO.getDocument(message.getMessageHeader().getMessageData().getRefToMessageId());
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException("No async ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
				processMessageError(message.getMessageHeader().getCPAId(),timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.ACKNOWLEDGMENT.action().equals(message.getMessageHeader().getAction()))
			{
				Document request = ebMSDAO.getDocument(message.getAcknowledgment().getRefToMessageId());
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException("No async Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
				processAcknowledgment(message.getMessageHeader().getCPAId(),timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.STATUS_REQUEST.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processStatusRequest(message.getMessageHeader().getCPAId(),timestamp,message);
				if (messageValidator.isSyncReply(message))
					return EbMSMessageUtils.getEbMSDocument(response);
				else
				{
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),new CacheablePartyId(response.getMessageHeader().getTo().getPartyId()),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction()),response);
					return null;
				}
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				try
				{
					messageValidator.validateStatusResponse(message,timestamp);
					deliveryManager.handleResponseMessage(message);
				}
				catch (ValidatorException e)
				{
					logger.warn("Unable to process StatusResponse " + message.getMessageHeader().getMessageData().getMessageId(),e);
				}
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(message.getMessageHeader().getCPAId(),timestamp,message);
				if (messageValidator.isSyncReply(message))
					return EbMSMessageUtils.getEbMSDocument(response);
				else
				{
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),new CacheablePartyId(response.getMessageHeader().getTo().getPartyId()),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction()),response);
					return null;
				}
			}
			else if (EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
			{
				try
				{
					messageValidator.validatePong(message,timestamp);
					deliveryManager.handleResponseMessage(message);
				}
				catch (ValidatorException e)
				{
					logger.warn("Unable to process Pong " + message.getMessageHeader().getMessageData().getMessageId(),e);
				}
				return null;
			}
			else
				throw new EbMSProcessingException("Unable to process message! Service=" + message.getMessageHeader().getService() + " and Action=" + message.getMessageHeader().getAction());
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
			if (requestMessage.getAckRequested() != null && requestMessage.getSyncReply() != null && response == null)
				throw new EbMSProcessingException("No response received for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
			
			if (response != null)
			{
				xsdValidator.validate(response.getMessage());
				Date timestamp = new Date();
				final EbMSMessage responseMessage = EbMSMessageUtils.getEbMSMessage(response);
				if (Constants.EBMS_SERVICE_URI.equals(responseMessage.getMessageHeader().getService().getValue()))
				{
					if (EbMSAction.MESSAGE_ERROR.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (!messageValidator.isSyncReply(requestMessage))
							throw new EbMSProcessingException("No sync ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processMessageError(requestMessage.getMessageHeader().getCPAId(),timestamp,requestMessage,responseMessage);
					}
					else if (EbMSAction.ACKNOWLEDGMENT.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (requestMessage.getAckRequested() == null || !messageValidator.isSyncReply(requestMessage))
							throw new EbMSProcessingException("No sync Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processAcknowledgment(requestMessage.getMessageHeader().getCPAId(),timestamp,requestMessage,responseMessage);
					}
					else
						throw new EbMSProcessingException("Unexpected response received for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
				}
				else
					throw new EbMSProcessingException("Unexpected response received for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
			}
			else if (requestMessage.getAckRequested() == null && requestMessage.getSyncReply() != null)
			{
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.updateMessage(requestMessage.getMessageHeader().getMessageData().getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERED);
							eventListener.onMessageAcknowledged(requestMessage.getMessageHeader().getMessageData().getMessageId());
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
						ebMSDAO.insertMessage(timestamp,responseMessage,null);
						ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERY_FAILED);
						eventListener.onMessageFailed(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
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
			ebMSDAO.insertMessage(timestamp,responseMessage,null);
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
						ebMSDAO.insertMessage(timestamp,responseMessage,null);
						if (ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERED) > 0)
						{
							eventListener.onMessageAcknowledged(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
							if (!Constants.ENABLE_HARDENED_ORDERING)
								if (requestMessage.getMessageOrder() != null)
								{
									EbMSMessageContext nextMessage = ebMSDAO.getNextOrderedMessageContext(requestMessage.getMessageHeader().getMessageData().getMessageId());
									if (nextMessage != null)
										if (ebMSDAO.updateMessage(nextMessage.getMessageId(),EbMSMessageStatus.PENDING,EbMSMessageStatus.SENDING) > 0)
											eventManager.createEvent(nextMessage.getCpaId(),cpaManager.getReceiveDeliveryChannel(nextMessage.getCpaId(),new CacheablePartyId(nextMessage.getToRole().getPartyId()),nextMessage.getToRole().getRole(),nextMessage.getService(),nextMessage.getAction()),nextMessage.getMessageId(),nextMessage.getTimeToLive(),nextMessage.getTimestamp(),cpaManager.isConfidential(nextMessage.getCpaId(),new CacheablePartyId(nextMessage.getFromRole().getPartyId()),nextMessage.getFromRole().getRole(),nextMessage.getService(),nextMessage.getAction()),nextMessage.getSequenceNr() != null);
								}
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
			ebMSDAO.insertMessage(timestamp,responseMessage,null);
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
							ebMSDAO.insertMessage(timestamp,message,EbMSMessageStatus.RECEIVED);
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
							ebMSDAO.insertMessage(timestamp,message,EbMSMessageStatus.RECEIVED);
							ebMSDAO.insertMessage(timestamp,acknowledgment,null);
							if (!messageValidator.isSyncReply(message))
								eventManager.createEvent(messageHeader.getCPAId(),cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(acknowledgment.getMessageHeader().getTo().getPartyId()),acknowledgment.getMessageHeader().getTo().getRole(),CPAUtils.toString(acknowledgment.getMessageHeader().getService()),acknowledgment.getMessageHeader().getAction()),acknowledgment.getMessageHeader().getMessageData().getMessageId(),acknowledgment.getMessageHeader().getMessageData().getTimeToLive(),acknowledgment.getMessageHeader().getMessageData().getTimestamp(),false,false);
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
						ebMSDAO.insertMessage(timestamp,message,EbMSMessageStatus.FAILED);
						ebMSDAO.insertMessage(timestamp,messageError,null);
						if (!messageValidator.isSyncReply(message))
							eventManager.createEvent(messageHeader.getCPAId(),cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageError.getMessageHeader().getTo().getPartyId()),messageError.getMessageHeader().getTo().getRole(),CPAUtils.toString(messageError.getMessageHeader().getService()),messageError.getMessageHeader().getAction()),messageError.getMessageHeader().getMessageData().getMessageId(),messageError.getMessageHeader().getMessageData().getTimeToLive(),messageError.getMessageHeader().getMessageData().getTimestamp(),false,false);
					}
				}
			);
			return messageValidator.isSyncReply(message) ? new EbMSDocument(messageError.getContentId(),messageError.getMessage()) : null;
		}
	}

	protected EbMSMessage processStatusRequest(String cpaId, final Date timestamp, final EbMSMessage message) throws ValidatorException, DatatypeConfigurationException, JAXBException, EbMSProcessorException
	{
		messageValidator.validateStatusRequest(message,timestamp);
		Date date = null;
		EbMSMessageStatus status = EbMSMessageStatus.UNAUTHORIZED;
		EbMSMessageContext context = ebMSDAO.getMessageContext(message.getStatusRequest().getRefToMessageId());
		if (context == null || Constants.EBMS_SERVICE_URI.equals(context.getService()))
			status = EbMSMessageStatus.NOT_RECOGNIZED;
		else if (!context.getCpaId().equals(message.getMessageHeader().getCPAId()))
			status = EbMSMessageStatus.UNAUTHORIZED;
		else
		{
			status = ebMSDAO.getMessageStatus(message.getStatusRequest().getRefToMessageId());
			if (status != null && (MessageStatusType.RECEIVED.equals(status.statusCode()) || MessageStatusType.PROCESSED.equals(status.statusCode()) || MessageStatusType.FORWARDED.equals(status.statusCode())))
				date = context.getTimestamp();
			else
				status = EbMSMessageStatus.NOT_RECOGNIZED;
		}
		return ebMSMessageFactory.createEbMSStatusResponse(cpaId,message,status,date); 
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
}
