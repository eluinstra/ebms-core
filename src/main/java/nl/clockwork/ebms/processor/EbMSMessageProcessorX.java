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
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.signature.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SignatureTypeValidator;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.validation.XSDValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSMessageProcessorX extends EbMSMessageProcessor
{
  protected transient Log logger = LogFactory.getLog(getClass());
  
	public void init()
	{
		xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/msg-header-2_0.xsd");
		cpaValidator = new CPAValidator();
		messageHeaderValidator = new MessageHeaderValidator(ebMSDAO);
		manifestValidator = new ManifestValidator();
		signatureTypeValidator = new SignatureTypeValidator(signatureValidator);
		service = new Service();
		service.setValue(Constants.EBMS_SERVICE_URI);
	}
	
	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			GregorianCalendar timestamp = new GregorianCalendar();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document.getMessage(),document.getAttachments());
			final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(message.getMessageHeader().getCPAId());
			if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			{
				return process(cpa,timestamp,message);
			}
			else if (EbMSAction.MESSAGE_ERROR.action().equals(message.getMessageHeader().getAction()))
			{
				Document request = ebMSDAO.getDocument(message.getMessageHeader().getMessageData().getRefToMessageId());
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException("No async ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(document.getMessage()));
				processMessageError(cpa,timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.ACKNOWLEDGMENT.action().equals(message.getMessageHeader().getAction()))
			{
				Document request = ebMSDAO.getDocument(message.getAcknowledgment().getRefToMessageId());
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException("No async Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(document.getMessage()));
				processAcknowledgment(cpa,timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.STATUS_REQUEST.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = deliveryManager.handleResponseMessage(cpa,message,processStatusRequest(cpa,timestamp,message));
				return response == null ? null : EbMSMessageUtils.getEbMSDocument(response);
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				deliveryManager.handleResponseMessage(message);
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = deliveryManager.handleResponseMessage(cpa,message,processPing(cpa,timestamp,message));
				return response == null ? null : EbMSMessageUtils.getEbMSDocument(response);
			}
			else if (EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
			{
				deliveryManager.handleResponseMessage(message);
				return null;
			}
			else
				throw new EbMSProcessingException("Unable to process message! Action=" + message.getMessageHeader().getAction());
		}
		catch (ValidationException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ValidatorException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (XPathExpressionException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (JAXBException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (SAXException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (SOAPException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
	}
	
	public void processResponse(EbMSDocument request, EbMSDocument response) throws EbMSProcessorException
	{
		try
		{
			final EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request.getMessage());
			if (requestMessage.getAckRequested() != null && requestMessage.getSyncReply() != null && response == null)
				throw new EbMSProcessingException("No response received for message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
			
			if (response != null)
			{
				xsdValidator.validate(response.getMessage());
				GregorianCalendar timestamp = new GregorianCalendar();
				final EbMSMessage responseMessage = EbMSMessageUtils.getEbMSMessage(response.getMessage(),response.getAttachments());
				if (Constants.EBMS_SERVICE_URI.equals(responseMessage.getMessageHeader().getService().getValue()))
				{
					if (EbMSAction.MESSAGE_ERROR.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (requestMessage.getSyncReply() == null)
							throw new EbMSProcessingException("No sync ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processMessageError(ebMSDAO.getCPA(requestMessage.getMessageHeader().getCPAId()),timestamp,requestMessage,responseMessage);
					}
					else if (EbMSAction.ACKNOWLEDGMENT.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() == null)
							throw new EbMSProcessingException("No sync Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processAcknowledgment(ebMSDAO.getCPA(requestMessage.getMessageHeader().getCPAId()),timestamp,requestMessage,responseMessage);
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
		catch (ValidationException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ValidatorException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (XPathExpressionException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (JAXBException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (SAXException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
	}
	
	private void processMessageError(CollaborationProtocolAgreement cpa, final Calendar timestamp, final EbMSMessage requestMessage, final EbMSMessage responseMessage)
	{
		if (isDuplicateMessage(responseMessage))
			ebMSDAO.insertDuplicateMessage(timestamp.getTime(),responseMessage);
		else
		{
			try
			{
				messageHeaderValidator.validate(requestMessage,responseMessage);
				messageHeaderValidator.validate(cpa,responseMessage,timestamp);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp.getTime(),responseMessage,null);
							ebMSDAO.deleteEvents(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSEventStatus.UNPROCESSED);
							ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENT,EbMSMessageStatus.DELIVERY_ERROR);
							eventListener.onMessageDeliveryFailed(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
						}
					}
				);
			}
			catch (ValidationException e)
			{
				ebMSDAO.insertMessage(timestamp.getTime(),responseMessage,null);
				logger.warn("Unable to process MessageError " + responseMessage.getMessageHeader().getMessageData().getMessageId(),e);
			}
		}
	}
	
	private void processAcknowledgment(CollaborationProtocolAgreement cpa, final Calendar timestamp, final EbMSMessage requestMessage, final EbMSMessage responseMessage)
	{
		if (isDuplicateMessage(responseMessage))
			ebMSDAO.insertDuplicateMessage(timestamp.getTime(),responseMessage);
		else
		{
			try
			{
				messageHeaderValidator.validate(requestMessage,responseMessage);
				messageHeaderValidator.validate(cpa,responseMessage,timestamp);
				signatureValidator.validate(cpa,requestMessage,responseMessage);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp.getTime(),responseMessage,null);
							ebMSDAO.deleteEvents(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSEventStatus.UNPROCESSED);
							ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENT,EbMSMessageStatus.DELIVERED);
							eventListener.onMessageAcknowledged(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
						}
					}
				);
			}
			catch (ValidatorException e)
			{
				ebMSDAO.insertMessage(timestamp.getTime(),responseMessage,null);
				logger.warn("Unable to process Acknowledgment " + responseMessage.getMessageHeader().getMessageData().getMessageId(),e);
			}
		}
	}
	
	public void setSignatureGenerator(EbMSSignatureGenerator signatureGenerator)
	{
		this.signatureGenerator = signatureGenerator;
	}
	
}
