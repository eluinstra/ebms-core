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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBElement;
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
import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.MessageStatusType;
import nl.clockwork.ebms.model.ebxml.Service;
import nl.clockwork.ebms.model.ebxml.SeverityType;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.soap.envelope.Envelope;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SignatureTypeValidator;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSMessageProcessorImpl implements EbMSMessageProcessor
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;
  private EbMSSignatureValidator signatureValidator;
  private String hostname;
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
	}
	
	@Override
	public EbMSDocument process(EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			GregorianCalendar timestamp = new GregorianCalendar();
			EbMSMessage message = getEbMSMessage(document.getMessage(),document.getAttachments());
			if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			{
				EbMSMessage response = process(timestamp,document,message);
				return response == null ? null : getEbMSDocument(response);
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
				return response == null ? null : getEbMSDocument(response);
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message);
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(timestamp,message);
				return response == null ? null : getEbMSDocument(response);
			}
			else if (EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message);
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
	
	private EbMSMessage process(final GregorianCalendar timestamp, EbMSDocument document, final EbMSMessage message) throws DAOException, ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		if (isDuplicateMessage(message))
		{
			logger.warn("Duplicate message found!");
			if (equalsDuplicateMessage(message))
			{
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
				logger.warn("Duplicate messages are not identical! Message discarded.");
				return null;
			}
		}
		else
		{
			ErrorList errorList = createErrorList();
			final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			if (cpaValidator.isValid(errorList,cpa,messageHeader,timestamp)
				&& messageHeaderValidator.isValid(errorList,cpa,messageHeader,message.getAckRequested(),message.getSyncReply(),message.getMessageOrder(),timestamp)
				&& signatureTypeValidator.isValid(errorList,cpa,messageHeader,message.getSignature())
				&& manifestValidator.isValid(errorList,message.getManifest(),message.getAttachments())
				&& signatureTypeValidator.isValid(errorList,cpa,document,messageHeader)
			)
			{
				logger.info("Message valid.");
				if (message.getAckRequested() != null)
				{
					final EbMSMessage acknowledgment = EbMSMessageUtils.createEbMSAcknowledgment(message,hostname,timestamp);
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
									final EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(cpa,acknowledgment.getMessageHeader());
									ebMSDAO.insertSendEvent(id,sendEvent);
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
				logger.warn("Message not valid.");
				final EbMSMessage messageError = EbMSMessageUtils.createEbMSMessageError(message,errorList,hostname,timestamp);
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
								final EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(cpa,messageError.getMessageHeader());
								ebMSDAO.insertSendEvent(id,sendEvent);
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
			logger.warn("Duplicate message found!");
			if (!equalsDuplicateMessage(message))
				logger.warn("Duplicate messages are not identical! Message discarded.");
		}
		else if (isDuplicateRefToMessage(message))
			logger.warn("Duplicate response message found! Message discarded.");
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
		if (isDuplicateMessage(message))
		{
			logger.warn("Duplicate message found!");
			if (equalsDuplicateMessage(message))
			{
				if (message.getSyncReply() == null)
				{
					long responseId = ebMSDAO.getMessageId(message.getMessageHeader().getMessageData().getMessageId(),service,EbMSAction.STATUS_RESPONSE.action());
					ebMSDAO.insertSendEvent(responseId);
					return null;
				}
				else
					return ebMSDAO.getMessage(message.getMessageHeader().getMessageData().getMessageId(),service,EbMSAction.STATUS_RESPONSE.action());
			}
			else
			{
				logger.warn("Duplicate messages are not identical! Message discarded.");
				return null;
			}
		}
		else
		{
			MessageHeader messageHeader = message.getMessageHeader();
			final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			ErrorList errorList = createErrorList();
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
					if (MessageStatusType.RECEIVED.equals(status.statusCode()) || MessageStatusType.PROCESSED.equals(status.statusCode()) || MessageStatusType.FORWARDED.equals(status.statusCode()))
						c = header.getMessageData().getTimestamp().toGregorianCalendar();
				}
			}
			else
				status = EbMSMessageStatus.UNAUTHORIZED;
			final EbMSMessage statusResponse = EbMSMessageUtils.createEbMSStatusResponse(message,hostname,status,c); 
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
							final EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(cpa,statusResponse.getMessageHeader());
							ebMSDAO.insertSendEvent(id,sendEvent);
						}
					}
				}
			);
			return message.getSyncReply() == null ? null : statusResponse;
		}
	}
	
	private EbMSMessage processPing(final GregorianCalendar timestamp, final EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		if (isDuplicateMessage(message))
		{
			logger.warn("Duplicate message found!");
			if (equalsDuplicateMessage(message))
			{
				if (message.getSyncReply() == null)
				{
					long responseId = ebMSDAO.getMessageId(message.getMessageHeader().getMessageData().getMessageId(),service,EbMSAction.PONG.action());
					ebMSDAO.insertSendEvent(responseId);
					return null;
				}
				else
					return ebMSDAO.getMessage(message.getMessageHeader().getMessageData().getMessageId(),service,EbMSAction.PONG.action());
			}
			else
			{
				logger.warn("Duplicate messages are not identical! Message discarded.");
				return null;
			}
		}
		else
		{
			MessageHeader messageHeader = message.getMessageHeader();
			final CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			ErrorList errorList = createErrorList();
			final EbMSMessage pong = cpaValidator.isValid(errorList,cpa,messageHeader,timestamp) ? null : EbMSMessageUtils.createEbMSPong(message,hostname);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.insertMessage(timestamp.getTime(),message,null);
						long id = ebMSDAO.insertMessage(timestamp.getTime(),pong,null);
						if (message.getSyncReply() == null)
						{
							final EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(cpa,pong.getMessageHeader());
							ebMSDAO.insertSendEvent(id,sendEvent);
						}
					}
				}
			);
			return message.getSyncReply() == null ? null : pong;
		}
	}
	
	private void process(final GregorianCalendar timestamp, final EbMSMessage message)
	{
		if (isDuplicateMessage(message))
		{
			logger.warn("Duplicate message found!");
			if (!equalsDuplicateMessage(message))
				logger.warn("Duplicate messages are not identical! Message discarded.");
		}
		else
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.insertMessage(timestamp.getTime(),message,null);
					}
				}
			);
	}

	private boolean isDuplicateMessage(EbMSMessage message)
	{
		return /*message.getMessageHeader().getDuplicateElimination()!= null && */ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getMessageId());
	}
	
	private boolean equalsDuplicateMessage(EbMSMessage message)
	{
		//TODO extend comparison (signature)
		MessageHeader duplicateMessageHeader = ebMSDAO.getMessageHeader(message.getMessageHeader().getMessageData().getMessageId());
		return message.getMessageHeader().getCPAId().equals(duplicateMessageHeader.getCPAId())
		&& (message.getMessageHeader().getService().getType() == null ? duplicateMessageHeader.getService().getType() == null : message.getMessageHeader().getService().getType().equals(duplicateMessageHeader.getService().getType()))
		&& message.getMessageHeader().getService().getValue().equals(duplicateMessageHeader.getService().getValue())
		&& message.getMessageHeader().getAction().equals(duplicateMessageHeader.getAction());
	}

	private boolean isDuplicateRefToMessage(EbMSMessage message)
	{
		return ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getRefToMessageId(),service,new String[]{EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action()});
	}

	private EbMSMessage getEbMSMessage(Document document, List<EbMSAttachment> attachments) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		XMLMessageBuilder<Envelope> messageBuilder = XMLMessageBuilder.getInstance(Envelope.class,Envelope.class,MessageHeader.class,SyncReply.class,MessageOrder.class,AckRequested.class,SignatureType.class,ErrorList.class,Acknowledgment.class,Manifest.class,StatusRequest.class,StatusResponse.class);
		Envelope envelope = messageBuilder.handle(document);
		return getEbMSMessage(envelope,attachments);
		
