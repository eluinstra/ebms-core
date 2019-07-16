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
package nl.clockwork.ebms.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActorType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PerMessageCharacteristicsType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageOrder;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;

public class MessageHeaderValidator
{
	protected EbMSDAO ebMSDAO;
	protected CPAManager cpaManager;

	public void validate(EbMSMessage message, Date timestamp) throws EbMSValidationException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		AckRequested ackRequested = message.getAckRequested();
		SyncReply syncReply = message.getSyncReply();
		MessageOrder messageOrder = message.getMessageOrder();
		
		if (!Constants.EBMS_VERSION.equals(messageHeader.getVersion()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!isValid(messageHeader.getFrom().getPartyId()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()) && StringUtils.isEmpty(messageHeader.getFrom().getRole()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/From/Role",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!isValid(messageHeader.getTo().getPartyId()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()) && StringUtils.isEmpty(messageHeader.getTo().getRole()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/To/Role",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!isValid(messageHeader.getService()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/Service",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (StringUtils.isEmpty(messageHeader.getAction()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		
		if (cpaManager.getPartyInfo(message.getMessageHeader().getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId())) == null)
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT,"Value not found."));
		if (cpaManager.getPartyInfo(message.getMessageHeader().getCPAId(),new CacheablePartyId(messageHeader.getTo().getPartyId())) == null)
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT,"Value not found."));

		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
		{
			if (!cpaManager.canSend(message.getMessageHeader().getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED,"Value not found."));
			if (!cpaManager.canReceive(message.getMessageHeader().getCPAId(),new CacheablePartyId(messageHeader.getTo().getPartyId()),messageHeader.getTo().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED,"Value not found."));
		}

		DeliveryChannel deliveryChannel = cpaManager.getSendDeliveryChannel(message.getMessageHeader().getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction());
		if (deliveryChannel == null)
			throw new EbMSValidationException(EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN,"No DeliveryChannel found."));
		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
		{
			if (!existsRefToMessageId(messageHeader.getMessageData().getRefToMessageId()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/MessageData/RefToMessageId",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED,"Value not found."));
			if (!checkTimeToLive(messageHeader,timestamp))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/MessageData/TimeToLive",Constants.EbMSErrorCode.TIME_TO_LIVE_EXPIRED,null));
			if (!checkDuplicateElimination(deliveryChannel,messageHeader))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",Constants.EbMSErrorCode.INCONSISTENT,"Wrong value."));

			if (!checkAckRequested(deliveryChannel,ackRequested))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/AckRequested",Constants.EbMSErrorCode.INCONSISTENT,"Wrong value."));
			if (ackRequested != null && !Constants.EBMS_VERSION.equals(ackRequested.getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/AckRequested/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			if (ackRequested != null && ackRequested.getActor() != null && !ackRequested.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value()))
				if (ackRequested.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/AckRequested/@actor",Constants.EbMSErrorCode.NOT_SUPPORTED,"NextMSH not supported."));
				else
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/AckRequested/@actor",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			if (!checkAckSignatureRequested(deliveryChannel,ackRequested))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/AckRequested/@signed",Constants.EbMSErrorCode.INCONSISTENT,"Wrong value."));

			if (!checkSyncReply(deliveryChannel,syncReply))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.INCONSISTENT,"Wrong value."));
			if (syncReply != null && !Constants.EBMS_VERSION.equals(syncReply.getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/SyncReply/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			if (syncReply != null && syncReply.getActor() != null && !syncReply.getActor().equals(Constants.NSURI_SOAP_NEXT_ACTOR))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/SyncReply/@actor",Constants.EbMSErrorCode.INCONSISTENT,"Wrong value."));

			if (messageOrder != null)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.NOT_SUPPORTED,"MessageOrder not supported."));
//			if (messageOrder != null && !Constants.EBMS_VERSION.equals(messageOrder.getVersion()))
//				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
		}
		if (Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()) && EbMSAction.ACKNOWLEDGMENT.action().equals(messageHeader.getAction()))
		{
			Acknowledgment acknowledgment = message.getAcknowledgment();
			if (acknowledgment != null && !Constants.EBMS_VERSION.equals(acknowledgment.getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Acknowledgment/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			if (!checkActor(deliveryChannel,acknowledgment))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Acknowledgment/@actor",Constants.EbMSErrorCode.INCONSISTENT,"Wrong value."));
			if (acknowledgment.getActor() != null && !acknowledgment.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value()))
				if (acknowledgment.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Acknowledgment/@actor",Constants.EbMSErrorCode.NOT_SUPPORTED,"NextMSH not supported."));
				else
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Acknowledgment/@actor",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			if (!checkAckSignatureRequested(deliveryChannel,acknowledgment))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Acknowledgment/Reference",Constants.EbMSErrorCode.INCONSISTENT,"Wrong value."));
		}
	}

	public void validate(EbMSMessage requestMessage, EbMSMessage responseMessage) throws ValidationException
	{
		if (!requestMessage.getMessageHeader().getCPAId().equals(responseMessage.getMessageHeader().getCPAId()))
			throw new ValidationException("Request cpaId " + requestMessage.getMessageHeader().getCPAId() + " does not equal response cpaId " + responseMessage.getMessageHeader().getCPAId());
		if (!requestMessage.getMessageHeader().getMessageData().getMessageId().equals(responseMessage.getMessageHeader().getMessageData().getRefToMessageId()))
			throw new ValidationException("Request messageId " + requestMessage.getMessageHeader().getMessageData().getMessageId() + " does not equal response refToMessageId " + responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
		compare(requestMessage.getMessageHeader().getFrom().getPartyId(),responseMessage.getMessageHeader().getTo().getPartyId());
		compare(requestMessage.getMessageHeader().getTo().getPartyId(),responseMessage.getMessageHeader().getFrom().getPartyId());
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
		catch (URISyntaxException e)
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
	
	private boolean checkTimeToLive(MessageHeader messageHeader, Date timestamp)
	{
		return messageHeader.getMessageData().getTimeToLive() == null
				|| timestamp.before(messageHeader.getMessageData().getTimeToLive());
	}
	
	private boolean checkDuplicateElimination(DeliveryChannel deliveryChannel, MessageHeader messageHeader)
	{
		return deliveryChannel.getMessagingCharacteristics().getDuplicateElimination() == null || deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.NEVER) && messageHeader.getDuplicateElimination() == null)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.ALWAYS) && messageHeader.getDuplicateElimination() != null);
	}
	
	private boolean checkAckRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckRequested() == null || deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.ALWAYS) && ackRequested != null)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.NEVER) && ackRequested == null);
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested() == null || deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.ALWAYS) && (ackRequested == null || ackRequested.isSigned()))
				|| (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.NEVER) && (ackRequested == null || !ackRequested.isSigned()));
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel, Acknowledgment acknowledgment)
	{
		return (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.ALWAYS) && (acknowledgment.getReference() != null))
				|| deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.NEVER));
	}

	private boolean checkSyncReply(DeliveryChannel deliveryChannel, SyncReply syncReply)
	{
		return ((deliveryChannel.getMessagingCharacteristics().getSyncReplyMode() == null || deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(SyncReplyModeType.NONE)) && syncReply == null)
				|| (deliveryChannel.getMessagingCharacteristics().getSyncReplyMode() != null && !deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(SyncReplyModeType.NONE) && syncReply != null);
	}
	
	private boolean checkActor(DeliveryChannel deliveryChannel, Acknowledgment acknowledgment)
	{
		return (deliveryChannel.getMessagingCharacteristics().getActor() == null && acknowledgment.getActor() == null) 
				|| deliveryChannel.getMessagingCharacteristics().getActor().value().equals(acknowledgment.getActor());
	}

	private void compare(List<PartyId> requestPartyIds, List<PartyId> responsePartyIds) throws ValidationException
	{
		for (PartyId requestPartyId : requestPartyIds)
			for (PartyId responsePartyId : responsePartyIds)
				if (EbMSMessageUtils.toString(requestPartyId).equals(EbMSMessageUtils.toString(responsePartyId)))
					return;
		throw new ValidationException("Request PartyIds do not match response PartyIds");
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}
}
