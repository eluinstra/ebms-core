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

import java.util.GregorianCalendar;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.Constants.EbMSMessageType;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.model.Signature;
import nl.clockwork.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.Error;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.From;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageStatusType;
import nl.clockwork.ebms.model.ebxml.SeverityType;
import nl.clockwork.ebms.model.xml.dsig.ReferenceType;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.ManifestValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.SignatureValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSMessageProcessorImpl implements EbMSMessageProcessor
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private String hostname;
  private EbMSDAO ebMSDAO;
  private CPAValidator cpaValidator;
  private MessageHeaderValidator messageHeaderValidator;
  private ManifestValidator manifestValidator;
  private SignatureValidator signatureValidator;
  
	public void init()
	{
		cpaValidator = new CPAValidator();
		messageHeaderValidator = new MessageHeaderValidator(ebMSDAO);
		manifestValidator = new ManifestValidator();
		signatureValidator = new SignatureValidator();
	}
	
	@Override
	public EbMSBaseMessage process(EbMSMessage message)
	{
		if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
		{
			try
			{
				GregorianCalendar timestamp = new GregorianCalendar();
				Error error = null;
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
							return getResponseMessage(messageHeader);
					}
					else
					{
						logger.warn("Duplicate messages are not identical! Message discarded.");
						return null;
					}
				}
				else
				{
					CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
					if ((error = cpaValidator.validate(cpa,messageHeader,timestamp)) == null
						&& (error = messageHeaderValidator.validate(cpa,messageHeader,message.getAckRequested(),message.getSyncReply(),message.getMessageOrder(),timestamp)) == null
						&& (error = signatureValidator.validate(cpa,messageHeader,(Signature)message.getSignature())) == null
						&& (error = manifestValidator.validate(message.getManifest(),message.getAttachments())) == null)
					{
						logger.info("Message valid.");
						if (message.getAckRequested() != null)
						{
							EbMSAcknowledgment acknowledgment = createEbMSAcknowledgment(message,timestamp);
							if (message.getSyncReply() == null)
							{
								EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(ebMSDAO.getCPA(acknowledgment.getMessageHeader().getCPAId()),acknowledgment.getMessageHeader());
								ebMSDAO.insertMessage(message,EbMSMessageStatus.RECEIVED,acknowledgment,sendEvent);
								return null;
							}
							else
							{
								ebMSDAO.insertMessage(message,EbMSMessageStatus.RECEIVED,acknowledgment,null);
								return acknowledgment;
							}
						}
						else
						{
							ebMSDAO.insertMessage(message,EbMSMessageStatus.RECEIVED,(EbMSAcknowledgment)null,(EbMSSendEvent)null);
							return null;
						}
						//ebMSDAO.insertMessage(message,EbMSMessageStatus.RECEIVED,message.getAckRequested() == null ? (EbMSAcknowledgment)null : acknowledgment,message.getSyncReply() != null ? (EbMSSendEvent)null : sendEvent);
						//return message.getAckRequested() == null || message.getSyncReply() == null ? null : acknowledgment;
					}
					else
					{
						logger.warn("Message not valid.");
						EbMSMessageError messageError = createEbMSMessageError(message,error,timestamp);
						EbMSSendEvent sendEvent = EbMSMessageUtils.getEbMSSendEvent(ebMSDAO.getCPA(messageError.getMessageHeader().getCPAId()),messageError.getMessageHeader());
						if (message.getSyncReply() == null)
						{
							ebMSDAO.insertMessage(message,EbMSMessageStatus.FAILED,messageError,sendEvent);
							return null;
						}
						else
						{
							ebMSDAO.insertMessage(message,EbMSMessageStatus.FAILED,messageError,null);
							return messageError;
						}
						//ebMSDAO.insertMessage(message,EbMSMessageStatus.FAILED,messageError,message.getSyncReply() != null ? null : sendEvent);
						//return message.getSyncReply() == null ? null : messageError;
					}
				}
				//return null;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		else if (EbMSMessageType.MESSAGE_ERROR.action().equals(message.getMessageHeader().getAction()))
		{
			process(new EbMSMessageError(message.getMessageHeader(),message.getErrorList()));
			return null;
		}
		else if (EbMSMessageType.ACKNOWLEDGMENT.action().equals(message.getMessageHeader().getAction()))
		{
			process(new EbMSAcknowledgment(message.getMessageHeader(),message.getAcknowledgment()));
			return null;
		}
		else if (EbMSMessageType.STATUS_REQUEST.action().equals(message.getMessageHeader().getAction()))
		{
			EbMSBaseMessage response = process(new EbMSStatusRequest(message.getMessageHeader(),null,message.getStatusRequest()));
			return message.getSyncReply() == null ? null : response;
		}
		else if (EbMSMessageType.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
		{
			//process(new EbMSStatusResponse(message.getMessageHeader(),message.getStatusResponse()));
			return null;
		}
		else if (EbMSMessageType.PING.action().equals(message.getMessageHeader().getAction()))
		{
			EbMSBaseMessage response = process(new EbMSPing(message.getMessageHeader(),null));
			return message.getSyncReply() == null ? null : response;
		}
		else if (EbMSMessageType.PONG.action().equals(message.getMessageHeader().getAction()))
		{
			//process(new EbMSPong(message.getMessageHeader()));
			return null;
		}
		else
			// TODO create messageError???
			return null;
	}
	
	private void process(EbMSMessageError messageError)
	{
		MessageHeader messageHeader = messageError.getMessageHeader();
		if (isDuplicate(messageHeader))
		{
			logger.warn("Duplicate message found!");
			if (!equalsDuplicateMessageHeader(messageHeader))
				logger.warn("Duplicate messages are not identical! Message discarded.");
		}
		else
			ebMSDAO.insertMessage(messageError,EbMSMessageStatus.DELIVERY_FAILED);
	}
	
	private void process(EbMSAcknowledgment acknowledgment)
	{
		MessageHeader messageHeader = acknowledgment.getMessageHeader();
		if (isDuplicate(messageHeader))
		{
			logger.warn("Duplicate message found!");
			if (!equalsDuplicateMessageHeader(messageHeader))
				logger.warn("Duplicate messages are not identical! Message discarded.");
		}
		else
			ebMSDAO.insertMessage(acknowledgment,EbMSMessageStatus.DELIVERED);
	}
	
	private EbMSBaseMessage process(EbMSStatusRequest statusRequest)
	{
		try
		{
			GregorianCalendar timestamp = new GregorianCalendar();
			MessageHeader messageHeader = statusRequest.getMessageHeader();
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			EbMSStatusResponse statusResponse = createEbMSStatusResponse(statusRequest,cpaValidator.validate(cpa,messageHeader,timestamp) == null ? null : EbMSMessageStatus.UNAUTHORIZED);
			if (statusRequest.getSyncReply() == null)
			{
				//TODO store statusResponse and sendEvent and add duplicate detection
				//return null;
				return createEbMSMessageError(statusRequest,EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"SyncReply mode not supported."),timestamp);
			}
			else
				return statusResponse;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private EbMSBaseMessage process(EbMSPing ping)
	{
		try
		{
			GregorianCalendar timestamp = new GregorianCalendar();
			MessageHeader messageHeader = ping.getMessageHeader();
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			EbMSPong pong = cpaValidator.validate(cpa,messageHeader,timestamp) == null ? null : createEbMSPong(ping);
			if (ping.getSyncReply() == null)
			{
				//TODO store pong and sendEvent and add duplicate detection
				//return null;
				return createEbMSMessageError(ping,EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"SyncReply mode not supported."),timestamp);
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

	private EbMSBaseMessage getResponseMessage(MessageHeader messageHeader)
	{
		return ebMSDAO.getEbMSMessageResponse(messageHeader.getMessageData().getMessageId());
	}

	private EbMSMessageError createEbMSMessageError(EbMSBaseMessage message, Error error, GregorianCalendar timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(message.getMessageHeader(),hostname,timestamp,EbMSMessageType.MESSAGE_ERROR.action());
		
		ErrorList errorList = new ErrorList();

		errorList.setVersion(Constants.EBMS_VERSION);
		errorList.setMustUnderstand(true);
		errorList.setHighestSeverity(SeverityType.ERROR);

		if (error == null)
			error = EbMSMessageUtils.createError(Constants.EbMSErrorLocation.UNKNOWN.location(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"An unknown error occurred!");
		errorList.getError().add(error);
		
		return new EbMSMessageError(messageHeader,errorList);
	}

	private EbMSAcknowledgment createEbMSAcknowledgment(EbMSMessage message, GregorianCalendar timestamp) throws DatatypeConfigurationException, JAXBException
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

		return new EbMSAcknowledgment(messageHeader,acknowledgment);
	}
	
	private EbMSStatusResponse createEbMSStatusResponse(EbMSStatusRequest statusRequest, EbMSMessageStatus status) throws DatatypeConfigurationException, JAXBException
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

	public EbMSPong createEbMSPong(EbMSPing ping) throws DatatypeConfigurationException, JAXBException
	{
		return EbMSMessageUtils.ebMSPingToEbMSPong(ping,hostname);
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
