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
import nl.clockwork.ebms.encryption.EbMSMessageDecrypter;
import nl.clockwork.ebms.event.EventListener;
import nl.clockwork.ebms.job.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SSLSessionValidator;
import nl.clockwork.ebms.validation.SignatureValidator;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.validation.XSDValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSMessageProcessor
{
  protected transient Log logger = LogFactory.getLog(getClass());
  protected boolean checkDuplicateMessage;
  protected DeliveryManager deliveryManager;
  protected EventListener eventListener;
	protected EbMSDAO ebMSDAO;
	protected CPAManager cpaManager;
	protected EbMSMessageFactory ebMSMessageFactory;
	protected EventManager eventManager;
	protected EbMSSignatureGenerator signatureGenerator;
	protected XSDValidator xsdValidator;
	protected SSLSessionValidator sslCertificateValidator;
	protected CPAValidator cpaValidator;
	protected MessageHeaderValidator messageHeaderValidator;
	protected ManifestValidator manifestValidator;
	protected SignatureValidator signatureValidator;
	protected EbMSMessageDecrypter messageDecrypter;
	protected Service mshMessageService;

	public EbMSDocument processRequest(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			xsdValidator.validate(document.getMessage());
			Date timestamp = new Date();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document);
			if (!cpaManager.existsCPA(message.getMessageHeader().getCPAId()))
				throw new EbMSProcessingException("CPA " + message.getMessageHeader().getCPAId() + " not found!");
			sslCertificateValidator.validate(message);
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
				if (isSyncReply(message))
					return EbMSMessageUtils.getEbMSDocument(response);
				else
				{
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),new CacheablePartyId(response.getMessageHeader().getTo().getPartyId()),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction()),response);
					return null;
				}
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				deliveryManager.handleResponseMessage(message);
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(message.getMessageHeader().getCPAId(),timestamp,message);
				if (isSyncReply(message))
					return EbMSMessageUtils.getEbMSDocument(response);
				else
				{
					deliveryManager.sendResponseMessage(cpaManager.getUri(response.getMessageHeader().getCPAId(),new CacheablePartyId(response.getMessageHeader().getTo().getPartyId()),response.getMessageHeader().getTo().getRole(),CPAUtils.toString(response.getMessageHeader().getService()),response.getMessageHeader().getAction()),response);
					return null;
				}
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
					{
						if (!isSyncReply(requestMessage))
							throw new EbMSProcessingException("No sync ErrorMessage expected for message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + "\n" + DOMUtils.toString(response.getMessage()));
						processMessageError(requestMessage.getMessageHeader().getCPAId(),timestamp,requestMessage,responseMessage);
					}
					else if (EbMSAction.ACKNOWLEDGMENT.action().equals(responseMessage.getMessageHeader().getAction()))
					{
						if (requestMessage.getAckRequested() == null || !isSyncReply(requestMessage))
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
		if (isDuplicateMessage(responseMessage))
		{
			if (isIdenticalMessage(responseMessage))
			{
				logger.warn("MessageError " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
				ebMSDAO.insertDuplicateMessage(timestamp,responseMessage);
			}
			else
				throw new EbMSProcessingException("MessageId " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " already used!");
		}
		else
		{
			try
			{
				messageHeaderValidator.validate(requestMessage,responseMessage);
				messageHeaderValidator.validate(responseMessage,timestamp);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp,responseMessage,null);
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
	
	private void processAcknowledgment(String cpaId, final Date timestamp, final EbMSMessage requestMessage, final EbMSMessage responseMessage) throws EbMSProcessingException
	{
		if (isDuplicateMessage(responseMessage))
		{
			if (isIdenticalMessage(responseMessage))
			{
				logger.warn("Acknowledgment " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
				ebMSDAO.insertDuplicateMessage(timestamp,responseMessage);
			}
			else
				throw new EbMSProcessingException("MessageId " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " already used!");
		}
		else
		{
			try
			{
				messageHeaderValidator.validate(requestMessage,responseMessage);
				messageHeaderValidator.validate(responseMessage,timestamp);
				signatureValidator.validate(requestMessage,responseMessage);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp,responseMessage,null);
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
	
	protected EbMSDocument process(final Date timestamp, final EbMSMessage message) throws DAOException, ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, EbMSProcessorException
	{
		final MessageHeader messageHeader = message.getMessageHeader();
		if (isDuplicateMessage(message))
		{
			if (isIdenticalMessage(message))
			{
				logger.warn("Message " + message.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
				if (isSyncReply(message))
				{
					ebMSDAO.insertDuplicateMessage(timestamp,message);
					EbMSDocument result = ebMSDAO.getEbMSDocumentByRefToMessageId(messageHeader.getCPAId(),messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
					if (result == null)
						logger.warn("No response found for duplicate message " + message.getMessageHeader().getMessageData().getMessageId() + "!");
					return result;
				}
				else
				{
					ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertDuplicateMessage(timestamp,message);
								EbMSMessageContext messageContext = ebMSDAO.getMessageContextByRefToMessageId(messageHeader.getCPAId(),messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
								if (messageContext != null)
									eventManager.createEvent(messageHeader.getCPAId(),cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(CPAUtils.createEbMSMessageService()),null),messageContext.getMessageId(),message.getMessageHeader().getMessageData().getTimeToLive(),messageContext.getTimestamp(),false);
								else
									logger.warn("No response found for duplicate message " + message.getMessageHeader().getMessageData().getMessageId() + "!");
							}
						}
					);
					return null;
				}
			}
			else
				throw new EbMSProcessingException("MessageId " + message.getMessageHeader().getMessageData().getMessageId() + " already used!");
		}
		else
		{
			try
			{
				cpaValidator.validate(message);
				messageHeaderValidator.validate(message,timestamp);
				signatureValidator.validate(message);
				manifestValidator.validate(message);
				messageDecrypter.decrypt(message);
				signatureValidator.validateSignature(message);
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
								if (!isSyncReply(message))
									eventManager.createEvent(messageHeader.getCPAId(),cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(acknowledgment.getMessageHeader().getTo().getPartyId()),acknowledgment.getMessageHeader().getTo().getRole(),CPAUtils.toString(acknowledgment.getMessageHeader().getService()),acknowledgment.getMessageHeader().getAction()),acknowledgment.getMessageHeader().getMessageData().getMessageId(),acknowledgment.getMessageHeader().getMessageData().getTimeToLive(),acknowledgment.getMessageHeader().getMessageData().getTimestamp(),false);
								eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
							}
						}
					);
					return isSyncReply(message) ? new EbMSDocument(acknowledgment.getContentId(),acknowledgment.getMessage()) : null;
				}
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
							if (!isSyncReply(message))
								eventManager.createEvent(messageHeader.getCPAId(),cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageError.getMessageHeader().getTo().getPartyId()),messageError.getMessageHeader().getTo().getRole(),CPAUtils.toString(messageError.getMessageHeader().getService()),messageError.getMessageHeader().getAction()),messageError.getMessageHeader().getMessageData().getMessageId(),messageError.getMessageHeader().getMessageData().getTimeToLive(),messageError.getMessageHeader().getMessageData().getTimestamp(),false);
						}
					}
				);
				return isSyncReply(message) ? new EbMSDocument(messageError.getContentId(),messageError.getMessage()) : null;
			}
		}
	}

	protected boolean isSyncReply(EbMSMessage message)
	{
		try
		{
			SyncReplyModeType syncReply = cpaManager.getSyncReply(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction());
			return syncReply != null && !syncReply.equals(SyncReplyModeType.NONE);
		}
		catch (Exception e)
		{
			return message.getSyncReply() != null;
		}
	}

	protected EbMSMessage processStatusRequest(String cpaId, final Date timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException, EbMSValidationException, EbMSProcessorException
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
	
	protected EbMSMessage processPing(String cpaId, final Date timestamp, final EbMSMessage message) throws EbMSProcessorException
	{
		return ebMSMessageFactory.createEbMSPong(cpaId,message);
	}
	
	protected boolean isDuplicateMessage(EbMSMessage message)
	{
		return /*message.getMessageHeader().getDuplicateElimination()!= null && */ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getMessageId());
	}
	
	private boolean isIdenticalMessage(EbMSMessage message)
	{
		return !checkDuplicateMessage || ebMSDAO.existsIdenticalMessage(message);
	}

	public void setCheckDuplicateMessage(boolean checkDuplicateMessage)
	{
		this.checkDuplicateMessage = checkDuplicateMessage;
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

	public void setSslCertificateValidator(SSLSessionValidator sslCertificateValidator)
	{
		this.sslCertificateValidator = sslCertificateValidator;
	}

	public void setCpaValidator(CPAValidator cpaValidator)
	{
		this.cpaValidator = cpaValidator;
	}

	public void setMessageHeaderValidator(MessageHeaderValidator messageHeaderValidator)
	{
		this.messageHeaderValidator = messageHeaderValidator;
	}

	public void setManifestValidator(ManifestValidator manifestValidator)
	{
		this.manifestValidator = manifestValidator;
	}

	public void setSignatureValidator(SignatureValidator signatureValidator)
	{
		this.signatureValidator = signatureValidator;
	}

	public void setMessageDecrypter(EbMSMessageDecrypter messageDecrypter)
	{
		this.messageDecrypter = messageDecrypter;
	}
}
