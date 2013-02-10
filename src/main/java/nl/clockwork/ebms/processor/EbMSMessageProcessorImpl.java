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
import javax.xml.datatype.DatatypeFactory;
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
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.From;
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
import nl.clockwork.ebms.model.xml.dsig.ReferenceType;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SignatureTypeValidator;

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
			final GregorianCalendar timestamp = new GregorianCalendar();
			final EbMSMessage message = getEbMSMessage(document.getMessage(),document.getAttachments());
			if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			{
				MessageHeader messageHeader = message.getMessageHeader();
				if (isDuplicateMessage(message))
				{
					logger.warn("Duplicate message found!");
					if (equalsDuplicateMessage(message))
					{
						if (message.getSyncReply() == null)
						{
							long responseId = ebMSDAO.getMessageId(messageHeader.getMessageData().getMessageId(),service,new String[]{EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action()});
							ebMSDAO.insertSendEvent(responseId);
							return null;
						}
						else
							return getEbMSDocument(ebMSDAO.getMessage(messageHeader.getMessageData().getMessageId(),service,new String[]{EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action()}));
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
					CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
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
							final EbMSMessage acknowledgment = createEbMSAcknowledgment(timestamp,message);
							if (message.getSyncReply() == null)
							{
								final EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(ebMSDAO.getCPA(acknowledgment.getMessageHeader().getCPAId()),acknowledgment.getMessageHeader());
								ebMSDAO.executeTransaction(
									new DAOTransactionCallback()
									{
										@Override
										public void doInTransaction()
										{
											ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.RECEIVED);
											long id = ebMSDAO.insertMessage(timestamp.getTime(),acknowledgment,null);
											ebMSDAO.insertSendEvent(id,sendEvent);
										}
									}
								);
								return null;
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
											ebMSDAO.insertMessage(timestamp.getTime(),acknowledgment,null);
										}
									}
								);
								return getEbMSDocument(acknowledgment);
							}
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
						final EbMSMessage messageError = createEbMSMessageError(timestamp,message,errorList);
						final EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(ebMSDAO.getCPA(messageError.getMessageHeader().getCPAId()),messageError.getMessageHeader());
						if (message.getSyncReply() == null)
						{
							ebMSDAO.executeTransaction(
								new DAOTransactionCallback()
								{
									@Override
									public void doInTransaction()
									{
										ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.FAILED);
										long id = ebMSDAO.insertMessage(timestamp.getTime(),messageError,null);
										ebMSDAO.insertSendEvent(id,sendEvent);
									}
								}
							);
							return null;
						}
						else
						{
							ebMSDAO.executeTransaction(
								new DAOTransactionCallback()
								{
									@Override
									public void doInTransaction()
									{
										ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.FAILED);
										ebMSDAO.insertMessage(timestamp.getTime(),messageError,null);
									}
								}
							);
							return getEbMSDocument(messageError);
						}
					}
				}
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
				return message.getSyncReply() == null ? null : getEbMSDocument(response);
			}
			else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			{
				//process(timestamp,message);
				return null;
			}
			else if (EbMSAction.PING.action().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(timestamp,message);
				return message.getSyncReply() == null ? null : getEbMSDocument(response);
			}
			else if (EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
			{
				//process(message);
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
	public EbMSMessage getEbMSMessage(Envelope envelope, List<EbMSAttachment> attachments)
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
	
	private EbMSMessage processStatusRequest(GregorianCalendar timestamp, EbMSMessage message)
	{
		try
		{
			//TODO store statusResponse and sendEvent and add duplicate detection
			MessageHeader messageHeader = message.getMessageHeader();
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			ErrorList errorList = createErrorList();
			EbMSMessage statusResponse = createEbMSStatusResponse(message,cpaValidator.isValid(errorList,cpa,messageHeader,timestamp) ? null : EbMSMessageStatus.UNAUTHORIZED);
			if (message.getSyncReply() == null)
			{
				errorList = createErrorList();
				errorList.getError().add(EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"SyncReply mode not supported."));
				return createEbMSMessageError(timestamp,message,errorList );
			}
			else
				return statusResponse;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private EbMSMessage processPing(GregorianCalendar timestamp, EbMSMessage message)
	{
		try
		{
			//TODO store pong and sendEvent and add duplicate detection
			MessageHeader messageHeader = message.getMessageHeader();
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			ErrorList errorList = createErrorList();
			EbMSMessage pong = cpaValidator.isValid(errorList,cpa,messageHeader,timestamp) ? null : createEbMSPong(message);
			if (message.getSyncReply() == null)
			{
				errorList = createErrorList();
				errorList.getError().add(EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"SyncReply mode not supported."));
				return createEbMSMessageError(timestamp,message,errorList);
			}
			else
				return pong;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
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
		&& message.getMessageHeader().getService().getType().equals(duplicateMessageHeader.getService().getType())
		&& message.getMessageHeader().getService().getValue().equals(duplicateMessageHeader.getService().getValue())
		&& message.getMessageHeader().getAction().equals(duplicateMessageHeader.getAction());
	}

	private boolean isDuplicateRefToMessage(EbMSMessage message)
	{
		return ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getRefToMessageId(),service,new String[]{EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action()});
	}

	private ErrorList createErrorList()
	{
		ErrorList result = new ErrorList();
		result.setVersion(Constants.EBMS_VERSION);
		result.setMustUnderstand(true);
		result.setHighestSeverity(SeverityType.WARNING);
		return result;
	}
	
	private EbMSMessage createEbMSMessageError(GregorianCalendar timestamp, EbMSMessage message, ErrorList errorList) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(message.getMessageHeader(),hostname,timestamp,EbMSAction.MESSAGE_ERROR);
		if (errorList.getError().size() == 0)
		{
			errorList.getError().add(EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"An unknown error occurred!"));
			errorList.setHighestSeverity(SeverityType.ERROR);
		}
		return new EbMSMessage(messageHeader,errorList);
	}

	private EbMSMessage createEbMSAcknowledgment(GregorianCalendar timestamp, EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(message.getMessageHeader(),hostname,timestamp,EbMSAction.ACKNOWLEDGMENT);
		
		Acknowledgment acknowledgment = new Acknowledgment();

		acknowledgment.setVersion(Constants.EBMS_VERSION);
		acknowledgment.setMustUnderstand(true);

		acknowledgment.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp));
		acknowledgment.setRefToMessageId(messageHeader.getMessageData().getRefToMessageId());
		acknowledgment.setFrom(new From()); //optioneel
		acknowledgment.getFrom().getPartyId().addAll(messageHeader.getFrom().getPartyId());
		// ebMS specs 1701
		//acknowledgment.getFrom().setRole(messageHeader.getFrom().getRole());
		acknowledgment.getFrom().setRole(null);
		
		//TODO resolve actor from CPA
		acknowledgment.setActor(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value());
		
		if (message.getAckRequested().isSigned() && message.getSignature() != null)
			for (ReferenceType reference : message.getSignature().getSignedInfo().getReference())
				acknowledgment.getReference().add(reference);

		return new EbMSMessage(messageHeader,acknowledgment);
	}
	
	private EbMSMessage createEbMSStatusResponse(EbMSMessage statusRequest, EbMSMessageStatus status) throws DatatypeConfigurationException, JAXBException
	{
		GregorianCalendar timestamp = null;
		if (status == null)
		{
			MessageHeader messageHeader = ebMSDAO.getMessageHeader(statusRequest.getStatusRequest().getRefToMessageId());
			if (messageHeader == null || messageHeader.getService().getValue().equals(Constants.EBMS_SERVICE_URI))
				status = EbMSMessageStatus.NOT_RECOGNIZED;
			else if (!messageHeader.getCPAId().equals(statusRequest.getMessageHeader().getCPAId()))
				status = EbMSMessageStatus.UNAUTHORIZED;
			else
			{
				status = ebMSDAO.getMessageStatus(statusRequest.getStatusRequest().getRefToMessageId());
				if (MessageStatusType.RECEIVED.equals(status.statusCode()) || MessageStatusType.PROCESSED.equals(status.statusCode()) || MessageStatusType.FORWARDED.equals(status.statusCode()))
					timestamp = messageHeader.getMessageData().getTimestamp().toGregorianCalendar();
			}
		}
		return EbMSMessageUtils.ebMSStatusRequestToEbMSStatusResponse(statusRequest,hostname,status,timestamp);
	}

	public EbMSMessage createEbMSPong(EbMSMessage ping) throws DatatypeConfigurationException, JAXBException
	{
		return EbMSMessageUtils.ebMSPingToEbMSPong(ping,hostname);
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
