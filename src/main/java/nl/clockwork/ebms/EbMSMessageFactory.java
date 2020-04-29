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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
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
import org.xml.sax.SAXException;

import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSDataSourceMTOM;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

public class EbMSMessageFactory
{
	private CPAManager cpaManager;
	private EbMSIdGenerator ebMSIdGenerator;

	public EbMSMessage createEbMSMessageError(EbMSMessage message, ErrorList errorList, Date timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createResponseMessageHeader(message.getMessageHeader(),timestamp,EbMSAction.MESSAGE_ERROR);
		if (errorList.getError().size() == 0)
		{
			errorList.getError().add(EbMSMessageUtils.createError(
					EbMSErrorCode.UNKNOWN.errorCode(),
					EbMSErrorCode.UNKNOWN,
					"An unknown error occurred!"));
			errorList.setHighestSeverity(SeverityType.ERROR);
		}
		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(messageHeader);
		result.setErrorList(errorList);
		return result;
	}

	public EbMSMessage createEbMSAcknowledgment(EbMSMessage message, Date timestamp) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = createResponseMessageHeader(message.getMessageHeader(),timestamp,EbMSAction.ACKNOWLEDGMENT);
			
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
				message.getSignature().getSignedInfo().getReference().forEach(r -> acknowledgment.getReference().add(r));

			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(messageHeader);
			result.setAcknowledgment(acknowledgment);
			result.setMessage(EbMSMessageUtils.createSOAPMessage(result));
			return result;
		}
		catch (JAXBException | SOAPException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | ParserConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	public EbMSMessage createEbMSPing(String cpaId, Party fromParty, Party toParty) throws EbMSProcessorException
	{
		try
		{
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(createMessageHeader(cpaId,fromParty,toParty,EbMSAction.PING));
			result.setSyncReply(createSyncReply(cpaId,fromParty,EbMSAction.PING.action()));
			result.setMessage(EbMSMessageUtils.createSOAPMessage(result));
			return result;
		}
		catch (JAXBException | SOAPException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	public EbMSMessage createEbMSPong(EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(createResponseMessageHeader(message.getMessageHeader(),new Date(),EbMSAction.PONG));
			result.setMessage(EbMSMessageUtils.createSOAPMessage(result));
			return result;
		}
		catch (JAXBException | SOAPException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | ParserConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	public EbMSMessage createEbMSStatusRequest(String cpaId, Party fromParty, Party toParty, String messageId) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = createMessageHeader(cpaId,fromParty,toParty,EbMSAction.STATUS_REQUEST);
			StatusRequest statusRequest = EbMSMessageUtils.createStatusRequest(messageId);
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(messageHeader);
			result.setSyncReply(createSyncReply(cpaId,fromParty,EbMSAction.STATUS_REQUEST.action()));
			result.setStatusRequest(statusRequest);
			result.setMessage(EbMSMessageUtils.createSOAPMessage(result));
			return result;
		}
		catch (JAXBException | SOAPException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | ParserConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	public EbMSMessage createEbMSStatusResponse(EbMSMessage request, EbMSMessageStatus status, Date timestamp) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = createResponseMessageHeader(request.getMessageHeader(),new Date(),EbMSAction.STATUS_RESPONSE);
			StatusResponse statusResponse = createStatusResponse(request.getStatusRequest(),status,timestamp);
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(messageHeader);
			result.setStatusResponse(statusResponse);
			result.setMessage(EbMSMessageUtils.createSOAPMessage(result));
			return result;
		}
		catch (JAXBException | SOAPException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | ParserConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	public EbMSMessage createEbMSMessage(EbMSMessageContent content) throws EbMSProcessorException
	{
		try
		{
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(createMessageHeader(content.getContext()));
			result.setAckRequested(createAckRequested(content.getContext()));
			result.setSyncReply(createSyncReply(content.getContext()));
			if (content.getDataSources() != null && content.getDataSources().size() > 0)
			{
				Manifest manifest = EbMSMessageUtils.createManifest();
				List<EbMSAttachment> attachments = new ArrayList<>();
				content.getDataSources().forEach(ds -> attachments.add(createEbMSAttachment(manifest,ds)));
				result.setManifest(manifest);
				result.setAttachments(attachments);
			}
			result.setMessage(EbMSMessageUtils.createSOAPMessage(result));
			return result;
		}
		catch (JAXBException | SOAPException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | ParserConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private EbMSAttachment createEbMSAttachment(Manifest manifest, EbMSDataSource ds)
	{
		String contentId = ds.getContentId() == null ? UUID.randomUUID().toString() : ds.getContentId();
		manifest.getReference().add(EbMSMessageUtils.createReference(contentId));
		return EbMSAttachmentFactory.createEbMSAttachment(ds.getName(),contentId,ds.getContentType(),ds.getContent());
	}

	public EbMSMessage createEbMSMessageMTOM(EbMSMessageContentMTOM content) throws EbMSProcessorException
	{
		try
		{
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(createMessageHeader(content.getContext()));
			result.setAckRequested(createAckRequested(content.getContext()));
			result.setSyncReply(createSyncReply(content.getContext()));
			if (content.getDataSources() != null && content.getDataSources().size() > 0)
			{
				Manifest manifest = EbMSMessageUtils.createManifest();
				List<EbMSAttachment> attachments = new ArrayList<>();
				content.getDataSources().forEach(ds -> attachments.add(createEbMSAttachmentMTOM(manifest,ds)));
				result.setManifest(manifest);
				result.setAttachments(attachments);
			}
			result.setMessage(EbMSMessageUtils.createSOAPMessage(result));
			return result;
		}
		catch (JAXBException | SOAPException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (DatatypeConfigurationException | ParserConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private EbMSAttachment createEbMSAttachmentMTOM(Manifest manifest, EbMSDataSourceMTOM ds)
	{
		try
		{
			String contentId = ds.getContentId() == null ? UUID.randomUUID().toString() : ds.getContentId();
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
		MessageHeader messageHeader = new MessageHeader();
		messageHeader.setVersion(Constants.EBMS_VERSION);
		messageHeader.setMustUnderstand(true);
		messageHeader.setCPAId(cpaId);
		messageHeader.setConversationId(conversationId);
		messageHeader.setFrom(from);
		messageHeader.setTo(to);
		messageHeader.setService(service);
		messageHeader.setAction(action);
		messageHeader.setMessageData(messageData);
		messageHeader.setDuplicateElimination(PerMessageCharacteristicsType.ALWAYS.equals(duplicateElimination) ? "" : null);
		return messageHeader;
	}

	private MessageHeader createMessageHeader(String cpaId, Party fromParty, Party toParty, EbMSAction action)
	{
		EbMSPartyInfo fromPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,fromParty)
				.orElseThrow(() -> StreamUtils.illegalStateException("EbMSPartyInfo",cpaId,fromParty));
		EbMSPartyInfo toPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,toParty)
				.orElseThrow(() -> StreamUtils.illegalStateException("EbMSPartyInfo",cpaId,toParty));
		CacheablePartyId partyId = new CacheablePartyId(fromPartyInfo.getPartyIds());
		String hostname = CPAUtils.getHostname(
				cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action.action())
				.orElseThrow(() -> StreamUtils.illegalStateException("DefaultDeliveryChannel",cpaId,partyId,action)));
		String conversationId = ebMSIdGenerator.generateConversationId();
		From from = createForm(fromPartyInfo.getPartyIds(),null);
		To to = createTo(toPartyInfo.getPartyIds(),null);
		Service service = createService(null,Constants.EBMS_SERVICE_URI);
		String messageId = ebMSIdGenerator.createMessageId(hostname,conversationId);
		MessageData messageData = createMessageData(messageId,null,new Date(),null);
		return createMessageHeader(cpaId,conversationId,from,to,service,action.action(),messageData,null); //deliveryChannel.getMessagingCharacteristics().getDuplicateElimination()
	}

	private MessageHeader createMessageHeader(EbMSMessageContext context) throws DatatypeConfigurationException
	{
		String cpaId = context.getCpaId();
		FromPartyInfo fromPartyInfo = cpaManager.getFromPartyInfo(cpaId,context.getFromRole(),context.getService(),context.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("FromPartyInfo",cpaId,context.getFromRole(),context.getService(),context.getAction()));
		ToPartyInfo toPartyInfo = cpaManager.getToPartyInfoByFromPartyActionBinding(cpaId,context.getFromRole(),context.getService(),context.getAction())
				.orElse(cpaManager.getToPartyInfo(cpaId,context.getToRole(),context.getService(),context.getAction())
						.orElseThrow(() -> StreamUtils.illegalStateException("ToPartyInfo",cpaId,context.getToRole(),context.getService(),context.getAction())));
		DeliveryChannel deliveryChannel = CPAUtils.getDeliveryChannel(fromPartyInfo.getCanSend().getThisPartyActionBinding());
		String hostname = CPAUtils.getHostname(deliveryChannel);
		String conversationId = context.getConversationId() == null ? ebMSIdGenerator.generateConversationId() : context.getConversationId();
		From from = createForm(fromPartyInfo.getPartyIds(),fromPartyInfo.getRole());
		To to = createTo(toPartyInfo.getPartyIds(),toPartyInfo.getRole());
		Service service = createService(fromPartyInfo.getService().getType(),fromPartyInfo.getService().getValue());
		String action = fromPartyInfo.getCanSend().getThisPartyActionBinding().getAction();
		String messageId = ebMSIdGenerator.createMessageId(hostname,conversationId,context.getMessageId());
		Date timestamp = new Date();
		Date timeToLive = createTimeToLive(deliveryChannel,timestamp);
		MessageData messageData = createMessageData(messageId,context.getRefToMessageId(),timestamp,timeToLive);
		return createMessageHeader(cpaId,conversationId,from,to,service,action,messageData,deliveryChannel.getMessagingCharacteristics().getDuplicateElimination());
	}

	private MessageHeader createResponseMessageHeader(MessageHeader messageHeader, Date timestamp, EbMSAction action) throws DatatypeConfigurationException, JAXBException
	{
		String cpaId = messageHeader.getCPAId();
		CacheablePartyId partyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
		DeliveryChannel deliveryChannel = cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action.action()).orElse(null);
		String hostname = CPAUtils.getHostname(deliveryChannel);
		From from = createForm(messageHeader.getTo().getPartyId(),null);
		To to = createTo(messageHeader.getFrom().getPartyId(),null);
		Service service = createService(null,Constants.EBMS_SERVICE_URI);
		String messageId = ebMSIdGenerator.generateMessageId(hostname);
		MessageData messageData = createMessageData(messageId,messageHeader.getMessageData().getMessageId(),timestamp,null);
		return createMessageHeader(messageHeader.getCPAId(),messageHeader.getConversationId(),from,to,service,action.action(),messageData,null);
	}

	private From createForm(Collection<? extends PartyId> partyIds, String role)
	{
		From result = new From();
		result.getPartyId().addAll(partyIds);
		result.setRole(role);
		return result;
	}

	private To createTo(List<PartyId> partyIds, String role)
	{
		To result = new To();
		result.getPartyId().addAll(partyIds);
		result.setRole(role);
		return result;
	}

	private Service createService(String type, String value)
	{
		Service result = new Service();
		result.setType(type);
		result.setValue(value);
		return result;
	}

	private MessageData createMessageData(String messageId, String refToMessageId, Date timestamp, Date timeToLive)
	{
		MessageData result = new MessageData();
		result.setMessageId(messageId);
		result.setRefToMessageId(refToMessageId);
		result.setTimestamp(timestamp);
		result.setTimeToLive(timeToLive);
		return result;
	}

	private Date createTimeToLive(DeliveryChannel deliveryChannel, Date date) throws DatatypeConfigurationException
	{
		if (CPAUtils.isReliableMessaging(deliveryChannel))
		{
			Duration duration = CPAUtils.getSenderReliableMessaging(deliveryChannel)
					.getRetryInterval()
					.multiply(CPAUtils.getSenderReliableMessaging(deliveryChannel).getRetries().intValue() + 1);
			Date result = (Date)date.clone();
			duration.addTo(result);
			return result;
		}
		else
			return null;
	}

	private AckRequested createAckRequested(EbMSMessageContext context)
	{
		String cpaId = context.getCpaId();
		DeliveryChannel channel = cpaManager.getFromPartyInfo(cpaId,context.getFromRole(),context.getService(),context.getAction())
				.map(p -> CPAUtils.getDeliveryChannel(p.getCanSend().getThisPartyActionBinding()))
				.orElseThrow(() -> StreamUtils.illegalStateException("FromPartyInfo",cpaId,context.getFromRole(),context.getService(),context.getAction()));

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
	
	private SyncReply createSyncReply(String cpaId, Party fromParty, String action)
	{
		CacheablePartyId partyId = new CacheablePartyId(cpaManager.getEbMSPartyInfo(cpaId,fromParty)
				.orElseThrow(() -> StreamUtils.illegalStateException("EbMSPartyInfo",cpaId,fromParty)).getPartyIds());
		return EbMSMessageUtils.createSyncReply(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,action)
				.orElseThrow(() -> StreamUtils.illegalStateException("DefaultDeliveryChannel",cpaId,partyId,action)));
	}
	
	private SyncReply createSyncReply(EbMSMessageContext context)
	{
		String cpaId = context.getCpaId();
		CacheablePartyId partyId = new CacheablePartyId(cpaManager.getFromPartyInfo(cpaId,context.getFromRole(),context.getService(),context.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("FromPartyInfo",cpaId,context.getFromRole(),context.getService(),context.getAction()))
				.getPartyIds());
		return EbMSMessageUtils.createSyncReply(cpaManager.getDefaultDeliveryChannel(cpaId,partyId,context.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("DefaultDeliveryChannel",cpaId,partyId,context.getAction())));
	}

	private StatusResponse createStatusResponse(StatusRequest statusRequest, EbMSMessageStatus status, Date timestamp) throws DatatypeConfigurationException
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

	public void setEbMSIdGenerator(EbMSIdGenerator ebMSIdGenerator)
	{
		this.ebMSIdGenerator = ebMSIdGenerator;
	}
}
