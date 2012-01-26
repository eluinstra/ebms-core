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
package nl.clockwork.mule.ebms.util;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import nl.clockwork.common.util.XMLUtils;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.Constants.EbMSMessageType;
import nl.clockwork.mule.ebms.model.EbMSAction;
import nl.clockwork.mule.ebms.model.EbMSAttachment;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.model.EbMSPing;
import nl.clockwork.mule.ebms.model.EbMSPong;
import nl.clockwork.mule.ebms.model.EbMSStatusRequest;
import nl.clockwork.mule.ebms.model.EbMSStatusResponse;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.cpp.cpa.PerMessageCharacteristicsType;
import nl.clockwork.mule.ebms.model.cpp.cpa.ReliableMessaging;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Description;
import nl.clockwork.mule.ebms.model.ebxml.Error;
import nl.clockwork.mule.ebms.model.ebxml.From;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageData;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.MessageStatusType;
import nl.clockwork.mule.ebms.model.ebxml.PartyId;
import nl.clockwork.mule.ebms.model.ebxml.Reference;
import nl.clockwork.mule.ebms.model.ebxml.Service;
import nl.clockwork.mule.ebms.model.ebxml.SeverityType;
import nl.clockwork.mule.ebms.model.ebxml.StatusRequest;
import nl.clockwork.mule.ebms.model.ebxml.StatusResponse;
import nl.clockwork.mule.ebms.model.ebxml.To;

import org.apache.commons.io.IOUtils;
import org.mule.util.UUID;

public class EbMSMessageUtils
{
	public static MessageHeader createMessageHeader(CollaborationProtocolAgreement cpa, EbMSMessageContext context, String hostname) throws DatatypeConfigurationException
	{
		String uuid = UUID.getUUID();//UUID.randomUUID().toString();//nameUUIDFromBytes(hostname.getBytes()).toString();
		PartyInfo partyInfo = CPAUtils.getSendingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());
		PartyInfo otherPartyInfo = CPAUtils.getReceivingPartyInfo(cpa,context.getToRole(),context.getServiceType(),context.getService(),context.getAction());
		//PartyInfo otherPartyInfo = CPAUtils.getOtherReceivingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());

		MessageHeader messageHeader = new MessageHeader();

		messageHeader.setVersion(Constants.EBMS_VERSION);
		messageHeader.setMustUnderstand(true);

		messageHeader.setCPAId(cpa.getCpaid());
		messageHeader.setConversationId(context.getConversationId() != null ? context.getConversationId() : uuid);
		
		messageHeader.setFrom(new From());
		PartyId from = new PartyId();
		from.setType(partyInfo.getPartyId().get(0).getType());
		from.setValue(partyInfo.getPartyId().get(0).getValue());
		messageHeader.getFrom().getPartyId().add(from);
		messageHeader.getFrom().setRole(partyInfo.getCollaborationRole().get(0).getRole().getName());

		messageHeader.setTo(new To());
		PartyId to = new PartyId();
		to.setType(otherPartyInfo.getPartyId().get(0).getType());
		to.setValue(otherPartyInfo.getPartyId().get(0).getValue());
		messageHeader.getTo().getPartyId().add(to);
		messageHeader.getTo().setRole(otherPartyInfo.getCollaborationRole().get(0).getRole().getName());
		
		messageHeader.setService(new Service());
		messageHeader.getService().setType(partyInfo.getCollaborationRole().get(0).getServiceBinding().getService().getType());
		messageHeader.getService().setValue(partyInfo.getCollaborationRole().get(0).getServiceBinding().getService().getValue());
		messageHeader.setAction(partyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getThisPartyActionBinding().getAction());

		messageHeader.setMessageData(new MessageData());
		messageHeader.getMessageData().setMessageId(uuid + "@" + hostname);
		messageHeader.getMessageData().setRefToMessageId(context.getRefToMessageId());
		messageHeader.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

