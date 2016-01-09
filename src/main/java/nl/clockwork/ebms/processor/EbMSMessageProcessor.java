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
import nl.clockwork.ebms.Constants.EbMSEventType;
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
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.signature.EbMSSignatureGenerator;
import nl.clockwork.ebms.signature.EbMSSignatureValidator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SignatureTypeValidator;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.validation.XSDValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PerMessageCharacteristicsType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
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
	protected EbMSSignatureValidator signatureValidator;
  protected XSDValidator xsdValidator;
  protected CPAValidator cpaValidator;
  protected MessageHeaderValidator messageHeaderValidator;
  protected ManifestValidator manifestValidator;
  protected SignatureTypeValidator signatureTypeValidator;
  protected Service mshMessageService;

  public EbMSMessageProcessor()
	{
		signatureGenerator = new EbMSSignatureGenerator()
		{
			@Override
			public void generate(String cpaId, AckRequested ackRequested, EbMSMessage message) throws EbMSProcessorException
			{
			}
		};
		mshMessageService = new Service();
		mshMessageService.setValue(Constants.EBMS_SERVICE_URI);
	}

	public void init()
	{
		xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/msg-header-2_0.xsd");
		cpaValidator = new CPAValidator(cpaManager);
		messageHeaderValidator = new MessageHeaderValidator(ebMSDAO,cpaManager);
		messageHeaderValidator.setAckSignatureRequested(PerMessageCharacteristicsType.NEVER);
		manifestValidator = new ManifestValidator();
		signatureTypeValidator = new SignatureTypeValidator(cpaManager,signatureValidator);
	}
	
	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			Date timestamp = new Date();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document);
			if (!cpaManager.existsCPA(message.getMessageHeader().getCPAId()))
			{
				logger.warn("CPA " + message.getMessageHeader().getCPAId() + " not found!");
				return null;
			}
			if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			{
				return process(message.getMessageHeader().getCPAId(),timestamp,message);
			}
			else if (EbMSAction.MESSAGE_ERROR.action().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message,EbMSMessageStatus.DELIVERY_FAILED);
				return null;
			}
			else if (EbMSAction.ACKNOWLEDGMENT.action().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message,EbMSMessageStatus.DELIVERED);
				return null;
			}
			else if (EbMSAction.STATUS_REQUEST.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processStatusRequest(message.getMessageHeader().getCPAId(),timestamp,message);
				if (message.getSyncReply() == null)
				{
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),cpaManager.getToDeliveryChannel(response.getMessageHeader().getCPAId(),new CacheablePartyId(response.getMessageHeader().getTo().getPartyId()),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction())),response);
					return null;
				}
				else
					return EbMSMessageUtils.getEbMSDocument(response);
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				deliveryManager.handleResponseMessage(message);
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(message.getMessageHeader().getCPAId(),timestamp,message);
				if (message.getSyncReply() == null)
				{
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),cpaManager.getToDeliveryChannel(response.getMessageHeader().getCPAId(),new CacheablePartyId(response.getMessageHeader().getTo().getPartyId()),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction())),response);
					return null;
				}
				else
					return EbMSMessageUtils.getEbMSDocument(response);
			}
			else if (EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
			{
				deliveryManager.handleResponseMessage(message);
				return null;
			}
			else
				throw new EbMSProcessingException("Unable to process message! Action=" + message.getMessageHeader().getAction());
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
						process(timestamp,responseMessage,EbMSMessageStatus.DELIVERY_FAILED);
					else if (EbMSAction.ACKNOWLEDGMENT.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() == null)
							throw new EbMSProcessingException("No response expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						process(timestamp,responseMessage,EbMSMessageStatus.DELIVERED);
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
							ebMSDAO.updateMessage(requestMessage.getMessageHeader().getMessageData().getMessageId(),EbMSMessageStatus.SENT,EbMSMessageStatus.DELIVERED);
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
	
	protected EbMSDocument process(final String cpaId, final Date timestamp, final EbMSMessage message) throws DAOException, ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, EbMSProcessorException
	{
		final MessageHeader messageHeader = message.getMessageHeader();
		if (isDuplicateMessage(message))
		{
			logger.warn("Message " + message.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
			if (message.getSyncReply() == null)
			{
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertDuplicateMessage(timestamp,message);
							EbMSMessageContext messageContext = ebMSDAO.getMessageContextByRefToMessageId(messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
							ebMSDAO.insertEvent(messageContext.getMessageId(),EbMSEventType.SEND,cpaManager.getUri(cpaId,cpaManager.getToDeliveryChannel(cpaId,new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(CPAUtils.createEbMSMessageService()),null)));
						}
					}
				);
				return null;
			}
			else
			{
				ebMSDAO.insertDuplicateMessage(timestamp,message);
				return ebMSDAO.getEbMSDocumentByRefToMessageId(messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
			}
		}
		else
		{
			try
			{
				cpaValidator.validate(cpaId,message);
				messageHeaderValidator.validate(cpaId,message,timestamp);
				signatureTypeValidator.validate(cpaId,message);
				manifestValidator.validate(message);
				signatureTypeValidator.validateSignature(cpaId,message);
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
					final EbMSMessage acknowledgment = ebMSMessageFactory.createEbMSAcknowledgment(cpaId,message,timestamp);
					acknowledgment.setMessage(EbMSMessageUtils.createSOAPMessage(acknowledgment));
					signatureGenerator.generate(cpaId,message.getAckRequested(),acknowledgment);
					ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertMessage(timestamp,message,EbMSMessageStatus.RECEIVED);
								ebMSDAO.insertMessage(timestamp,acknowledgment,null);
								if (message.getSyncReply() == null)
									ebMSDAO.insertEvent(eventManager.createEbMSSendEvent(acknowledgment,cpaManager.getUri(cpaId,cpaManager.getToDeliveryChannel(cpaId,new CacheablePartyId(acknowledgment.getMessageHeader().getTo().getPartyId()),acknowledgment.getMessageHeader().getTo().getRole(),CPAUtils.toString(acknowledgment.getMessageHeader().getService()),acknowledgment.getMessageHeader().getAction()))));
								eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
							}
						}
					);
					return message.getSyncReply() == null ? null : new EbMSDocument(acknowledgment.getMessage());
				}
			}
			catch (EbMSValidationException e)
			{
				logger.warn("Message " + message.getMessageHeader().getMessageData().getMessageId() + " invalid.\n" + e.getMessage());
				ErrorList errorList = EbMSMessageUtils.createErrorList();
				errorList.getError().add(e.getError());
				final EbMSMessage messageError = ebMSMessageFactory.createEbMSMessageError(cpaId,message,errorList,timestamp);
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
							if (message.getSyncReply() == null)
								ebMSDAO.insertEvent(eventManager.createEbMSSendEvent(messageError,cpaManager.getUri(cpaId,cpaManager.getToDeliveryChannel(cpaId,new CacheablePartyId(messageError.getMessageHeader().getTo().getPartyId()),messageError.getMessageHeader().getTo().getRole(),CPAUtils.toString(messageError.getMessageHeader().getService()),messageError.getMessageHeader().getAction()))));
						}
					}
				);
				return message.getSyncReply() == null ? null : new EbMSDocument(messageError.getMessage());
			}
		}
	}

	protected void process(final Date timestamp, final EbMSMessage responseMessage, final EbMSMessageStatus status)
	{
		if (isDuplicateMessage(responseMessage))
			ebMSDAO.insertDuplicateMessage(timestamp,responseMessage);
		else
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.insertMessage(timestamp,responseMessage,null);
						//ebMSDAO.deleteEvents(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSEventStatus.UNPROCESSED);
						ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENT,status);
						if (status.equals(EbMSMessageStatus.DELIVERED))
							eventListener.onMessageAcknowledged(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
						else if (status.equals(EbMSMessageStatus.DELIVERY_FAILED))
							eventListener.onMessageFailed(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
					}
				}
			);
	}
	
	protected EbMSMessage processStatusRequest(String cpaId, final Date timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException, EbMSValidationException
	{
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
	
	protected EbMSMessage processPing(String cpaId, final Date timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		return ebMSMessageFactory.createEbMSPong(cpaId,message);
	}
	
	protected boolean isDuplicateMessage(EbMSMessage message)
	{
		return /*message.getMessageHeader().getDuplicateElimination()!= null && */ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getMessageId());
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

	public void setSignatureValidator(EbMSSignatureValidator signatureValidator)
	{
		this.signatureValidator = signatureValidator;
	}

}
