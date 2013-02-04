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

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.Constants.EbMSMessageType;
import nl.clockwork.ebms.common.util.DOMUtils;
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
import nl.clockwork.ebms.model.ebxml.SeverityType;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.dsig.ReferenceType;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SignatureValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class EbMSMessageProcessorImpl implements EbMSMessageProcessor
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;
  private EbMSSignatureValidator ebMSSignatureValidator;
  private String hostname;
  private CPAValidator cpaValidator;
  private MessageHeaderValidator messageHeaderValidator;
  private ManifestValidator manifestValidator;
  private SignatureValidator signatureValidator;
  
	public void init()
	{
		cpaValidator = new CPAValidator();
		messageHeaderValidator = new MessageHeaderValidator(ebMSDAO);
		manifestValidator = new ManifestValidator();
		signatureValidator = new SignatureValidator(ebMSSignatureValidator);
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
				if (isDuplicate(messageHeader))
				{
					logger.warn("Duplicate message found!");
					if (equalsDuplicateMessageHeader(messageHeader))
					{
						if (message.getSyncReply() == null)
						{
							long responseId = getResponseMessageId(messageHeader);
							ebMSDAO.insertSendEvent(responseId);
							return null;
						}
						else
							return getEbMSDocument(getResponseMessage(messageHeader));
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
					if (signatureValidator.validate(errorList,cpa,document,messageHeader)
						&& cpaValidator.validate(errorList,cpa,messageHeader,timestamp)
						&& messageHeaderValidator.validate(errorList,cpa,messageHeader,message.getAckRequested(),message.getSyncReply(),message.getMessageOrder(),timestamp)
						&& signatureValidator.validate(errorList,cpa,messageHeader,message.getSignature())
						&& manifestValidator.validate(errorList,message.getManifest(),message.getAttachments()))
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
							ebMSDAO.insertMessage(timestamp.getTime(),message,EbMSMessageStatus.RECEIVED);
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
				//return null;
			}
			else if (EbMSMessageType.MESSAGE_ERROR.action().getAction().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message,EbMSMessageStatus.DELIVERY_FAILED);
				return null;
			}
			else if (EbMSMessageType.ACKNOWLEDGMENT.action().getAction().equals(message.getMessageHeader().getAction()))
			{
				process(timestamp,message,EbMSMessageStatus.DELIVERED);
				return null;
			}
			else if (EbMSMessageType.STATUS_REQUEST.action().getAction().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processStatusRequest(timestamp,message);
				return message.getSyncReply() == null ? null : getEbMSDocument(response);
			}
			else if (EbMSMessageType.STATUS_RESPONSE.action().getAction().equals(message.getMessageHeader().getAction()))
			{
				//process(timestamp,message);
				return null;
			}
			else if (EbMSMessageType.PING.action().getAction().equals(message.getMessageHeader().getAction()))
			{
				EbMSMessage response = processPing(timestamp,message);
				return message.getSyncReply() == null ? null : getEbMSDocument(response);
			}
			else if (EbMSMessageType.PONG.action().getAction().equals(message.getMessageHeader().getAction()))
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
	
	private EbMSMessage getEbMSMessage(Document document, List<EbMSAttachment> attachments) throws JAXBException
	{
		//TODO: optimize
		SignatureType signature = XMLMessageBuilder.getInstance(SignatureType.class).handle(DOMUtils.getNode(document,"http://www.w3.org/2000/09/xmldsig#","Signature"));
		MessageHeader messageHeader = XMLMessageBuilder.getInstance(MessageHeader.class).handle(getNode(document,"MessageHeader"));
		SyncReply syncReply = XMLMessageBuilder.getInstance(SyncReply.class).handle(getNode(document,"SyncReply"));
		MessageOrder messageOrder = XMLMessageBuilder.getInstance(MessageOrder.class).handle(getNode(document,"MessageOrder"));
		AckRequested ackRequested = XMLMessageBuilder.getInstance(AckRequested.class).handle(getNode(document,"AckRequested"));
		ErrorList errorList = XMLMessageBuilder.getInstance(ErrorList.class).handle(getNode(document,"ErrorList"));
		Acknowledgment acknowledgment = XMLMessageBuilder.getInstance(Acknowledgment.class).handle(getNode(document,"Acknowledgment"));
		Manifest manifest = XMLMessageBuilder.getInstance(Manifest.class).handle(getNode(document,"Manifest"));
		StatusRequest statusRequest = XMLMessageBuilder.getInstance(StatusRequest.class).handle(getNode(document,"StatusRequest"));
		StatusResponse statusResponse = XMLMessageBuilder.getInstance(StatusResponse.class).handle(getNode(document,"StatusResponse"));
		return new EbMSMessage(signature,messageHeader,syncReply,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse,attachments);
	}

	private EbMSDocument getEbMSDocument(EbMSMessage message) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException
	{
		return new EbMSDocument(EbMSMessageUtils.createSOAPMessage(message),message.getAttachments());
		
	}
	
	private Node getNode(Document document, String tagName)
	{
		return DOMUtils.getNode(document,"http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd",tagName);
	}

	private void process(final Calendar timestamp, final EbMSMessage message, final EbMSMessageStatus status)
	{
		MessageHeader messageHeader = message.getMessageHeader();
		if (isDuplicate(messageHeader))
		{
			logger.warn("Duplicate message found!");
			if (!equalsDuplicateMessageHeader(messageHeader))
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
						Long id = ebMSDAO.getEbMSMessageId(message.getMessageHeader().getMessageData().getRefToMessageId());
						if (id != null)
						{
							ebMSDAO.deleteSendEvents(id);
							ebMSDAO.updateMessageStatus(id,status);
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
			EbMSMessage statusResponse = createEbMSStatusResponse(message,cpaValidator.validate(errorList,cpa,messageHeader,timestamp) ? null : EbMSMessageStatus.UNAUTHORIZED);
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
			EbMSMessage pong = cpaValidator.validate(errorList,cpa,messageHeader,timestamp) ? null : createEbMSPong(message);
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
	
	private boolean isDuplicate(MessageHeader messageHeader)
	{
		return /*messageHeader.getDuplicateElimination()!= null && */ebMSDAO.existsMessage(messageHeader.getMessageData().getMessageId());
	}

	private boolean equalsDuplicateMessageHeader(MessageHeader messageHeader)
	{
		//TODO extend comparison (signature)
		MessageHeader duplicateMessageHeader = ebMSDAO.getMessageHeader(messageHeader.getMessageData().getMessageId());
		return messageHeader.getCPAId().equals(duplicateMessageHeader.getCPAId())
		&& messageHeader.getService().getType().equals(duplicateMessageHeader.getService().getType())
		&& messageHeader.getService().getValue().equals(duplicateMessageHeader.getService().getValue())
		&& messageHeader.getAction().equals(duplicateMessageHeader.getAction());
	}

	private long getResponseMessageId(MessageHeader messageHeader)
	{
		return ebMSDAO.getEbMSMessageResponseId(messageHeader.getMessageData().getMessageId());
	}

	private EbMSMessage getResponseMessage(MessageHeader messageHeader)
	{
		return ebMSDAO.getEbMSMessageResponse(messageHeader.getMessageData().getMessageId());
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
		MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(message.getMessageHeader(),hostname,timestamp,EbMSMessageType.MESSAGE_ERROR.action());
		if (errorList.getError().size() == 0)
			errorList.getError().add(EbMSMessageUtils.createError(Constants.EbMSErrorLocation.UNKNOWN.location(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"An unknown error occurred!"));
		return new EbMSMessage(messageHeader,errorList);
	}

	private EbMSMessage createEbMSAcknowledgment(GregorianCalendar timestamp, EbMSMessage message) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(message.getMessageHeader(),hostname,timestamp,EbMSMessageType.ACKNOWLEDGMENT.action());
		
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
	
	public void setEbMSSignatureValidator(EbMSSignatureValidator ebMSSignatureValidator)
	{
		this.ebMSSignatureValidator = ebMSSignatureValidator;
	}
	
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}

}
