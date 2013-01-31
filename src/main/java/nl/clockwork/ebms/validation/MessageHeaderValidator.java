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
package nl.clockwork.ebms.validation;

import java.net.URI;
import java.util.GregorianCalendar;
import java.util.List;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.PerMessageCharacteristicsType;
import nl.clockwork.ebms.model.cpp.cpa.SyncReplyModeType;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Error;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.PartyId;
import nl.clockwork.ebms.model.ebxml.Service;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MessageHeaderValidator
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private PerMessageCharacteristicsType ackRequested;// = PerMessageCharacteristicsType.ALWAYS;
	private PerMessageCharacteristicsType ackSignatureRequested;// = PerMessageCharacteristicsType.NEVER;
	private PerMessageCharacteristicsType duplicateElimination;// = PerMessageCharacteristicsType.ALWAYS;
	private SyncReplyModeType syncReplyMode;// = SyncReplyModeType.NONE;
	private EbMSDAO ebMSDAO;

	public MessageHeaderValidator(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public Error validate(CollaborationProtocolAgreement cpa, MessageHeader messageHeader, AckRequested ackRequested, SyncReply syncReply, MessageOrder messageOrder, GregorianCalendar timestamp)
	{
		//ErrorList result = new ErrorList();
		PartyInfo from = null;
		PartyInfo to = null;

		if (!isValid(messageHeader.getFrom().getPartyId()))
			return EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid.");
		if (!isValid(messageHeader.getTo().getPartyId()))
			return EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid.");
		if (!isValid(messageHeader.getService()))
			return EbMSMessageUtils.createError("//Header/MessageHeader/Service",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid.");
		
		if ((from = CPAUtils.getPartyInfo(cpa, messageHeader.getFrom().getPartyId())) == null)
			return EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value not found.");
		if ((to = CPAUtils.getPartyInfo(cpa, messageHeader.getTo().getPartyId())) == null)
			return EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value not found.");

		if (!CPAUtils.canSend(from,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction()))
			return EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"Value not found.");
		if (!CPAUtils.canReceive(to,messageHeader.getTo().getRole(),messageHeader.getService(),messageHeader.getAction()))
			return EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"Value not found.");

		List<DeliveryChannel> deliveryChannels = CPAUtils.getSendingDeliveryChannels(from,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
		if (deliveryChannels.size() == 0)
			return EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"No DeliveryChannel found.");
		if (deliveryChannels.size() > 1)
			return EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"Multiple DeliveryChannels not supported.");
		DeliveryChannel deliveryChannel = deliveryChannels.get(0);
		if (!existsRefToMessageId(messageHeader.getMessageData().getRefToMessageId()))
			return EbMSMessageUtils.createError("//Header/MessageHeader/MessageData/RefToMessageId",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"Value not found.");
		if (!checkTimeToLive(messageHeader,timestamp))
			return EbMSMessageUtils.createError("//Header/MessageHeader/MessageData/TimeToLive",Constants.EbMSErrorCode.TIME_TO_LIVE_EXPIRED.errorCode(),null);
		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
		{
			if (!checkDuplicateElimination(deliveryChannel))
				return EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"DuplicateElimination mode not supported.");
			if (!checkDuplicateElimination(deliveryChannel,messageHeader))
				return EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value.");

			if (!checkAckRequested(deliveryChannel))
				return EbMSMessageUtils.createError("//Header/AckRequested",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"AckRequested mode not supported.");
			if (!checkAckRequested(deliveryChannel,ackRequested))
				return EbMSMessageUtils.createError("//Header/AckRequested",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value.");
			if (ackRequested != null && !Constants.EBMS_VERSION.equals(ackRequested.getVersion()))
				return EbMSMessageUtils.createError("//Header/AckRequested[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value.");
			if (ackRequested != null && ackRequested.getActor() != null && ackRequested.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
				return EbMSMessageUtils.createError("//Header/AckRequested[@actor]",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"NextMSH not supported.");
			if (!checkAckSignatureRequested(deliveryChannel))
				return EbMSMessageUtils.createError("//Header/AckRequested[@signed]",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"Signed Acknowledgment mode not supported.");
			if (!checkAckSignatureRequested(deliveryChannel,ackRequested))
				return EbMSMessageUtils.createError("//Header/AckRequested[@signed]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value.");

			if (!checkSyncReply(deliveryChannel))
				return EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"SyncReply mode not supported.");
			if (!checkSyncReply(deliveryChannel,syncReply))
				return EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value.");
			if (syncReply != null && !Constants.EBMS_VERSION.equals(syncReply.getVersion()))
				return EbMSMessageUtils.createError("//Header/SyncReply[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value.");
			if (syncReply != null && syncReply.getActor() != null && !syncReply.getActor().equals("http://schemas.xmlsoap.org/soap/actor/next"))
				return EbMSMessageUtils.createError("//Header/SyncReply[@actor]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value.");

			if (messageOrder != null)
				return EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"MessageOrder not supported.");
		}
