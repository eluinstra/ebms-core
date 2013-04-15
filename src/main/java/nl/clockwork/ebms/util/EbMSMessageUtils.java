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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.ActorType;
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
import nl.clockwork.ebms.model.xml.dsig.ReferenceType;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.xml.EbMSNamespaceMapper;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSMessageUtils
{
	public static EbMSMessage getEbMSMessage(Document document, List<EbMSAttachment> attachments) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		XMLMessageBuilder<Envelope> messageBuilder = XMLMessageBuilder.getInstance(Envelope.class,Envelope.class,MessageHeader.class,SyncReply.class,MessageOrder.class,AckRequested.class,SignatureType.class,ErrorList.class,Acknowledgment.class,Manifest.class,StatusRequest.class,StatusResponse.class);
		Envelope envelope = messageBuilder.handle(document);
		return getEbMSMessage(envelope,attachments);
	}

	@SuppressWarnings("unchecked")
	public static EbMSMessage getEbMSMessage(Envelope envelope, List<EbMSAttachment> attachments)
	{
		
		SignatureType signature = null;
		MessageHeader messageHeader = null;
		SyncReply syncReply = null;
		MessageOrder messageOrder = null;
		AckRequested ackRequested = null;
		ErrorList errorList = null;
		Acknowledgment acknowledgment = null;
		for (Object element : envelope.getHeader().getAny())
			if (element instanceof JAXBElement && ((JAXBElement<?>)element).getValue() instanceof SignatureType)
				signature = ((JAXBElement<SignatureType>)element).getValue();
			else if (element instanceof MessageHeader)
				messageHeader = (MessageHeader)element;
			else if (element instanceof SyncReply)
				syncReply = (SyncReply)element;
			else if (element instanceof MessageOrder)
				messageOrder = (MessageOrder)element;
			else if (element instanceof AckRequested)
				ackRequested = (AckRequested)element;
			else if (element instanceof ErrorList)
				errorList = (ErrorList)element;
			else if (element instanceof Acknowledgment)
				acknowledgment = (Acknowledgment)element;

		Manifest manifest = null;
		StatusRequest statusRequest = null;
		StatusResponse statusResponse = null;
		for (Object element : envelope.getBody().getAny())
			if (element instanceof Manifest)
				manifest = (Manifest)element;
			else if (element instanceof StatusRequest)
				statusRequest = (StatusRequest)element;
			else if (element instanceof StatusResponse)
				statusResponse = (StatusResponse)element;

		return new EbMSMessage(signature,messageHeader,syncReply,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse,attachments);
	}
	
	public static EbMSDocument getEbMSDocument(EbMSMessage message) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		return new EbMSDocument(EbMSMessageUtils.createSOAPMessage(message),message.getAttachments());
	}
	
	public static MessageHeader createMessageHeader(CollaborationProtocolAgreement cpa, String fromParty, String toParty,String action) throws DatatypeConfigurationException
	{
		String uuid = UUID.randomUUID().toString();
		PartyInfo sendingPartyInfo = CPAUtils.getPartyInfo(cpa,fromParty);
		PartyInfo receivingPartyInfo = CPAUtils.getPartyInfo(cpa,toParty);
		//PartyInfo receivingPartyInfo = CPAUtils.getOtherReceivingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());
		DeliveryChannel deliveryChannel = CPAUtils.getDeliveryChannel(sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getThisPartyActionBinding());
		String hostname = CPAUtils.getHostname(deliveryChannel);

		MessageHeader messageHeader = new MessageHeader();

		messageHeader.setVersion(Constants.EBMS_VERSION);
		messageHeader.setMustUnderstand(true);

		messageHeader.setCPAId(cpa.getCpaid());
		messageHeader.setConversationId(uuid);
		
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
		messageHeader.getService().setType(null);
		messageHeader.getService().setValue(Constants.EBMS_SERVICE_URI);
		messageHeader.setAction(action);

		messageHeader.setMessageData(new MessageData());
		messageHeader.getMessageData().setMessageId(uuid + "@" + hostname);
		//messageHeader.getMessageData().setRefToMessageId(null);
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

		//messageHeader.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(deliveryChannel.getMessagingCharacteristics().getDuplicateElimination()) ? "" : null);
		
		return messageHeader;
	}

	public static MessageHeader createMessageHeader(CollaborationProtocolAgreement cpa, EbMSMessageContext context) throws DatatypeConfigurationException
	{
		String uuid = UUID.randomUUID().toString();
		PartyInfo sendingPartyInfo = CPAUtils.getSendingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());
		PartyInfo receivingPartyInfo = CPAUtils.getReceivingPartyInfo(cpa,context.getToRole(),context.getServiceType(),context.getService(),context.getAction());
		//PartyInfo receivingPartyInfo = CPAUtils.getOtherReceivingPartyInfo(cpa,context.getFromRole(),context.getServiceType(),context.getService(),context.getAction());
		DeliveryChannel deliveryChannel = CPAUtils.getDeliveryChannel(sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getThisPartyActionBinding());
		String hostname = CPAUtils.getHostname(deliveryChannel);

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

		messageHeader.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(deliveryChannel.getMessagingCharacteristics().getDuplicateElimination()) ? "" : null);
		
		return messageHeader;
	}

	public static MessageHeader createMessageHeader(CollaborationProtocolAgreement cpa, MessageHeader messageHeader, GregorianCalendar timestamp, EbMSAction action) throws DatatypeConfigurationException, JAXBException
	{
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getTo().getPartyId());
		DeliveryChannel deliveryChannel = CPAUtils.getDefaultDeliveryChannel(partyInfo);
		String hostname = CPAUtils.getHostname(deliveryChannel);

		MessageHeader result = XMLMessageBuilder.deepCopy(messageHeader);

		result.getFrom().getPartyId().clear();
		result.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
		result.getTo().getPartyId().clear();
		result.getTo().getPartyId().addAll(messageHeader.getFrom().getPartyId());

		result.getFrom().setRole(null);
		result.getTo().setRole(null);

		result.getMessageData().setRefToMessageId(messageHeader.getMessageData().getMessageId());
		result.getMessageData().setMessageId(UUID.randomUUID().toString() + "@" + hostname);
		result.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp));
		result.getMessageData().setTimeToLive(null);

		result.setService(new Service());
		result.getService().setValue(Constants.EBMS_SERVICE_URI);
		result.setAction(action.action());

		result.setDuplicateElimination(null);

		return result;
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
	
	public static ErrorList createErrorList()
	{
		ErrorList result = new ErrorList();
		result.setVersion(Constants.EBMS_VERSION);
		result.setMustUnderstand(true);
		result.setHighestSeverity(SeverityType.WARNING);
		return result;
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
		error.setCodeContext(Constants.EBMS_SERVICE_URI + ":errors");
		error.setLocation(location);
		error.setErrorCode(errorCode);
		error.setDescription(new Description());
		error.getDescription().setLang(language);
		error.getDescription().setValue(description);
		error.setSeverity(severity);
		return error;
	}
	
	public static StatusRequest createStatusRequest(String refToMessageId) throws DatatypeConfigurationException
	{
		StatusRequest request = new StatusRequest();
		request.setVersion(Constants.EBMS_VERSION);
		request.setRefToMessageId(refToMessageId);
		return request;
	}

	public static StatusResponse createStatusResponse(StatusRequest statusRequest, EbMSMessageStatus status, GregorianCalendar timestamp) throws DatatypeConfigurationException
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

	public static EbMSMessage createEbMSMessageError(CollaborationProtocolAgreement cpa, EbMSMessage message, ErrorList errorList, GregorianCalendar timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(cpa,message.getMessageHeader(),timestamp,EbMSAction.MESSAGE_ERROR);
		if (errorList.getError().size() == 0)
		{
			errorList.getError().add(EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"An unknown error occurred!"));
			errorList.setHighestSeverity(SeverityType.ERROR);
		}
		return new EbMSMessage(messageHeader,errorList);
	}

	public static EbMSMessage createEbMSAcknowledgment(CollaborationProtocolAgreement cpa, EbMSMessage message, GregorianCalendar timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(cpa,message.getMessageHeader(),timestamp,EbMSAction.ACKNOWLEDGMENT);
		
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
	
	public static EbMSMessage createEbMSPing(CollaborationProtocolAgreement cpa, String fromParty, String toParty) throws DatatypeConfigurationException, JAXBException
	{
		return new EbMSMessage(createMessageHeader(cpa,fromParty,toParty,EbMSAction.PING.action()));
	}
	
	public static EbMSMessage createEbMSPong(CollaborationProtocolAgreement cpa, EbMSMessage ping) throws DatatypeConfigurationException, JAXBException
	{
		return new EbMSMessage(createMessageHeader(cpa,ping.getMessageHeader(),new GregorianCalendar(),EbMSAction.PONG));
	}
	
	public static EbMSMessage createEbMSStatusRequest(CollaborationProtocolAgreement cpa, String fromParty, String toParty, String messageId) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(cpa,fromParty,toParty,EbMSAction.STATUS_REQUEST.action());
		StatusRequest statusRequest = createStatusRequest(messageId);
		EbMSMessage request = new EbMSMessage(messageHeader,statusRequest);
		return request;
	}

	public static EbMSMessage createEbMSStatusResponse(CollaborationProtocolAgreement cpa, EbMSMessage request, EbMSMessageStatus status, GregorianCalendar timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(cpa,request.getMessageHeader(),new GregorianCalendar(),EbMSAction.STATUS_RESPONSE);
		StatusResponse statusResponse = createStatusResponse(request.getStatusRequest(),status,timestamp);
		EbMSMessage response = new EbMSMessage(messageHeader,statusResponse);
		return response;
	}

	public static EbMSMessage ebMSMessageContentToEbMSMessage(CollaborationProtocolAgreement cpa, EbMSMessageContent content) throws DatatypeConfigurationException
	{
		MessageHeader messageHeader = createMessageHeader(cpa,content.getContext());

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

	public static Reference createReference(int cid)
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

	public static EbMSSendEvent getEbMSSendEvent(long id, MessageHeader messageHeader)
	{
		return new EbMSSendEvent(id,messageHeader.getMessageData().getTimestamp().toGregorianCalendar().getTime());
	}

	public static List<EbMSSendEvent> getEbMSSendEvents(CollaborationProtocolAgreement cpa, long id, MessageHeader messageHeader)
	{
		List<EbMSSendEvent> result = new ArrayList<EbMSSendEvent>();
		Date sendTime = messageHeader.getMessageData().getTimestamp().toGregorianCalendar().getTime();
		ReliableMessaging rm = CPAUtils.getReliableMessaging(cpa,messageHeader);
		if (rm != null)
			for (int i = 0; i < rm.getRetries().intValue() + 1; i++)
			{
				result.add(new EbMSSendEvent(id,(Date)sendTime.clone()));
				rm.getRetryInterval().addTo(sendTime);
			}
		return result;
	}

	public static Document createSOAPMessage(EbMSMessage ebMSMessage) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
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
		
		DocumentBuilder db = DOMUtils.getDocumentBuilder();
		XMLMessageBuilder<Envelope> messageBuilder = XMLMessageBuilder.getInstance(Envelope.class,Envelope.class,MessageHeader.class,SyncReply.class,MessageOrder.class,AckRequested.class,SignatureType.class,ErrorList.class,Acknowledgment.class,Manifest.class,StatusRequest.class,StatusResponse.class);

		//Document d = db.parse(new ByteArrayInputStream(messageBuilder.handle(new JAXBElement<Envelope>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope)).getBytes()));

		Document d = db.parse(new ByteArrayInputStream(messageBuilder.handle(new JAXBElement<Envelope>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope),new EbMSNamespaceMapper()).getBytes()));

		return d;
	}

}
