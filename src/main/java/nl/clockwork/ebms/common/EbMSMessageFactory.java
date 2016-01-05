package nl.clockwork.ebms.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;
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
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SeverityType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusRequest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusResponse;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.To;
import org.w3._2000._09.xmldsig.ReferenceType;

public class EbMSMessageFactory
{
	private CPAManager cpaManager;

	public MessageHeader createMessageHeader(String cpaId, Party fromParty, Party toParty, String action)
	{
		String uuid = UUID.randomUUID().toString();
		EbMSPartyInfo fromPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,fromParty);
		EbMSPartyInfo toPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,toParty);
		DeliveryChannel deliveryChannel = (DeliveryChannel)fromPartyInfo.getDefaultMshChannelId();
		String hostname = CPAUtils.getHostname(deliveryChannel);

		MessageHeader messageHeader = new MessageHeader();

		messageHeader.setVersion(Constants.EBMS_VERSION);
		messageHeader.setMustUnderstand(true);

		messageHeader.setCPAId(cpaId);
		messageHeader.setConversationId(uuid);
		
		messageHeader.setFrom(new From());
		messageHeader.getFrom().getPartyId().addAll(fromPartyInfo.getPartyIds());
		messageHeader.getFrom().setRole(fromParty.getRole());

		messageHeader.setTo(new To());
		messageHeader.getTo().getPartyId().addAll(toPartyInfo.getPartyIds());
		messageHeader.getTo().setRole(toParty.getRole());
		
		messageHeader.setService(new Service());
		messageHeader.getService().setType(null);
		messageHeader.getService().setValue(Constants.EBMS_SERVICE_URI);
		messageHeader.setAction(action);

		messageHeader.setMessageData(new MessageData());
		messageHeader.getMessageData().setMessageId(uuid + "@" + hostname);
		//messageHeader.getMessageData().setRefToMessageId(null);
		messageHeader.getMessageData().setTimestamp(new Date());

		//setTimeToLive(cpa,deliveryChannel,messageHeader);

		//messageHeader.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(deliveryChannel.getMessagingCharacteristics().getDuplicateElimination()) ? "" : null);
		
		return messageHeader;
	}

	public MessageHeader createMessageHeader(String cpaId, EbMSMessageContext context) throws DatatypeConfigurationException
	{
		String uuid = context.getMessageId() == null ? UUID.randomUUID().toString() : context.getMessageId();
		FromPartyInfo fromPartyInfo = cpaManager.getFromPartyInfo(cpaId,context.getFromRole(),context.getService(),context.getAction());
		ToPartyInfo toPartyInfo = cpaManager.getToPartyInfoByFromPartyActionBinding(cpaId,context.getFromRole(),context.getService(),context.getAction());
		if (toPartyInfo == null)
			toPartyInfo = cpaManager.getToPartyInfo(cpaId,context.getToRole(),context.getService(),context.getAction());
		DeliveryChannel deliveryChannel = CPAUtils.getDeliveryChannel(fromPartyInfo.getCanSend().getThisPartyActionBinding());
		String hostname = CPAUtils.getHostname(deliveryChannel);

		MessageHeader messageHeader = new MessageHeader();

		messageHeader.setVersion(Constants.EBMS_VERSION);
		messageHeader.setMustUnderstand(true);

		messageHeader.setCPAId(cpaId);
		messageHeader.setConversationId(context.getConversationId() != null ? context.getConversationId() : uuid);
		
		messageHeader.setFrom(new From());
		messageHeader.getFrom().getPartyId().addAll(fromPartyInfo.getPartyIds());
		messageHeader.getFrom().setRole(fromPartyInfo.getRole());

		messageHeader.setTo(new To());
		messageHeader.getTo().getPartyId().addAll(toPartyInfo.getPartyIds());
		messageHeader.getTo().setRole(toPartyInfo.getRole());
		
		messageHeader.setService(new Service());
		messageHeader.getService().setType(fromPartyInfo.getService().getType());
		messageHeader.getService().setValue(fromPartyInfo.getService().getValue());
		messageHeader.setAction(fromPartyInfo.getCanSend().getThisPartyActionBinding().getAction());

		messageHeader.setMessageData(new MessageData());
		messageHeader.getMessageData().setMessageId(uuid + "@" + hostname);
		messageHeader.getMessageData().setRefToMessageId(context.getRefToMessageId());
		messageHeader.getMessageData().setTimestamp(new Date());

		setTimeToLive(deliveryChannel,messageHeader);

		messageHeader.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(deliveryChannel.getMessagingCharacteristics().getDuplicateElimination()) ? "" : null);
		
		return messageHeader;
	}

	public AckRequested createAckRequested(String cpaId, EbMSMessageContext context)
	{
		FromPartyInfo partyInfo = cpaManager.getFromPartyInfo(cpaId,context.getFromRole(),context.getService(),context.getAction());
		DeliveryChannel channel = CPAUtils.getDeliveryChannel(partyInfo.getCanSend().getThisPartyActionBinding());

		if (PerMessageCharacteristicsType.ALWAYS.equals(channel.getMessagingCharacteristics().getAckRequested()))
		{
			AckRequested ackRequested = new AckRequested();
			ackRequested.setVersion(Constants.EBMS_VERSION);
			ackRequested.setMustUnderstand(true);
			ackRequested.setSigned(PerMessageCharacteristicsType.ALWAYS.equals(channel.getMessagingCharacteristics().getAckSignatureRequested()));
			ackRequested.setActor(channel.getMessagingCharacteristics().getActor().value());
			return ackRequested;
		}
		else
			return null;
	}
	
	public SyncReply createSyncReply(String cpaId, Party fromParty)
	{
		return EbMSMessageUtils.createSyncReply(cpaManager.getEbMSPartyInfo(cpaId,fromParty).getDefaultMshChannelId());
	}
	
	public SyncReply createSyncReply(String cpaId, EbMSMessageContext context)
	{
		FromPartyInfo fromPartyInfo = cpaManager.getFromPartyInfo(cpaId,context.getFromRole(),context.getService(),context.getAction());
		return EbMSMessageUtils.createSyncReply(fromPartyInfo.getDeliveryChannel());
	}

	public EbMSMessage createEbMSMessageError(String cpaId, EbMSMessage message, ErrorList errorList, Date timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(cpaId,message.getMessageHeader(),timestamp,EbMSAction.MESSAGE_ERROR);
		if (errorList.getError().size() == 0)
		{
			errorList.getError().add(EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"An unknown error occurred!"));
			errorList.setHighestSeverity(SeverityType.ERROR);
		}
		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(messageHeader);
		result.setErrorList(errorList);
		return result;
	}

	public EbMSMessage createEbMSAcknowledgment(String cpaId, EbMSMessage message, Date timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(cpaId,message.getMessageHeader(),timestamp,EbMSAction.ACKNOWLEDGMENT);
		
		Acknowledgment acknowledgment = new Acknowledgment();

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
			for (ReferenceType reference : message.getSignature().getSignedInfo().getReference())
				acknowledgment.getReference().add(reference);

		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(messageHeader);
		result.setAcknowledgment(acknowledgment);
		return result;
	}
	
	public EbMSMessage createEbMSPing(String cpaId, Party fromParty, Party toParty) throws DatatypeConfigurationException, JAXBException
	{
		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(createMessageHeader(cpaId,fromParty,toParty,EbMSAction.PING.action()));
		result.setSyncReply(createSyncReply(cpaId,fromParty));
		return result;
	}
	
	public EbMSMessage createEbMSPong(String cpaId, EbMSMessage ping) throws DatatypeConfigurationException, JAXBException
	{
		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(createMessageHeader(cpaId,ping.getMessageHeader(),new Date(),EbMSAction.PONG));
		return result;
	}
	
	public EbMSMessage createEbMSStatusRequest(String cpaId, Party fromParty, Party toParty, String messageId) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(cpaId,fromParty,toParty,EbMSAction.STATUS_REQUEST.action());
		StatusRequest statusRequest = EbMSMessageUtils.createStatusRequest(messageId);
		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(messageHeader);
		result.setSyncReply(createSyncReply(cpaId,fromParty));
		result.setStatusRequest(statusRequest);
		return result;
	}

	public EbMSMessage createEbMSStatusResponse(String cpaId, EbMSMessage request, EbMSMessageStatus status, Date timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(cpaId,request.getMessageHeader(),new Date(),EbMSAction.STATUS_RESPONSE);
		StatusResponse statusResponse = createStatusResponse(request.getStatusRequest(),status,timestamp);
		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(messageHeader);
		result.setStatusResponse(statusResponse);
		return result;
	}

	public EbMSMessage ebMSMessageContentToEbMSMessage(String cpaId, EbMSMessageContent content) throws DatatypeConfigurationException
	{
		MessageHeader messageHeader = createMessageHeader(cpaId,content.getContext());
		AckRequested ackRequested = createAckRequested(cpaId,content.getContext());
		SyncReply syncReply = createSyncReply(cpaId,content.getContext());
		Manifest manifest = EbMSMessageUtils.createManifest();

		List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
		int i = 1;
		for (EbMSDataSource dataSource : content.getDataSources())
		{
			manifest.getReference().add(EbMSMessageUtils.createReference(i));
			ByteArrayDataSource ds = new ByteArrayDataSource(dataSource.getContent(),dataSource.getContentType());
			ds.setName(dataSource.getName());
			attachments.add(new EbMSAttachment(ds,"" + i));
			i++;
		}

		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(messageHeader);
		result.setAckRequested(ackRequested);
		result.setSyncReply(syncReply);
		result.setManifest(manifest);
		result.setAttachments(attachments);
		return result;
	}

	private void setTimeToLive(DeliveryChannel deliveryChannel, MessageHeader messageHeader) throws DatatypeConfigurationException
	{
		if (CPAUtils.isReliableMessaging(deliveryChannel))
		{
			Duration duration = CPAUtils.getPersistantDuration(deliveryChannel).add(CPAUtils.getRetryInterval(deliveryChannel));
			Date timestamp = (Date)messageHeader.getMessageData().getTimestamp().clone();
			duration.addTo(timestamp);
			messageHeader.getMessageData().setTimeToLive(timestamp);
		}
	}

	private MessageHeader createMessageHeader(String cpaId, MessageHeader messageHeader, Date timestamp, EbMSAction action) throws DatatypeConfigurationException, JAXBException
	{
		DeliveryChannel deliveryChannel = cpaManager.getDefaultDeliveryChannel(cpaId,messageHeader.getTo().getPartyId(),action.action());
		String hostname = CPAUtils.getHostname(deliveryChannel);

		MessageHeader result = XMLMessageBuilder.deepCopy(messageHeader);

		result.getFrom().getPartyId().clear();
		result.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
		result.getFrom().setRole(messageHeader.getTo().getRole());

		result.getTo().getPartyId().clear();
		result.getTo().getPartyId().addAll(messageHeader.getFrom().getPartyId());
		result.getTo().setRole(messageHeader.getFrom().getRole());

		result.getMessageData().setRefToMessageId(messageHeader.getMessageData().getMessageId());
		result.getMessageData().setMessageId(UUID.randomUUID().toString() + "@" + hostname);
		result.getMessageData().setTimestamp(timestamp);
		result.getMessageData().setTimeToLive(null);

		result.setService(new Service());
		result.getService().setValue(Constants.EBMS_SERVICE_URI);
		result.setAction(action.action());

		result.setDuplicateElimination(null);

		return result;
	}

	public StatusResponse createStatusResponse(StatusRequest statusRequest, EbMSMessageStatus status, Date timestamp) throws DatatypeConfigurationException
	{
		StatusResponse response = new StatusResponse();
		response.setVersion(Constants.EBMS_VERSION);
		response.setRefToMessageId(statusRequest.getRefToMessageId());
		if (status != null)
		{
			response.setMessageStatus(status.statusCode());
			if (MessageStatusType.RECEIVED.equals(status.statusCode()) || MessageStatusType.PROCESSED.equals(status.statusCode()))
				response.setTimestamp(timestamp);
		}
		return response;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}
}
