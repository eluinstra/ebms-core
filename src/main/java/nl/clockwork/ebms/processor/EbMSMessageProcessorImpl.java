/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.processor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageStatusType;
import nl.clockwork.ebms.model.ebxml.Service;
import nl.clockwork.ebms.model.xml.dsig.ReferenceType;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SignatureTypeValidator;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

public class EbMSMessageProcessorImpl implements EbMSMessageProcessor
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;
  private ExecutorService executorService;
  private int threadPoolSize = 10;
  private EbMSClient ebMSClient;
  private EbMSSignatureValidator signatureValidator;
  private CPAValidator cpaValidator;
  private MessageHeaderValidator messageHeaderValidator;
  private ManifestValidator manifestValidator;
  private SignatureTypeValidator signatureTypeValidator;
  private Service service;
  
	public void init()
	{
		cpaValidator = new CPAValidator();
		messageHeaderValidator = new MessageHeaderValidator(ebMSDAO);
		manifestValidator = new ManifestValidator();
		signatureTypeValidator = new SignatureTypeValidator(signatureValidator);
		service = new Service();
		service.setValue(Constants.EBMS_SERVICE_URI);
		executorService = Executors.newFixedThreadPool(threadPoolSize);
	}
	
	@Override
	public EbMSDocument process(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			GregorianCalendar timestamp = new GregorianCalendar();
			final EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document.getMessage(),document.getAttachments());
			final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(message.getMessageHeader().getCPAId());
			if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			{
				EbMSMessage response = process(cpa,timestamp,document,message);
				return response == null ? null : EbMSMessageUtils.getEbMSDocument(response);
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
				EbMSMessage response = processStatusRequest(timestamp,message);
				return handleResponseMessage(cpa,message,response);
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(timestamp,message);
				return handleResponseMessage(cpa,message,response);
			}
			else if (EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
			{
				return null;
			}
			else
				// TODO create messageError???
				return null;
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	private EbMSDocument handleResponseMessage(final CollaborationProtocolAgreement cpa, final EbMSMessage message, final EbMSMessage response) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		if (message.getSyncReply() == null)
		{
			if (response != null)
			{
				Runnable command = new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							String uri = CPAUtils.getUri(cpa,message);
							ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
						}
						catch (Exception e)
						{
							logger.error("",e);
						}
					}
				};
				executorService.execute(command);
			}
			return null;
		}
		else
		{
			return response == null ? null : EbMSMessageUtils.getEbMSDocument(response);
		}
	}

	private EbMSMessage process(CollaborationProtocolAgreement cpa, final GregorianCalendar timestamp, EbMSDocument document, final EbMSMessage message) throws DAOException, ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		if (isDuplicateMessage(message))
		{
			if (equalsDuplicateMessage(message))
			{
				logger.warn("Duplicate message found! MessageId: " + message.getMessageHeader().getMessageData().getMessageId());
				if (message.getSyncReply() == null)
				{
					long responseId = ebMSDAO.getMessageId(messageHeader.getMessageData().getMessageId(),service,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
					ebMSDAO.insertSendEvent(responseId);
					return null;
				}
				else
					return ebMSDAO.getMessage(messageHeader.getMessageData().getMessageId(),service,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
			}
			else
			{
				logger.warn("Duplicate but unidentical message found! MessageId: " + message.getMessageHeader().getMessageData().getMessageId());
				return null;
			}
		}
		else
		{
			ErrorList errorList = EbMSMessageUtils.createErrorList();
			if (cpaValidator.isValid(errorList,cpa,messageHeader,timestamp)
				&& messageHeaderValidator.isValid(errorList,cpa,messageHeader,message.getAckRequested(),message.getSyncReply(),message.getMessageOrder(),timestamp)
				&& signatureTypeValidator.isValid(errorList,cpa,messageHeader,message.getSignature())
				&& manifestValidator.isValid(errorList,message.getManifest(),message.getAttachments())
				&& signatureTypeValidator.isValid(errorList,cpa,document,messageHeader)
			)
			{
				logger.info("Message valid. MessageId: " + message.getMessageHeader().getMessageData().getMessageId());
				if (message.getAckRequested() != null)
				{
					final EbMSMessage acknowledgment = EbMSMessageUtils.createEbMSAcknowledgment(cpa,message,timestamp);
					ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.RECEIVED);
								long id = ebMSDAO.insertMessage(timestamp.getTime(),acknowledgment,null);
								if (message.getSyncReply() == null)
								{
									EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(id,acknowledgment.getMessageHeader());
									ebMSDAO.insertSendEvent(sendEvent);
								}
							}
						}
					);
					return message.getSyncReply() == null ? null : acknowledgment;
				}
				else
				{
					ebMSDAO.executeTransaction(
						new DAOTransactionCallback()
						{
							@Override
							public void doInTransaction()
							{
								ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.RECEIVED);
							}
						}
					);
					return null;
				}
			}
			else
			{
				logger.warn("Message invalid. MessageId: " + message.getMessageHeader().getMessageData().getMessageId());
				final EbMSMessage messageError = EbMSMessageUtils.createEbMSMessageError(cpa,message,errorList,timestamp);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.FAILED);
							long id = ebMSDAO.insertMessage(timestamp.getTime(),messageError,null);
							if (message.getSyncReply() == null)
							{
								EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(id,messageError.getMessageHeader());
								ebMSDAO.insertSendEvent(sendEvent);
							}
						}
					}
				);
				return message.getSyncReply() == null ? null : messageError;
			}
		}
	}

	private void process(final Calendar timestamp, final EbMSMessage message, final EbMSMessageStatus status)
	{
		if (isDuplicateMessage(message))
		{
			if (equalsDuplicateMessage(message))
				logger.warn("Duplicate message found! MessageId: " + message.getMessageHeader().getMessageData().getMessageId());
			else
				logger.warn("Duplicate but unidentical message found! MessageId: " + message.getMessageHeader().getMessageData().getMessageId());
		}
		else if (isDuplicateRefToMessage(message))
			logger.warn("Duplicate response message found! RefToMessageId: " + message.getMessageHeader().getMessageData().getRefToMessageId());
		else
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.insertMessage(timestamp.getTime(),message,null);
						Long id = ebMSDAO.getMessageId(message.getMessageHeader().getMessageData().getRefToMessageId());
						if (id != null)
						{
							ebMSDAO.deleteSendEvents(id,EbMSEventStatus.UNPROCESSED);
							ebMSDAO.updateMessageStatus(id,null,status);
						}
					}
				}
			);
	}
	
	private EbMSMessage processStatusRequest(final GregorianCalendar timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		GregorianCalendar c = timestamp;
		MessageHeader messageHeader = message.getMessageHeader();
		final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
		ErrorList errorList = EbMSMessageUtils.createErrorList();
		EbMSMessageStatus status = null; //EbMSMessageStatus.UNAUTHORIZED;
		if (cpaValidator.isValid(errorList,cpa,messageHeader,timestamp))
		{
			MessageHeader header = ebMSDAO.getMessageHeader(message.getStatusRequest().getRefToMessageId());
			if (header == null || header.getService().getValue().equals(Constants.EBMS_SERVICE_URI))
				status = EbMSMessageStatus.NOT_RECOGNIZED;
			else if (!header.getCPAId().equals(message.getMessageHeader().getCPAId()))
				status = EbMSMessageStatus.UNAUTHORIZED;
			else
			{
				status = ebMSDAO.getMessageStatus(message.getStatusRequest().getRefToMessageId());
				if (status != null && (MessageStatusType.RECEIVED.equals(status.statusCode()) || MessageStatusType.PROCESSED.equals(status.statusCode()) || MessageStatusType.FORWARDED.equals(status.statusCode())))
					c = header.getMessageData().getTimestamp().toGregorianCalendar();
			}
		}
		else
			status = EbMSMessageStatus.UNAUTHORIZED;
		final EbMSMessage statusResponse = EbMSMessageUtils.createEbMSStatusResponse(cpa,message,status,c); 
		ebMSDAO.executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					ebMSDAO.insertMessage(timestamp.getTime(),message,null);
					long id = ebMSDAO.insertMessage(timestamp.getTime(),statusResponse,null);
					if (message.getSyncReply() == null)
					{
						EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(id,statusResponse.getMessageHeader());
						ebMSDAO.insertSendEvent(sendEvent);
					}
				}
			}
		);
		return statusResponse;
	}
	
	private EbMSMessage processPing(final GregorianCalendar timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
		ErrorList errorList = EbMSMessageUtils.createErrorList();
		return cpaValidator.isValid(errorList,cpa,messageHeader,timestamp) ? null : EbMSMessageUtils.createEbMSPong(cpa,message);
	}
	
	private boolean isDuplicateMessage(EbMSMessage message)
	{
		return /*message.getMessageHeader().getDuplicateElimination()!= null && */ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getMessageId());
	}
	
	private boolean equalsDuplicateMessage(EbMSMessage message)
	{
		EbMSMessage duplicateMessage = ebMSDAO.getMessage(message.getMessageHeader().getMessageData().getMessageId());
		return equals(message.getMessageHeader(),duplicateMessage.getMessageHeader())
				&& equals(message.getSignature(),duplicateMessage.getSignature());
	}

	private boolean equals(MessageHeader messageHeader, MessageHeader duplicateMessageHeader)
	{
		return messageHeader.getCPAId().equals(duplicateMessageHeader.getCPAId())
				&& (messageHeader.getService().getType() == null ? duplicateMessageHeader.getService().getType() == null : messageHeader.getService().getType().equals(duplicateMessageHeader.getService().getType()))
				&& messageHeader.getService().getValue().equals(duplicateMessageHeader.getService().getValue())
				&& messageHeader.getAction().equals(duplicateMessageHeader.getAction());
	}

	private boolean equals(SignatureType signature, SignatureType duplicateSignature)
	{
		if (signature == null && duplicateSignature == null)
			return true;
		else if (signature == null || duplicateSignature == null)
			return false;
		else
		{
			boolean result = Arrays.equals(signature.getSignatureValue().getValue(),duplicateSignature.getSignatureValue().getValue()) && signature.getSignedInfo().getReference().size() == duplicateSignature.getSignedInfo().getReference().size();
			for (int i = 0; result && i < signature.getSignedInfo().getReference().size() ; i++)
				result &= equals(signature.getSignedInfo().getReference().get(i),duplicateSignature.getSignedInfo().getReference().get(i));
			return result;
		}
	}

	private boolean equals(ReferenceType referenceType, ReferenceType duplicateRreferenceType)
	{
		return (referenceType.getId() == null ? duplicateRreferenceType.getId() == null : referenceType.getId().equals(duplicateRreferenceType.getId()))
				&& Arrays.equals(referenceType.getDigestValue(),duplicateRreferenceType.getDigestValue());
	}

	private boolean isDuplicateRefToMessage(EbMSMessage message)
	{
		return ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getRefToMessageId(),service,new String[]{EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action()});
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setSignatureValidator(EbMSSignatureValidator signatureValidator)
	{
		this.signatureValidator = signatureValidator;
	}
	
	public void setThreadPoolSize(int threadPoolSize)
	{
		this.threadPoolSize = threadPoolSize;
	} 
	
}
