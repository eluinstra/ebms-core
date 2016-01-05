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
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.signature.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
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
		messageHeaderValidator = new MessageHeaderValidator(ebMSDAO,cpaManager);
		manifestValidator = new ManifestValidator();
		signatureTypeValidator = new SignatureTypeValidator(signatureValidator);
		mshMessageService = new Service();
		mshMessageService.setValue(Constants.EBMS_SERVICE_URI);
	}
	
	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			Date timestamp = new Date();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document);
			if (cpaManager.existsCPA(message.getMessageHeader().getCPAId()))
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
				Document request = ebMSDAO.getDocument(message.getMessageHeader().getMessageData().getRefToMessageId());
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException("No async ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(document.getMessage()));
				processMessageError(message.getMessageHeader().getCPAId(),timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.ACKNOWLEDGMENT.action().equals(message.getMessageHeader().getAction()))
			{
				Document request = ebMSDAO.getDocument(message.getAcknowledgment().getRefToMessageId());
				EbMSMessage requestMessage = EbMSMessageUtils.getEbMSMessage(request);
				if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() != null)
					throw new EbMSProcessingException("No async Acknowledgment expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(document.getMessage()));
				processAcknowledgment(message.getMessageHeader().getCPAId(),timestamp,requestMessage,message);
				return null;
			}
			else if (EbMSAction.STATUS_REQUEST.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processStatusRequest(message.getMessageHeader().getCPAId(),timestamp,message);
				if (message.getSyncReply() == null)
				{
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),response.getMessageHeader().getTo().getPartyId(),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction()),response);
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
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),response.getMessageHeader().getTo().getPartyId(),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction()),response);
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
				Date timestamp = new Date();
				final EbMSMessage responseMessage = EbMSMessageUtils.getEbMSMessage(response);
				if (Constants.EBMS_SERVICE_URI.equals(responseMessage.getMessageHeader().getService().getValue()))
				{
					if (EbMSAction.MESSAGE_ERROR.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (requestMessage.getSyncReply() == null)
							throw new EbMSProcessingException("No sync ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processMessageError(requestMessage.getMessageHeader().getCPAId(),timestamp,requestMessage,responseMessage);
					}
					else if (EbMSAction.ACKNOWLEDGMENT.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (requestMessage.getAckRequested() == null || requestMessage.getSyncReply() == null)
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
	
	private void processMessageError(String cpaId, final Date timestamp, final EbMSMessage requestMessage, final EbMSMessage responseMessage)
	{
		if (isDuplicateMessage(responseMessage))
			ebMSDAO.insertDuplicateMessage(timestamp,responseMessage);
		else
		{
			try
			{
				messageHeaderValidator.validate(requestMessage,responseMessage);
				messageHeaderValidator.validate(cpaId,responseMessage,timestamp);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp,responseMessage,null);
							ebMSDAO.deleteEvents(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSEventStatus.UNPROCESSED);
							ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENT,EbMSMessageStatus.DELIVERY_FAILED);
							eventListener.onMessageFailed(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
						}
					}
				);
			}
			catch (ValidationException e)
			{
				ebMSDAO.insertMessage(timestamp,responseMessage,null);
				logger.warn("Unable to process MessageError " + responseMessage.getMessageHeader().getMessageData().getMessageId(),e);
			}
		}
	}
	
	private void processAcknowledgment(String cpaId, final Date timestamp, final EbMSMessage requestMessage, final EbMSMessage responseMessage)
	{
		if (isDuplicateMessage(responseMessage))
			ebMSDAO.insertDuplicateMessage(timestamp,responseMessage);
		else
		{
			try
			{
				messageHeaderValidator.validate(requestMessage,responseMessage);
				messageHeaderValidator.validate(cpaId,responseMessage,timestamp);
				signatureValidator.validate(cpaId,requestMessage,responseMessage);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp,responseMessage,null);
							ebMSDAO.deleteEvents(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSEventStatus.UNPROCESSED);
							ebMSDAO.updateMessage(responseMessage.getMessageHeader().getMessageData().getRefToMessageId(),EbMSMessageStatus.SENT,EbMSMessageStatus.DELIVERED);
							eventListener.onMessageAcknowledged(responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
						}
					}
				);
			}
			catch (ValidatorException e)
			{
				ebMSDAO.insertMessage(timestamp,responseMessage,null);
				logger.warn("Unable to process Acknowledgment " + responseMessage.getMessageHeader().getMessageData().getMessageId(),e);
			}
		}
	}
	
	public void setSignatureGenerator(EbMSSignatureGenerator signatureGenerator)
	{
		this.signatureGenerator = signatureGenerator;
	}
	
}