//		SignatureType signature = XMLMessageBuilder.getInstance(SignatureType.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ds:Signature"));
//		MessageHeader messageHeader = XMLMessageBuilder.getInstance(MessageHeader.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ebxml:MessageHeader"));
//		SyncReply syncReply = XMLMessageBuilder.getInstance(SyncReply.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ebxml:SyncReply"));
//		MessageOrder messageOrder = XMLMessageBuilder.getInstance(MessageOrder.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ebxml:MessageOrder"));
//		AckRequested ackRequested = XMLMessageBuilder.getInstance(AckRequested.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ebxml:AckRequested"));
//		ErrorList errorList = XMLMessageBuilder.getInstance(ErrorList.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ebxml:ErrorList"));
//		Acknowledgment acknowledgment = XMLMessageBuilder.getInstance(Acknowledgment.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ebxml:Acknowledgment"));
//		Manifest manifest = XMLMessageBuilder.getInstance(Manifest.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Body/ebxml:Manifest"));
//		StatusRequest statusRequest = XMLMessageBuilder.getInstance(StatusRequest.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Body/ebxml:StatusRequest"));
//		StatusResponse statusResponse = XMLMessageBuilder.getInstance(StatusResponse.class).handle(DOMUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Body/ebxml:StatusResponse"));
//		return new EbMSMessage(signature,messageHeader,syncReply,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse,attachments);
	}

	@SuppressWarnings("unchecked")
	private EbMSMessage getEbMSMessage(Envelope envelope, List<EbMSAttachment> attachments)
	{
		
		SignatureType signature = null;
		MessageHeader messageHeader = null;
		SyncReply syncReply = null;
		MessageOrder messageOrder = null;
		AckRequested ackRequested = null;
		ErrorList errorList = null;
		Acknowledgment acknowledgment = null;
		for (Object element : envelope.getHeader().getAny())
			if (element instanceof JAXBElement && ((JAXBElement<?>)element).getValue() instanceof SignatureType)
				signature = ((JAXBElement<SignatureType>)element).getValue();
			else if (element instanceof MessageHeader)
				messageHeader = (MessageHeader)element;
			else if (element instanceof SyncReply)
				syncReply = (SyncReply)element;
			else if (element instanceof MessageOrder)
				messageOrder = (MessageOrder)element;
			else if (element instanceof AckRequested)
				ackRequested = (AckRequested)element;
			else if (element instanceof ErrorList)
				errorList = (ErrorList)element;
			else if (element instanceof Acknowledgment)
				acknowledgment = (Acknowledgment)element;

		Manifest manifest = null;
		StatusRequest statusRequest = null;
		StatusResponse statusResponse = null;
		for (Object element : envelope.getBody().getAny())
			if (element instanceof Manifest)
				manifest = (Manifest)element;
			else if (element instanceof StatusRequest)
				statusRequest = (StatusRequest)element;
			else if (element instanceof StatusResponse)
				statusResponse = (StatusResponse)element;

		return new EbMSMessage(signature,messageHeader,syncReply,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse,attachments);
	}
	
	private EbMSDocument getEbMSDocument(EbMSMessage message) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		return new EbMSDocument(EbMSMessageUtils.createSOAPMessage(message),message.getAttachments());
	}
	
	private ErrorList createErrorList()
	{
		ErrorList result = new ErrorList();
		result.setVersion(Constants.EBMS_VERSION);
		result.setMustUnderstand(true);
		result.setHighestSeverity(SeverityType.WARNING);
		return result;
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setSignatureValidator(EbMSSignatureValidator signatureValidator)
	{
		this.signatureValidator = signatureValidator;
	}
	
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}

}