//		if (EbMSMessageType.ACKNOWLEDGMENT.action().getService().getValue().equals(messageHeader.getService().getValue()) && EbMSMessageType.ACKNOWLEDGMENT.action().getAction().equals(messageHeader.getAction()))
//		{
//			Acknowledgment acknowledgment = ((EbMSAcknowledgment)message.getPayload()).getAcknowledgment();
//			if (!checkActor(deliveryChannel,acknowledgment))
//				return EbMSMessageUtils.createError("//Header/Acknowledgment[@actor]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
//			if (acknowledgment.getActor() != null && acknowledgment.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
//				return EbMSMessageUtils.createError("//Header/Acknowledgment[@actor]",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"NextMSH not supported."));
//			if (!checkAckSignatureRequested(deliveryChannel,acknowledgment))
//				return EbMSMessageUtils.createError("//Header/Acknowledgment/Reference",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
//		}
		return null;
	}

	private boolean isValid(List<PartyId> partyIds)
	{
		for (PartyId partyId : partyIds)
			if (!StringUtils.isEmpty(partyId.getType()) || isValidURI(partyId.getValue())/*FIXME replace by: org.apache.commons.validator.UrlValidator.isValid(partyId.getValue())*/)
				return true;
		return false;
	}

	private boolean isValidURI(String s)
	{
		try
		{
			new URI(s);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private boolean isValid(Service service)
	{
		return !StringUtils.isEmpty(service.getType()) || isValidURI(service.getValue())/*FIXME replace by: org.apache.commons.validator.UrlValidator.isValid(service.getValue())*/; 
	}
	
	private boolean existsRefToMessageId(String refToMessageId)
	{
		return refToMessageId == null || ebMSDAO.existsMessage(refToMessageId);
	}
	
	private boolean checkTimeToLive(MessageHeader messageHeader, GregorianCalendar timestamp)
	{
		return messageHeader.getMessageData().getTimeToLive() == null
				|| timestamp.before(messageHeader.getMessageData().getTimeToLive().toGregorianCalendar());
	}
	
	private boolean checkDuplicateElimination(DeliveryChannel deliveryChannel, MessageHeader messageHeader)
	{
		return deliveryChannel.getMessagingCharacteristics().getDuplicateElimination() == null || deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.NEVER) && messageHeader.getDuplicateElimination() == null)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.ALWAYS) && messageHeader.getDuplicateElimination() != null);
	}
	
	private boolean checkDuplicateElimination(DeliveryChannel deliveryChannel)
	{
		return duplicateElimination == null || deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(duplicateElimination);
	}
	
	private boolean checkAckRequested(DeliveryChannel deliveryChannel)
	{
		return ackRequested == null || deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(ackRequested);
	}

	private boolean checkAckRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckRequested() == null || deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.ALWAYS) && ackRequested != null)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.NEVER) && ackRequested == null)
		;
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel)
	{
		return ackSignatureRequested == null || deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(ackSignatureRequested);
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.ALWAYS) && ackRequested.isSigned())
				|| deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.NEVER) && !ackRequested.isSigned());
	}

//	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel, Acknowledgment acknowledgment)
//	{
//		return (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.ALWAYS) && (acknowledgment.getReference() != null))
//		|| deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
//		|| (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.NEVER));
//	}

	private boolean checkSyncReply(DeliveryChannel deliveryChannel)
	{
		return syncReplyMode == null || deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(syncReplyMode);
	}
	
	private boolean checkSyncReply(DeliveryChannel deliveryChannel, SyncReply syncReply)
	{
		return !((deliveryChannel.getMessagingCharacteristics().getSyncReplyMode() == null || deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(SyncReplyModeType.NONE))
				&& syncReply != null);
	}
	
//	private boolean checkActor(DeliveryChannel deliveryChannel, Acknowledgment acknowledgment)
//	{
//		return (deliveryChannel.getMessagingCharacteristics().getActor() == null && acknowledgment.getActor() == null) 
//				|| deliveryChannel.getMessagingCharacteristics().getActor().value().equals(acknowledgment.getActor());
//	}

	public void setAckRequested(PerMessageCharacteristicsType ackRequested)
	{
		this.ackRequested = ackRequested;
	}
	
	public void setAckSignatureRequested(PerMessageCharacteristicsType ackSignatureRequested)
	{
		this.ackSignatureRequested = ackSignatureRequested;
	}
	
	public void setDuplicateElimination(PerMessageCharacteristicsType duplicateElimination)
	{
		this.duplicateElimination = duplicateElimination;
	}
	
	public void setSyncReplyMode(SyncReplyModeType syncReplyMode)
	{
		this.syncReplyMode = syncReplyMode;
	}
}
