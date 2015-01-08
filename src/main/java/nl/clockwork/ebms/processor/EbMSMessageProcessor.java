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
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.EventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.signature.EbMSSignatureGenerator;
import nl.clockwork.ebms.signature.EbMSSignatureValidator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.AttachmentValidator;
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
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
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
  protected EbMSSignatureGenerator signatureGenerator;
	protected EbMSSignatureValidator signatureValidator;
  protected XSDValidator xsdValidator;
  protected CPAValidator cpaValidator;
  protected MessageHeaderValidator messageHeaderValidator;
  protected ManifestValidator manifestValidator;
  protected AttachmentValidator attachmentValidator;
  protected SignatureTypeValidator signatureTypeValidator;
  protected boolean validateAttachment;
  protected Service mshMessageService;
  
	public void init()
	{
		signatureGenerator = new EbMSSignatureGenerator()
		{
			@Override
			public void generate(CollaborationProtocolAgreement cpa, AckRequested ackRequested, EbMSMessage message) throws EbMSProcessorException
			{
			}
		};
		xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/msg-header-2_0.xsd");
		cpaValidator = new CPAValidator();
		messageHeaderValidator = new MessageHeaderValidator(ebMSDAO);
		messageHeaderValidator.setAckSignatureRequested(PerMessageCharacteristicsType.NEVER);
		manifestValidator = new ManifestValidator();
		attachmentValidator = validateAttachment ? new AttachmentValidator() : new AttachmentValidator()
		{
			@Override
			public void validate(CollaborationProtocolAgreement cpa, EbMSMessage message) throws EbMSValidationException
			{
			}
		};
		signatureTypeValidator = new SignatureTypeValidator(signatureValidator);
		mshMessageService = new Service();
		mshMessageService.setValue(Constants.EBMS_SERVICE_URI);
	}
	
	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			GregorianCalendar timestamp = new GregorianCalendar();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document);
			final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(message.getMessageHeader().getCPAId());
			if (cpa == null)
			{
				logger.warn("CPA " + message.getMessageHeader().getCPAId() + " not found!");
				return null;
			}
			if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			{
				return process(cpa,timestamp,message);
			}
			else if (EbMSAction.MESSAGE_ERROR.action().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message,EbMSMessageStatus.DELIVERY_ERROR);
				return null;
			}
			else if (EbMSAction.ACKNOWLEDGMENT.action().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message,EbMSMessageStatus.DELIVERED);
				return null;
			}
			else if (EbMSAction.STATUS_REQUEST.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processStatusRequest(cpa,timestamp,message);
				response = deliveryManager.sendResponseMessage(CPAUtils.getUri(cpa,response),message,response);
				return response == null ? null : EbMSMessageUtils.getEbMSDocument(response);
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				deliveryManager.sendResponseMessage(message);
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(cpa,timestamp,message);
				response = deliveryManager.sendResponseMessage(CPAUtils.getUri(cpa,response),message,response);
				return response == null ? null : EbMSMessageUtils.getEbMSDocument(response);
			}
			else if (EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
			{
				deliveryManager.sendResponseMessage(message);
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
				GregorianCalendar timestamp = new GregorianCalendar();
				final EbMSMessage responseMessage = EbMSMessageUtils.getEbMSMessage(response);
				if (Constants.EBMS_SERVICE_URI.equals(responseMessage.getMessageHeader().getService().getValue()))
				{
					if (EbMSAction.MESSAGE_ERROR.action().equals(responseMessage.getMessageHeader().getAction()))
						process(timestamp,responseMessage,EbMSMessageStatus.DELIVERY_ERROR);
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
	
	protected EbMSDocument process(final CollaborationProtocolAgreement cpa, final GregorianCalendar timestamp, final EbMSMessage message) throws DAOException, ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, EbMSProcessorException
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
							ebMSDAO.insertDuplicateMessage(timestamp.getTime(),message);
							EbMSMessageContext messageContext = ebMSDAO.getMessageContextByRefToMessageId(messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
							ebMSDAO.insertEvent(messageContext.getMessageId(),EbMSEventType.SEND,CPAUtils.getResponseUri(cpa,message));
						}
					}
				);
				return null;
			}
			else
			{
				ebMSDAO.insertDuplicateMessage(timestamp.getTime(),message);
				return ebMSDAO.getEbMSDocumentByRefToMessageId(messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
			}
		}
		else
		{
			try
			{
				cpaValidator.validate(cpa,message);
				messageHeaderValidator.validate(cpa,message,timestamp);
				signatureTypeValidator.validate(cpa,message);
				manifestValidator.validate(message);
				attachmentValidator.validate(cpa,message);
				signatureTypeValidator.validateSignature(cpa,message);
				if (message.getAckRequested() == null)
				{
					ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.RECEIVED);
								eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
							}
						}
					);
					return null;
				}
				else
				{
					final EbMSMessage acknowledgment = EbMSMessageUtils.createEbMSAcknowledgment(cpa,message,timestamp);
					acknowledgment.setMessage(EbMSMessageUtils.createSOAPMessage(acknowledgment));
					signatureGenerator.generate(cpa,message.getAckRequested(),acknowledgment);
					ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.RECEIVED);
								ebMSDAO.insertMessage(timestamp.getTime(),acknowledgment,null);
								if (message.getSyncReply() == null)
									ebMSDAO.insertEvent(EbMSMessageUtils.createEbMSSendEvent(acknowledgment,CPAUtils.getUri(cpa,acknowledgment)));
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
				final EbMSMessage messageError = EbMSMessageUtils.createEbMSMessageError(cpa,message,errorList,timestamp);
				Document document = EbMSMessageUtils.createSOAPMessage(messageError);
				messageError.setMessage(document);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.FAILED);
							ebMSDAO.insertMessage(timestamp.getTime(),messageError,null);
							if (message.getSyncReply() == null)
								ebMSDAO.insertEvent(EbMSMessageUtils.createEbMSSendEvent(messageError,CPAUtils.getUri(cpa,messageError)));
						}
					}
				);
				return message.getSyncReply() == null ? null : new EbMSDocument(messageError.getMessage());
			}
		}
	}

	protected void process(final Calendar timestamp, final EbMSMessage responseMessage, final EbMSMessageStatus status)
	{
		if (isDuplicateMessage(responseMessage))
			ebMSDAO.insertDuplicateMessage(timestamp.getTime(),responseMessage);
		else
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.insertMessage(timestamp.getTime(),responseMessage,null);
						ebMSDAO.deleteEvents(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSEventStatus.UNPROCESSED);
						ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENT,status);
						if (status.equals(EbMSMessageStatus.DELIVERED))
							eventListener.onMessageAcknowledged(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
						else if (status.equals(EbMSMessageStatus.DELIVERY_ERROR))
							eventListener.onMessageDeliveryFailed(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
					}
				}
			);
	}
	
	protected EbMSMessage processStatusRequest(CollaborationProtocolAgreement cpa, final GregorianCalendar timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		try
		{
			GregorianCalendar c = null;
			cpaValidator.cpaExists(cpa,message);
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
				{
					c = new GregorianCalendar();
					c.setTime(context.getTimestamp());
				}
				else
					status = EbMSMessageStatus.NOT_RECOGNIZED;
			}
			return EbMSMessageUtils.createEbMSStatusResponse(cpa,message,status,c); 
		}
		catch (EbMSValidationException e)
		{
			logger.warn("",e);
			return null;
		}
	}
	
	protected EbMSMessage processPing(CollaborationProtocolAgreement cpa, final GregorianCalendar timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		try
		{
			cpaValidator.cpaExists(cpa,message);
			return EbMSMessageUtils.createEbMSPong(cpa,message);
		}
		catch(EbMSValidationException e)
		{
			logger.warn("",e);
			return null;
		}
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
	
	public void setSignatureValidator(EbMSSignatureValidator signatureValidator)
	{
		this.signatureValidator = signatureValidator;
	}

	public void setValidateAttachment(boolean validateAttachment)
	{
		this.validateAttachment = validateAttachment;
	}
}
