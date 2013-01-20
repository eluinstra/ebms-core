package nl.clockwork.ebms.service;

import java.util.GregorianCalendar;

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
import nl.clockwork.ebms.model.xml.xmldsig.ReferenceType;
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

	@Override
	public EbMSBaseMessage process(EbMSMessage message)
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
					//return /*message.getAckRequested() == null || */message.getSyncReply() == null ? null : acknowledgment;
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
	
	@Override
	public void process(EbMSMessageError messageError)
	{
		MessageHeader messageHeader = messageError.getMessageHeader();
		if (isDuplicate(messageHeader))
		{
			logger.warn("Duplicate message found!");
			if (!equalsDuplicateMessageHeader(messageHeader))
				logger.warn("Duplicate messages are not identical! Message discarded.");
		}
		else
			ebMSDAO.insertMessage(messageError,null);
	}
	
	@Override
	public void process(EbMSAcknowledgment acknowledgment)
	{
		MessageHeader messageHeader = acknowledgment.getMessageHeader();
		if (isDuplicate(messageHeader))
		{
			logger.warn("Duplicate message found!");
			if (!equalsDuplicateMessageHeader(messageHeader))
				logger.warn("Duplicate messages are not identical! Message discarded.");
		}
		else
			ebMSDAO.insertMessage(acknowledgment,null);
	}
	
	@Override
	public EbMSBaseMessage process(EbMSStatusRequest statusRequest)
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
	
	@Override
	public EbMSBaseMessage process(EbMSPing ping)
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

	private EbMSMessageError createEbMSMessageError(EbMSBaseMessage message, Error error, GregorianCalendar timestamp) throws DatatypeConfigurationException
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

	private EbMSAcknowledgment createEbMSAcknowledgment(EbMSMessage message, GregorianCalendar timestamp) throws DatatypeConfigurationException
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
	
	private EbMSStatusResponse createEbMSStatusResponse(EbMSStatusRequest statusRequest, EbMSMessageStatus status) throws DatatypeConfigurationException
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

	public EbMSPong createEbMSPong(EbMSPing ping) throws DatatypeConfigurationException
	{
		return EbMSMessageUtils.ebMSPingToEbMSPong(ping,hostname);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
