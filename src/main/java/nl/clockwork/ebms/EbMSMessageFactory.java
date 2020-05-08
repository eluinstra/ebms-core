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
package nl.clockwork.ebms;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActorType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PerMessageCharacteristicsType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.From;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageData;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SeverityType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusRequest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusResponse;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.To;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.service.model.EbMSDataSource;
import nl.clockwork.ebms.service.model.EbMSDataSourceMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageFactory
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSIdGenerator ebMSIdGenerator;

	public EbMSMessageError createEbMSMessageError(EbMSMessage message, ErrorList errorList, Instant timestamp) throws DatatypeConfigurationException, JAXBException
	{
		val messageHeader = createResponseMessageHeader(message.getMessageHeader(),timestamp,EbMSAction.MESSAGE_ERROR);
		if (errorList.getError().size() == 0)
		{
			errorList.getError().add(EbMSMessageUtils.createError(
					EbMSErrorCode.UNKNOWN.getErrorCode(),
					EbMSErrorCode.UNKNOWN,
					"An unknown error occurred!"));
			errorList.setHighestSeverity(SeverityType.ERROR);
		}
		return EbMSMessageError.builder()
				.messageHeader(messageHeader)
				.errorList(errorList)
				.build();
	}

	public EbMSAcknowledgment createEbMSAcknowledgment(EbMSMessage message, Instant timestamp) throws EbMSProcessorException
	{
		try
		{
			val messageHeader = createResponseMessageHeader(message.getMessageHeader(),timestamp,EbMSAction.ACKNOWLEDGMENT);
			val acknowledgment = new Acknowledgment();
			acknowledgment.setVersion(Constants.EBMS_VERSION);
			acknowledgment.setMustUnderstand(true);
			acknowledgment.setTimestamp(timestamp);
			acknowledgment.setRefToMessageId(messageHeader.getMessageData().getRefToMessageId());
			acknowledgment.setFrom(new From());
			acknowledgment.getFrom().getPartyId().addAll(messageHeader.getFrom().getPartyId());
			acknowledgment.getFrom().setRole(null);
			//TODO resolve actor from CPA
			acknowledgment.setActor(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value());
			if (message.getAckRequested().isSigned() && message.getSignature() != null)
				message.getSignature().getSignedInfo().getReference().forEach(r -> acknowledgment.getReference().add(r));
			return EbMSAcknowledgment.builder()
					.messageHeader(messageHeader)
					.acknowledgment(acknowledgment)
					.build();
		}
		catch (JAXBException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	public EbMSPing createEbMSPing(String cpaId, String fromPartyId, String toPartyId) throws EbMSProcessorException
	{
		try
		{
			return EbMSPing.builder()
					.messageHeader(createMessageHeader(cpaId,fromPartyId,toPartyId,EbMSAction.PING))
					.syncReply(createSyncReply(cpaId,fromPartyId,EbMSAction.PING.getAction()))
					.build();
		}
		catch (TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	public EbMSPong createEbMSPong(EbMSPing message) throws EbMSProcessorException
	{
		try
		{
			return EbMSPong.builder()
					.messageHeader(createResponseMessageHeader(message.getMessageHeader(),Instant.now(),EbMSAction.PONG))
					.build();
		}
		catch (JAXBException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	public EbMSStatusRequest createEbMSStatusRequest(String cpaId, String fromPartyId, String toPartyId, String messageId) throws EbMSProcessorException
	{
		try
		{
			return EbMSStatusRequest.builder()
					.messageHeader(createMessageHeader(cpaId,fromPartyId,toPartyId,EbMSAction.STATUS_REQUEST))
					.syncReply(createSyncReply(cpaId,fromPartyId,EbMSAction.STATUS_REQUEST.getAction()))
					.statusRequest(EbMSMessageUtils.createStatusRequest(messageId))
					.build();
		}
		catch (DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	public EbMSStatusResponse createEbMSStatusResponse(EbMSStatusRequest request, EbMSMessageStatus status, Instant timestamp) throws EbMSProcessorException
	{
		try
		{
			return EbMSStatusResponse.builder()
					.messageHeader(createResponseMessageHeader(request.getMessageHeader(),Instant.now(),EbMSAction.STATUS_RESPONSE))
					.statusResponse(createStatusResponse(request.getStatusRequest(),status,timestamp))
					.build();
		}
		catch (JAXBException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	public EbMSMessage createEbMSMessage(EbMSMessageContent content) throws EbMSProcessorException
	{
		try
		{
			val builder = EbMSMessage.builder()
					.messageHeader(createMessageHeader(content.getContext()))
					.ackRequested(createAckRequested(content.getContext()))
					.syncReply(createSyncReply(content.getContext()));
			if (content.getDataSources() != null && content.getDataSources().size() > 0)
			{
				val manifest = EbMSMessageUtils.createManifest();
				val attachments = content.getDataSources().stream()
						.map(ds -> createEbMSAttachment(manifest,ds))
						.collect(Collectors.toList());
				builder.manifest(manifest);
				builder.attachments(attachments);
			}
			return builder.build();
		}
		catch (DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private EbMSAttachment createEbMSAttachment(Manifest manifest, EbMSDataSource ds)
	{
		val contentId = ds.getContentId() == null ? ebMSIdGenerator.generateConversationId() : ds.getContentId();
		manifest.getReference().add(EbMSMessageUtils.createReference(contentId));
		return EbMSAttachmentFactory.createEbMSAttachment(ds.getName(),contentId,ds.getContentType(),ds.getContent());
	}

	public EbMSMessage createEbMSMessageMTOM(EbMSMessageContentMTOM content) throws EbMSProcessorException
	{
		try
		{
			val builder = EbMSMessage.builder()
					.messageHeader(createMessageHeader(content.getContext()))
					.ackRequested(createAckRequested(content.getContext()))
					.syncReply(createSyncReply(content.getContext()));
			if (content.getDataSources() != null && content.getDataSources().size() > 0)
			{
				val manifest = EbMSMessageUtils.createManifest();
				val attachments = content.getDataSources().stream()
						.map(ds -> createEbMSAttachmentMTOM(manifest,ds))
						.collect(Collectors.toList());
				builder.manifest(manifest);
				builder.attachments(attachments);
			}
			return builder.build();
		}
		catch (DatatypeConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private EbMSAttachment createEbMSAttachmentMTOM(Manifest manifest, EbMSDataSourceMTOM ds)
	{
		try
		{
			val contentId = ds.getContentId() == null ? ebMSIdGenerator.generateContentId() : ds.getContentId();
			manifest.getReference().add(EbMSMessageUtils.createReference(contentId));
			return EbMSAttachmentFactory.createCachedEbMSAttachment(contentId,ds.getAttachment());
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private MessageHeader createMessageHeader(String cpaId, String conversationId, From from, To to, Service service, String action, MessageData messageData, PerMessageCharacteristicsType duplicateElimination)
	{
		val result = new MessageHeader();
		result.setVersion(Constants.EBMS_VERSION);
		result.setMustUnderstand(true);
		result.setCPAId(cpaId);
		result.setConversationId(conversationId);
		result.setFrom(from);
		result.setTo(to);
		result.setService(service);
		result.setAction(action);
		result.setMessageData(messageData);
		result.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(duplicateElimination) ? "" : null);
		return result;
	}

	private MessageHeader createMessageHeader(String cpaId, String fromPartyId, String toPartyId, EbMSAction action)
	{
		val fromPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,fromPartyId)
				.orElseThrow(() -> StreamUtils.illegalStateException("EbMSPartyInfo",cpaId,fromPartyId));
		val toPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,toPartyId)
				.orElseThrow(() -> StreamUtils.illegalStateException("EbMSPartyInfo",cpaId,toPartyId));
		val partyId = new CacheablePartyId(fromPartyInfo.getPartyIds());
		val hostname = CPAUtils.getHostname(
				cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("DefaultDeliveryChannel",cpaId,partyId,action)));
		val conversationId = ebMSIdGenerator.generateConversationId();
		val from = createForm(fromPartyInfo.getPartyIds(),null);
		val to = createTo(toPartyInfo.getPartyIds(),null);
		val service = createService(null,action.getServiceUri());
		val messageId = ebMSIdGenerator.createMessageId(hostname,conversationId);
		val messageData = createMessageData(messageId,null,Instant.now(),null);
		return createMessageHeader(cpaId,conversationId,from,to,service,action.getAction(),messageData,null); //deliveryChannel.getMessagingCharacteristics().getDuplicateElimination()
	}

	private MessageHeader createMessageHeader(EbMSMessageContext context) throws DatatypeConfigurationException
	{
		val cpaId = context.getCpaId();
		val fromPartyInfo = cpaManager.getFromPartyInfo(cpaId,context.getFromParty(),context.getService(),context.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("FromPartyInfo",cpaId,context.getFromParty(),context.getService(),context.getAction()));
		val toPartyInfo = cpaManager.getToPartyInfoByFromPartyActionBinding(cpaId,context.getFromParty(),context.getService(),context.getAction())
				.orElse(cpaManager.getToPartyInfo(cpaId,context.getToParty(),context.getService(),context.getAction())
						.orElseThrow(() -> StreamUtils.illegalStateException("ToPartyInfo",cpaId,context.getToParty(),context.getService(),context.getAction())));
		val deliveryChannel = CPAUtils.getDeliveryChannel(fromPartyInfo.getCanSend().getThisPartyActionBinding());
		val hostname = CPAUtils.getHostname(deliveryChannel);
		val conversationId = context.getConversationId() == null ? ebMSIdGenerator.generateConversationId() : context.getConversationId();
		val from = createForm(fromPartyInfo.getPartyIds(),fromPartyInfo.getRole());
		val to = createTo(toPartyInfo.getPartyIds(),toPartyInfo.getRole());
		val service = createService(fromPartyInfo.getService().getType(),fromPartyInfo.getService().getValue());
		val action = fromPartyInfo.getCanSend().getThisPartyActionBinding().getAction();
		val messageId = ebMSIdGenerator.createMessageId(hostname,conversationId,context.getMessageId());
		val timestamp = Instant.now();
		val timeToLive = createTimeToLive(deliveryChannel,timestamp);
		val messageData = createMessageData(messageId,context.getRefToMessageId(),timestamp,timeToLive);
		return createMessageHeader(cpaId,conversationId,from,to,service,action,messageData,deliveryChannel.getMessagingCharacteristics().getDuplicateElimination());
	}

	private MessageHeader createResponseMessageHeader(MessageHeader messageHeader, Instant timestamp, EbMSAction action) throws DatatypeConfigurationException, JAXBException
	{
		val cpaId = messageHeader.getCPAId();
		val partyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
		val deliveryChannel = cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action.getAction()).orElse(null);
		val hostname = CPAUtils.getHostname(deliveryChannel);
		val from = createForm(messageHeader.getTo().getPartyId(),null);
		val to = createTo(messageHeader.getFrom().getPartyId(),null);
		val service = createService(null,action.getServiceUri());
		val messageId = ebMSIdGenerator.generateMessageId(hostname);
		val messageData = createMessageData(messageId,messageHeader.getMessageData().getMessageId(),timestamp,null);
		return createMessageHeader(messageHeader.getCPAId(),messageHeader.getConversationId(),from,to,service,action.getAction(),messageData,null);
	}

	private From createForm(Collection<? extends PartyId> partyIds, String role)
	{
		val result = new From();
		result.getPartyId().addAll(partyIds);
		result.setRole(role);
		return result;
	}

	private To createTo(List<PartyId> partyIds, String role)
	{
		val result = new To();
		result.getPartyId().addAll(partyIds);
		result.setRole(role);
		return result;
	}

	private Service createService(String type, String value)
	{
		val result = new Service();
		result.setType(type);
		result.setValue(value);
		return result;
	}

	private MessageData createMessageData(String messageId, String refToMessageId, Instant timestamp, Instant timeToLive)
	{
		val result = new MessageData();
		result.setMessageId(messageId);
		result.setRefToMessageId(refToMessageId);
		result.setTimestamp(timestamp);
		result.setTimeToLive(timeToLive);
		return result;
	}

	private Instant createTimeToLive(DeliveryChannel deliveryChannel, Instant date)
	{
		if (CPAUtils.isReliableMessaging(deliveryChannel))
		{
			val duration = CPAUtils.getSenderReliableMessaging(deliveryChannel)
					.getRetryInterval()
					.multipliedBy(CPAUtils.getSenderReliableMessaging(deliveryChannel).getRetries().intValue() + 1);
			return date.plus(duration);
		}
		else
			return null;
	}

	private AckRequested createAckRequested(EbMSMessageContext context)
	{
		val cpaId = context.getCpaId();
		val channel = cpaManager.getFromPartyInfo(cpaId,context.getFromParty(),context.getService(),context.getAction())
				.map(p -> CPAUtils.getDeliveryChannel(p.getCanSend().getThisPartyActionBinding()))
				.orElseThrow(() -> StreamUtils.illegalStateException("FromPartyInfo",cpaId,context.getFromParty(),context.getService(),context.getAction()));

		if (PerMessageCharacteristicsType.ALWAYS.equals(channel.getMessagingCharacteristics().getAckRequested()))
		{
			val result = new AckRequested();
			result.setVersion(Constants.EBMS_VERSION);
			result.setMustUnderstand(true);
			result.setSigned(PerMessageCharacteristicsType.ALWAYS.equals(channel.getMessagingCharacteristics().getAckSignatureRequested()));
			result.setActor(channel.getMessagingCharacteristics().getActor().value());
			return result;
		}
		else
			return null;
	}
	
	private SyncReply createSyncReply(String cpaId, String fromPartyId, String action)
	{
		val partyId = new CacheablePartyId(cpaManager.getEbMSPartyInfo(cpaId,fromPartyId)
				.orElseThrow(() -> StreamUtils.illegalStateException("EbMSPartyInfo",cpaId,fromPartyId)).getPartyIds());
		return EbMSMessageUtils.createSyncReply(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action)
				.orElseThrow(() -> StreamUtils.illegalStateException("DefaultDeliveryChannel",cpaId,partyId,action)));
	}
	
	private SyncReply createSyncReply(EbMSMessageContext context)
	{
		val cpaId = context.getCpaId();
		val partyId = new CacheablePartyId(cpaManager.getFromPartyInfo(cpaId,context.getFromParty(),context.getService(),context.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("FromPartyInfo",cpaId,context.getFromParty(),context.getService(),context.getAction()))
				.getPartyIds());
		return EbMSMessageUtils.createSyncReply(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,context.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("DefaultDeliveryChannel",cpaId,partyId,context.getAction())));
	}

	private StatusResponse createStatusResponse(StatusRequest statusRequest, EbMSMessageStatus status, Instant timestamp) throws DatatypeConfigurationException
	{
		val result = new StatusResponse();
		result.setVersion(Constants.EBMS_VERSION);
		result.setRefToMessageId(statusRequest.getRefToMessageId());
		if (status != null)
		{
			result.setMessageStatus(status.getStatusCode());
			if (MessageStatusType.RECEIVED.equals(status.getStatusCode()) || MessageStatusType.PROCESSED.equals(status.getStatusCode()))
				result.setTimestamp(timestamp);
		}
		return result;
	}
}
