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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSErrorCode;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class MessageHeaderValidator
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;

	public void validate(EbMSMessage message, Date timestamp) throws EbMSValidationException
	{
		val messageHeader = message.getMessageHeader();
		validateMessageHeader(messageHeader);
		val deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction())
				.orElseThrow(() -> new EbMSValidationException(EbMSMessageUtils.createError(EbMSErrorCode.UNKNOWN.getErrorCode(),EbMSErrorCode.UNKNOWN,"No DeliveryChannel found.")));
		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
			validateMessage(message,timestamp,deliveryChannel);
		if (Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()) && EbMSAction.ACKNOWLEDGMENT.getAction().equals(messageHeader.getAction()))
		{
			val acknowledgment = message.getAcknowledgment();
			validateAcknowledgment(acknowledgment,deliveryChannel);
		}
	}

	public void validate(EbMSMessage requestMessage, EbMSMessage responseMessage) throws ValidationException
	{
		if (!requestMessage.getMessageHeader().getCPAId().equals(responseMessage.getMessageHeader().getCPAId()))
			throw new ValidationException(
					"Request cpaId " + requestMessage.getMessageHeader().getCPAId() + " does not equal response cpaId " + responseMessage.getMessageHeader().getCPAId());
		if (!requestMessage.getMessageHeader().getMessageData().getMessageId().equals(responseMessage.getMessageHeader().getMessageData().getRefToMessageId()))
			throw new ValidationException(
					"Request messageId " + requestMessage.getMessageHeader().getMessageData().getMessageId() + " does not equal response refToMessageId " + responseMessage.getMessageHeader().getMessageData().getRefToMessageId());
		compare(requestMessage.getMessageHeader().getFrom().getPartyId(),responseMessage.getMessageHeader().getTo().getPartyId());
		compare(requestMessage.getMessageHeader().getTo().getPartyId(),responseMessage.getMessageHeader().getFrom().getPartyId());
	}
	
	private void validateMessageHeader(MessageHeader messageHeader)
	{
		validateVersion(messageHeader);
		validateFrom(messageHeader);
		validateTo(messageHeader);
		validateServiceAction(messageHeader);
	}

	private void validateVersion(MessageHeader messageHeader)
	{
		if (!Constants.EBMS_VERSION.equals(messageHeader.getVersion()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/@version",EbMSErrorCode.INCONSISTENT,"Invalid value."));
	}

	private void validateFrom(MessageHeader messageHeader)
	{
		if (!isValid(messageHeader.getFrom().getPartyId()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()) && StringUtils.isEmpty(messageHeader.getFrom().getRole()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/From/Role",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		val fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
		cpaManager.getPartyInfo(messageHeader.getCPAId(),fromPartyId).orElseThrow(() -> new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",EbMSErrorCode.INCONSISTENT,"Value not found.")));
	}

	private void validateTo(MessageHeader messageHeader)
	{
		if (!isValid(messageHeader.getTo().getPartyId()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()) && StringUtils.isEmpty(messageHeader.getTo().getRole()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/To/Role",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		val toPartyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
		cpaManager.getPartyInfo(messageHeader.getCPAId(),toPartyId).orElseThrow(() -> new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",EbMSErrorCode.INCONSISTENT,"Value not found.")));
	}

	private void validateServiceAction(MessageHeader messageHeader)
	{
		if (!isValid(messageHeader.getService()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/Service",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (StringUtils.isEmpty(messageHeader.getAction()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/Action",EbMSErrorCode.INCONSISTENT,"Invalid value."));
	}

	private void validateMessage(EbMSMessage message, Date timestamp, DeliveryChannel deliveryChannel)
	{
		val messageHeader = message.getMessageHeader();
		val service = CPAUtils.toString(messageHeader.getService());
		validateService(messageHeader,service);
		validateMessageData(timestamp,messageHeader);
		validateDuplicateElimination(deliveryChannel,messageHeader);
		val ackRequested = message.getAckRequested();
		validateAckRequested(deliveryChannel,ackRequested);
		val syncReply = message.getSyncReply();
		validateSyncReply(deliveryChannel,syncReply);
		val messageOrder = message.getMessageOrder();
		validateMessageOrder(messageOrder);
	}

	private void validateService(final org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader messageHeader, final java.lang.String service)
	{
		if (!cpaManager.canSend(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),service,messageHeader.getAction()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/Action",EbMSErrorCode.VALUE_NOT_RECOGNIZED,"Value not found."));
		if (!cpaManager.canReceive(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getTo().getPartyId()),messageHeader.getTo().getRole(),service,messageHeader.getAction()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/Action",EbMSErrorCode.VALUE_NOT_RECOGNIZED,"Value not found."));
	}

	private void validateMessageData(Date timestamp, final org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader messageHeader)
	{
		if (!existsRefToMessageId(messageHeader.getMessageData().getRefToMessageId()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/MessageData/RefToMessageId",EbMSErrorCode.VALUE_NOT_RECOGNIZED,"Value not found."));
		if (!checkTimeToLive(messageHeader,timestamp))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/MessageData/TimeToLive",EbMSErrorCode.TIME_TO_LIVE_EXPIRED,null));
	}

	private void validateDuplicateElimination(DeliveryChannel deliveryChannel, final org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader messageHeader)
	{
		if (!checkDuplicateElimination(deliveryChannel,messageHeader))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",EbMSErrorCode.INCONSISTENT,"Wrong value."));
	}

	private void validateAckRequested(final org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel deliveryChannel, final org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested ackRequested)
	{
		if (!checkAckRequested(deliveryChannel,ackRequested))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/AckRequested",EbMSErrorCode.INCONSISTENT,"Wrong value."));
		if (ackRequested != null && !Constants.EBMS_VERSION.equals(ackRequested.getVersion()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/AckRequested/@version",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (ackRequested != null && ackRequested.getActor() != null
				&& !ackRequested.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value()))
			if (ackRequested.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
				throw new EbMSValidationException(
						EbMSMessageUtils.createError("//Header/AckRequested/@actor",EbMSErrorCode.NOT_SUPPORTED,"NextMSH not supported."));
			else
				throw new EbMSValidationException(
						EbMSMessageUtils.createError("//Header/AckRequested/@actor",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!checkAckSignatureRequested(deliveryChannel,ackRequested))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/AckRequested/@signed",EbMSErrorCode.INCONSISTENT,"Wrong value."));
	}

	private void validateSyncReply(final org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel deliveryChannel, final org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply syncReply)
	{
		if (!checkSyncReply(deliveryChannel,syncReply))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/SyncReply",EbMSErrorCode.INCONSISTENT,"Wrong value."));
		if (syncReply != null && !Constants.EBMS_VERSION.equals(syncReply.getVersion()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/SyncReply/@version",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (syncReply != null && syncReply.getActor() != null && !syncReply.getActor().equals(Constants.NSURI_SOAP_NEXT_ACTOR))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/SyncReply/@actor",EbMSErrorCode.INCONSISTENT,"Wrong value."));
	}

	private void validateMessageOrder(MessageOrder messageOrder)
	{
		if (messageOrder != null)
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/MessageOrder",EbMSErrorCode.NOT_SUPPORTED,"MessageOrder not supported."));
//			if (messageOrder != null && !Constants.EBMS_VERSION.equals(messageOrder.getVersion()))
//				throw new EbMSValidationException(
//						EbMSMessageUtils.createError("//Header/MessageOrder/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
	}

	private void validateAcknowledgment(Acknowledgment acknowledgment, final org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel deliveryChannel)
	{
		if (acknowledgment != null && !Constants.EBMS_VERSION.equals(acknowledgment.getVersion()))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/Acknowledgment/@version",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!checkActor(deliveryChannel,acknowledgment))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/Acknowledgment/@actor",EbMSErrorCode.INCONSISTENT,"Wrong value."));
		if (acknowledgment.getActor() != null
				&& !acknowledgment.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value()))
			if (acknowledgment.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
				throw new EbMSValidationException(
						EbMSMessageUtils.createError("//Header/Acknowledgment/@actor",EbMSErrorCode.NOT_SUPPORTED,"NextMSH not supported."));
			else
				throw new EbMSValidationException(
						EbMSMessageUtils.createError("//Header/Acknowledgment/@actor",EbMSErrorCode.INCONSISTENT,"Invalid value."));
		if (!checkAckSignatureRequested(deliveryChannel,acknowledgment))
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("//Header/Acknowledgment/Reference",EbMSErrorCode.INCONSISTENT,"Wrong value."));
	}

	private boolean isValid(List<PartyId> partyIds)
	{
		return partyIds.stream().anyMatch(p -> !StringUtils.isEmpty(p.getType())|| isValidURI(p.getValue()));//FIXME replace by: org.apache.commons.validator.UrlValidator.isValid(partyId.getValue())
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
		return deliveryChannel.getMessagingCharacteristics().getDuplicateElimination() == null
				|| deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.NEVER) && messageHeader.getDuplicateElimination() == null)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.ALWAYS) && messageHeader.getDuplicateElimination() != null);
	}
	
	private boolean checkAckRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckRequested() == null
				|| deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.ALWAYS) && ackRequested != null)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.NEVER) && ackRequested == null);
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested() == null
				|| deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
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
		return ((deliveryChannel.getMessagingCharacteristics().getSyncReplyMode() == null
				|| deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(SyncReplyModeType.NONE)) && syncReply == null)
				|| (deliveryChannel.getMessagingCharacteristics().getSyncReplyMode() != null && !deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(SyncReplyModeType.NONE) && syncReply != null);
	}
	
	private boolean checkActor(DeliveryChannel deliveryChannel, Acknowledgment acknowledgment)
	{
		return (deliveryChannel.getMessagingCharacteristics().getActor() == null && acknowledgment.getActor() == null) 
				|| deliveryChannel.getMessagingCharacteristics().getActor().value().equals(acknowledgment.getActor());
	}

	private void compare(List<PartyId> requestPartyIds, List<PartyId> responsePartyIds) throws ValidationException
	{
		val anyMatch = requestPartyIds.stream()
				.map(req -> EbMSMessageUtils.toString(req))
				.anyMatch(req -> responsePartyIds.stream().map(res -> EbMSMessageUtils.toString(res)).anyMatch(res -> req.equals(res)));
		if (!anyMatch)
			throw new ValidationException("Request PartyIds do not match response PartyIds");
    
//		val request = new HashSet<PartyId>(requestPartyIds);
//		val response = new HashSet<PartyId>(responsePartyIds);
//		val allMatch = request.size() == response.size() && request.stream()
//				.map(req -> EbMSMessageUtils.toString(req))
//				.allMatch(req -> response.stream().map(res -> EbMSMessageUtils.toString(res)).anyMatch(res -> req.equals(res)));
//		if (!allMatch)
//			throw new ValidationException("Request PartyIds do not match response PartyIds");
	}
}
