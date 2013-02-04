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
package nl.clockwork.ebms.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.PerMessageCharacteristicsType;
import nl.clockwork.ebms.model.cpp.cpa.ReliableMessaging;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.Description;
import nl.clockwork.ebms.model.ebxml.Error;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.From;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageData;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.MessageStatusType;
import nl.clockwork.ebms.model.ebxml.PartyId;
import nl.clockwork.ebms.model.ebxml.Reference;
import nl.clockwork.ebms.model.ebxml.Service;
import nl.clockwork.ebms.model.ebxml.SeverityType;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.ebxml.To;
import nl.clockwork.ebms.model.soap.envelope.Body;
import nl.clockwork.ebms.model.soap.envelope.Envelope;
import nl.clockwork.ebms.model.soap.envelope.Header;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSMessageUtils
{
	public static MessageHeader createMessageHeader(CollaborationProtocolAgreement cpa, EbMSMessageContext context, String hostname) throws DatatypeConfigurationException
	{
		String uuid = UUID.randomUUID().toString();
		PartyInfo sendingPartyInfo = CPAUtils.getSendingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());
		PartyInfo receivingPartyInfo = CPAUtils.getReceivingPartyInfo(cpa,context.getToRole(),context.getServiceType(),context.getService(),context.getAction());
		//PartyInfo receivingPartyInfo = CPAUtils.getOtherReceivingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());

		MessageHeader messageHeader = new MessageHeader();

		messageHeader.setVersion(Constants.EBMS_VERSION);
		messageHeader.setMustUnderstand(true);

		messageHeader.setCPAId(cpa.getCpaid());
		messageHeader.setConversationId(context.getConversationId() != null ? context.getConversationId() : uuid);
		
		messageHeader.setFrom(new From());
		PartyId from = new PartyId();
		from.setType(sendingPartyInfo.getPartyId().get(0).getType());
		from.setValue(sendingPartyInfo.getPartyId().get(0).getValue());
		messageHeader.getFrom().getPartyId().add(from);
		messageHeader.getFrom().setRole(sendingPartyInfo.getCollaborationRole().get(0).getRole().getName());

		messageHeader.setTo(new To());
		PartyId to = new PartyId();
		to.setType(receivingPartyInfo.getPartyId().get(0).getType());
		to.setValue(receivingPartyInfo.getPartyId().get(0).getValue());
		messageHeader.getTo().getPartyId().add(to);
		messageHeader.getTo().setRole(receivingPartyInfo.getCollaborationRole().get(0).getRole().getName());
		
		messageHeader.setService(new Service());
		messageHeader.getService().setType(sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getService().getType());
		messageHeader.getService().setValue(sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getService().getValue());
		messageHeader.setAction(sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getThisPartyActionBinding().getAction());

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

		DeliveryChannel channel = CPAUtils.getDeliveryChannel(sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getThisPartyActionBinding());

		messageHeader.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(channel.getMessagingCharacteristics().getDuplicateElimination()) ? "" : null);
		
		return messageHeader;
	}

	public static MessageHeader createMessageHeader(MessageHeader messageHeader, String hostname, GregorianCalendar timestamp, EbMSAction action) throws DatatypeConfigurationException, JAXBException
	{
		//TODO: optimize
		XMLMessageBuilder<MessageHeader> builder = XMLMessageBuilder.getInstance(MessageHeader.class);
		messageHeader = builder.handle(builder.handle(messageHeader));
		List<PartyId> partyIds = new ArrayList<PartyId>(messageHeader.getFrom().getPartyId());
		messageHeader.getFrom().getPartyId().clear();
		messageHeader.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
		messageHeader.getTo().getPartyId().clear();
		messageHeader.getTo().getPartyId().addAll(partyIds);

		messageHeader.getFrom().setRole(null);
		messageHeader.getTo().setRole(null);

		messageHeader.getMessageData().setRefToMessageId(messageHeader.getMessageData().getMessageId());
		messageHeader.getMessageData().setMessageId(UUID.randomUUID().toString() + "@" + hostname);
		messageHeader.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp));
		messageHeader.getMessageData().setTimeToLive(null);

		messageHeader.setService(new Service());
		messageHeader.getService().setValue(Constants.EBMS_SERVICE_URI);
		messageHeader.setAction(action.action());

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
		return createError(location,errorCode,description,Constants.EBMS_DEFAULT_LANGUAGE,SeverityType.ERROR);
	}
	
	public static Error createError(String location, String errorCode, String description, SeverityType severity)
	{
		return createError(location,errorCode,description,Constants.EBMS_DEFAULT_LANGUAGE,severity);
	}
	
	public static Error createError(String location, String errorCode, String description, String language, SeverityType severity)
	{
		Error error = new Error();
		error.setCodeContext(Constants.EBMS_ERROR_CODE_CONTEXT);
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

	public static EbMSMessage ebMSPingToEbMSPong(EbMSMessage ping, String hostname) throws DatatypeConfigurationException, JAXBException
	{
		EbMSMessage pong = new EbMSMessage(createMessageHeader(ping.getMessageHeader(),hostname,new GregorianCalendar(),EbMSAction.PONG));
		return pong;
	}

	public static EbMSMessage ebMSStatusRequestToEbMSStatusResponse(EbMSMessage request, String hostname, EbMSMessageStatus status, GregorianCalendar timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(request.getMessageHeader(),hostname,new GregorianCalendar(),EbMSAction.STATUS_RESPONSE);
		StatusResponse statusResponse = createStatusResponse(request.getStatusRequest(),status,timestamp);
		EbMSMessage response = new EbMSMessage(messageHeader,statusResponse);
		return response;
	}

	public static EbMSMessage ebMSMessageContentToEbMSMessage(CollaborationProtocolAgreement cpa, EbMSMessageContent content, String hostname) throws DatatypeConfigurationException
	{
		MessageHeader messageHeader = createMessageHeader(cpa,content.getContext(),hostname);

		AckRequested ackRequested = createAckRequested(cpa,content.getContext());
		
		Manifest manifest = createManifest();
		List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
		int i = 1;
		for (EbMSDataSource dataSource : content.getDataSources())
		{
			manifest.getReference().add(createReference(i));
			ByteArrayDataSource ds = new ByteArrayDataSource(dataSource.getContent(),dataSource.getContentType());
			ds.setName(dataSource.getName());
			attachments.add(new EbMSAttachment(ds,"" + i));
			i++;
		}

		return new EbMSMessage(messageHeader,ackRequested,manifest,attachments);
	}

	private static Reference createReference(int cid)
	{
		Reference reference = new Reference();
		reference.setHref(Constants.CID + cid);
		reference.setType("simple");
		//reference.setRole("XLinkRole");
		return reference;
	}

	public static EbMSMessageContent EbMSMessageToEbMSMessageContent(EbMSMessage message) throws IOException
	{
		List<EbMSDataSource> dataSources = new ArrayList<EbMSDataSource>();
		for (DataSource dataSource : message.getAttachments())
			dataSources.add(new EbMSDataSource(dataSource.getName(),dataSource.getContentType(),IOUtils.toByteArray(dataSource.getInputStream())));
		return new EbMSMessageContent(new EbMSMessageContext(message.getMessageHeader()),dataSources);
	}

	public static EbMSSendEvent getEbMSSendEvent(CollaborationProtocolAgreement cpa, MessageHeader messageHeader)
	{
		return new EbMSSendEvent(messageHeader.getMessageData().getTimestamp().toGregorianCalendar().getTime());
	}

	public static List<EbMSSendEvent> getEbMSSendEvents(CollaborationProtocolAgreement cpa, MessageHeader messageHeader)
	{
		List<EbMSSendEvent> result = new ArrayList<EbMSSendEvent>();
		Date sendTime = messageHeader.getMessageData().getTimestamp().toGregorianCalendar().getTime();
		ReliableMessaging rm = CPAUtils.getReliableMessaging(cpa,messageHeader);
		if (rm != null)
			for (int i = 0; i < rm.getRetries().intValue() + 1; i++)
			{
				result.add(new EbMSSendEvent((Date)sendTime.clone()));
				rm.getRetryInterval().addTo(sendTime);
			}
		return result;
	}

	public static Document createSOAPMessage(EbMSMessage ebMSMessage) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException
	{
		Envelope envelope = new Envelope();
		envelope.setHeader(new Header());
		envelope.setBody(new Body());
		
		envelope.getHeader().getAny().add(ebMSMessage.getMessageHeader());
		envelope.getHeader().getAny().add(ebMSMessage.getSyncReply());
		envelope.getHeader().getAny().add(ebMSMessage.getMessageOrder());
		envelope.getHeader().getAny().add(ebMSMessage.getAckRequested());
		envelope.getHeader().getAny().add(ebMSMessage.getSignature());
		envelope.getHeader().getAny().add(ebMSMessage.getErrorList());
		envelope.getHeader().getAny().add(ebMSMessage.getAcknowledgment());
		envelope.getBody().getAny().add(ebMSMessage.getManifest());
		envelope.getBody().getAny().add(ebMSMessage.getStatusRequest());
		envelope.getBody().getAny().add(ebMSMessage.getStatusResponse());
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		XMLMessageBuilder<Envelope> messageBuilder = XMLMessageBuilder.getInstance(Envelope.class,Envelope.class,MessageHeader.class,SyncReply.class,MessageOrder.class,AckRequested.class,SignatureType.class,ErrorList.class,Acknowledgment.class,Manifest.class,StatusRequest.class,StatusResponse.class);
		Document d = db.parse(new ByteArrayInputStream(messageBuilder.handle(new JAXBElement<Envelope>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope)).getBytes()));
		return d;
	}

}