		ReliableMessaging rm = CPAUtils.getReliableMessaging(cpa,messageHeader);
		if (rm != null)
		{
			GregorianCalendar timestamp = messageHeader.getMessageData().getTimestamp().toGregorianCalendar();
			Duration d = rm.getRetryInterval().multiply(rm.getRetries().add(new BigInteger("1")).intValue());
			d.addTo(timestamp);
			timestamp.add(Calendar.SECOND,1);
			messageHeader.getMessageData().setTimeToLive(DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp));
		}

		DeliveryChannel channel = CPAUtils.getDeliveryChannel(partyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getThisPartyActionBinding());

		messageHeader.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(channel.getMessagingCharacteristics().getDuplicateElimination()) ? "" : null);
		
		return messageHeader;
	}

	public static MessageHeader createMessageHeader(MessageHeader messageHeader, String hostname, GregorianCalendar timestamp, EbMSAction action) throws DatatypeConfigurationException
	{
		messageHeader = (MessageHeader)XMLUtils.xmlToObject(XMLUtils.objectToXML(messageHeader)); //FIXME: replace by more efficient copy
		List<PartyId> partyIds = new ArrayList<PartyId>(messageHeader.getFrom().getPartyId());
		messageHeader.getFrom().getPartyId().clear();
		messageHeader.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
		messageHeader.getTo().getPartyId().clear();
		messageHeader.getTo().getPartyId().addAll(partyIds);

		messageHeader.getFrom().setRole(null);
		messageHeader.getTo().setRole(null);

		messageHeader.getMessageData().setRefToMessageId(messageHeader.getMessageData().getMessageId());
		messageHeader.getMessageData().setMessageId(UUID.getUUID() + "@" + hostname);
		messageHeader.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp));
		messageHeader.getMessageData().setTimeToLive(null);

		messageHeader.getService().setType(action.getService().getType());
		messageHeader.getService().setValue(action.getService().getValue());
		messageHeader.setAction(action.getAction());

		messageHeader.setDuplicateElimination(null);

		return messageHeader;
	}

	public static AckRequested createAckRequested(CollaborationProtocolAgreement cpa, EbMSMessageContext context)
	{
		PartyInfo partyInfo = CPAUtils.getSendingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());
		DeliveryChannel channel = CPAUtils.getDeliveryChannel(partyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getThisPartyActionBinding());

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
	
	public static Manifest createManifest()
	{
		Manifest manifest = new Manifest();
		manifest.setVersion(Constants.EBMS_VERSION);
		return manifest;
	}
	
	public static Error createError(String location, String errorCode, String description)
	{
		return createError(location,errorCode,description,"en-US",SeverityType.ERROR);
	}
	
	public static Error createError(String location, String errorCode, String description, SeverityType severity)
	{
		return createError(location,errorCode,description,"en-US",severity);
	}
	
	public static Error createError(String location, String errorCode, String description, String language, SeverityType severity)
	{
		Error error = new Error();
		error.setCodeContext(EbMSMessageType.SERVICE_MESSAGE.action().getService().getValue() + ":errors");
		error.setLocation(location);
		error.setErrorCode(errorCode);
		error.setDescription(new Description());
		error.getDescription().setLang(language);
		error.getDescription().setValue(description);
		error.setSeverity(severity);
		return error;
	}
	
	private static StatusResponse createStatusResponse(StatusRequest statusRequest, EbMSMessageStatus status, GregorianCalendar timestamp) throws DatatypeConfigurationException
	{
		StatusResponse response = new StatusResponse();
		response.setVersion(Constants.EBMS_VERSION);
		response.setRefToMessageId(statusRequest.getRefToMessageId());
		if (status != null)
		{
			response.setMessageStatus(status.statusCode());
			if (MessageStatusType.RECEIVED.equals(status.statusCode()) || MessageStatusType.PROCESSED.equals(status.statusCode()))
				response.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp));
		}
		return response;
	}

	public static EbMSPong ebMSPingToEbMSPong(EbMSPing ping, String hostname) throws DatatypeConfigurationException
	{
		EbMSPong pong = new EbMSPong(createMessageHeader(ping.getMessageHeader(),hostname,new GregorianCalendar(),EbMSMessageType.PONG.action()));
		return pong;
	}

	public static EbMSStatusResponse ebMSStatusRequestToEbMSStatusResponse(EbMSStatusRequest request, String hostname, EbMSMessageStatus status, GregorianCalendar timestamp) throws DatatypeConfigurationException
	{
		MessageHeader messageHeader = createMessageHeader(request.getMessageHeader(),hostname,new GregorianCalendar(),EbMSMessageType.STATUS_RESPONSE.action());
		StatusResponse statusResponse = createStatusResponse(request.getStatusRequest(),status,timestamp);
		EbMSStatusResponse response = new EbMSStatusResponse(messageHeader,statusResponse);
		return response;
	}

	public static EbMSMessage ebMSMessageContentToEbMSMessage(CollaborationProtocolAgreement cpa, EbMSMessageContent content, String hostname) throws DatatypeConfigurationException
	{
		MessageHeader messageHeader = createMessageHeader(cpa,content.getContext(),hostname);

		AckRequested ackRequested = createAckRequested(cpa,content.getContext());
		
		Manifest manifest = createManifest();
		for (int i = 0; i < content.getAttachments().size(); i++)
			manifest.getReference().add(createReference(i + 1));
		
		List<DataSource> attachments = new ArrayList<DataSource>();
		for (EbMSAttachment attachment : content.getAttachments())
		{
			ByteArrayDataSource ds = new ByteArrayDataSource(attachment.getContent(),attachment.getContentType());
			ds.setName(attachment.getName());
			attachments.add(ds);
		}

		return new EbMSMessage(messageHeader,ackRequested,manifest,attachments);
	}

	private static Reference createReference(int cid)
	{
		Reference reference = new Reference();
		reference.setHref("cid:" + cid);
		reference.setType("simple");
		//reference.setRole("XLinkRole");
		return reference;
	}

	public static EbMSMessageContent EbMSMessageToEbMSMessageContent(EbMSMessage message) throws IOException
	{
		List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
		for (DataSource attachment : message.getAttachments())
			attachments.add(new EbMSAttachment(attachment.getName(),attachment.getContentType(),IOUtils.toByteArray(attachment.getInputStream())));

		return new EbMSMessageContent(new EbMSMessageContext(message.getMessageHeader()),attachments);
	}

}
