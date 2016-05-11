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
package nl.clockwork.ebms.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

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
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

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
import org.xml.sax.SAXException;

public class EbMSMessageFactory
{
	private boolean cleoPatch;
	private CPAManager cpaManager;

	public EbMSMessage createEbMSMessageError(String cpaId, EbMSMessage message, ErrorList errorList, Date timestamp) throws DatatypeConfigurationException, JAXBException
	{
		MessageHeader messageHeader = createMessageHeader(cpaId,message.getMessageHeader(),timestamp,EbMSAction.MESSAGE_ERROR);
		if (errorList.getError().size() == 0)
		{
			errorList.getError().add(EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN,"An unknown error occurred!"));
			errorList.setHighestSeverity(SeverityType.ERROR);
		}
		EbMSMessage result = new EbMSMessage();
		result.setMessageHeader(messageHeader);
		result.setErrorList(errorList);
		return result;
	}

	public EbMSMessage createEbMSAcknowledgment(String cpaId, EbMSMessage message, Date timestamp) throws EbMSProcessorException
	{
		try
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
			result.setMessageHeader(createMessageHeader(cpaId,fromParty,toParty,EbMSAction.PING.action()));
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
	
	public EbMSMessage createEbMSPong(String cpaId, EbMSMessage ping) throws EbMSProcessorException
	{
		try
		{
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(createMessageHeader(cpaId,ping.getMessageHeader(),new Date(),EbMSAction.PONG));
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
			MessageHeader messageHeader = createMessageHeader(cpaId,fromParty,toParty,EbMSAction.STATUS_REQUEST.action());
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

	public EbMSMessage createEbMSStatusResponse(String cpaId, EbMSMessage request, EbMSMessageStatus status, Date timestamp) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = createMessageHeader(cpaId,request.getMessageHeader(),new Date(),EbMSAction.STATUS_RESPONSE);
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

	public EbMSMessage createEbMSMessage(String cpaId, EbMSMessageContent content) throws EbMSProcessorException
	{
		try
		{
			EbMSMessage result = new EbMSMessage();
			result.setMessageHeader(createMessageHeader(cpaId,content.getContext()));
			result.setAckRequested(createAckRequested(cpaId,content.getContext()));
			result.setSyncReply(createSyncReply(cpaId,content.getContext()));
			if (content.getDataSources().size() > 0)
			{
				Manifest manifest = EbMSMessageUtils.createManifest();
				List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
				int i = 1;
				for (EbMSDataSource dataSource : content.getDataSources())
				{
					String contentId = createContentId(result.getMessageHeader().getMessageData().getMessageId(),i++);
					manifest.getReference().add(EbMSMessageUtils.createReference(contentId));
					ByteArrayDataSource ds = new ByteArrayDataSource(dataSource.getContent(),dataSource.getContentType());
					ds.setName(dataSource.getName());
					attachments.add(new EbMSAttachment(ds,contentId));
				}
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

	private String createContentId(String messageId, int i)
	{
		return messageId.replaceAll("^([^@]+)@(.+)$","$1-" + i + "@$2");
	}

	private MessageHeader createMessageHeader(String cpaId, Party fromParty, Party toParty, String action)
	{
		String uuid = UUID.randomUUID().toString();
		EbMSPartyInfo fromPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,fromParty);
		EbMSPartyInfo toPartyInfo = cpaManager.getEbMSPartyInfo(cpaId,toParty);
		String hostname = CPAUtils.getHostname(cpaManager.getDefaultDeliveryChannel(cpaId,new CacheablePartyId(fromPartyInfo.getPartyIds()),action));

		MessageHeader messageHeader = new MessageHeader();

		messageHeader.setVersion(Constants.EBMS_VERSION);
		messageHeader.setMustUnderstand(true);

		messageHeader.setCPAId(cpaId);
		messageHeader.setConversationId(uuid);
		
		messageHeader.setFrom(new From());
		messageHeader.getFrom().getPartyId().addAll(fromPartyInfo.getPartyIds());
		if (cleoPatch)
			messageHeader.getFrom().setRole(fromParty.getRole());

		messageHeader.setTo(new To());
		messageHeader.getTo().getPartyId().addAll(toPartyInfo.getPartyIds());
		if (cleoPatch)
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

	private MessageHeader createMessageHeader(String cpaId, EbMSMessageContext context) throws DatatypeConfigurationException
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

	private MessageHeader createMessageHeader(String cpaId, MessageHeader messageHeader, Date timestamp, EbMSAction action) throws DatatypeConfigurationException, JAXBException
	{
		DeliveryChannel deliveryChannel = cpaManager.getDefaultDeliveryChannel(cpaId,new CacheablePartyId(messageHeader.getTo().getPartyId()),action.action());
		String hostname = CPAUtils.getHostname(deliveryChannel);

		MessageHeader result = XMLMessageBuilder.deepCopy(messageHeader);

		result.getFrom().getPartyId().clear();
		result.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
		if (cleoPatch)
			result.getFrom().setRole(messageHeader.getTo().getRole());
		else
			result.getFrom().setRole(null);

		result.getTo().getPartyId().clear();
		result.getTo().getPartyId().addAll(messageHeader.getFrom().getPartyId());
		if (cleoPatch)
			result.getTo().setRole(messageHeader.getFrom().getRole());
		else
			result.getTo().setRole(null);

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

	private void setTimeToLive(DeliveryChannel deliveryChannel, MessageHeader messageHeader) throws DatatypeConfigurationException
	{
		if (CPAUtils.isReliableMessaging(deliveryChannel))
		{
			Duration duration = CPAUtils.getReliableMessaging(deliveryChannel).getRetryInterval().multiply(CPAUtils.getReliableMessaging(deliveryChannel).getRetries().intValue() + 1);
			//Duration duration = CPAUtils.getPersistantDuration(deliveryChannel);
			Date timestamp = (Date)messageHeader.getMessageData().getTimestamp().clone();
			duration.addTo(timestamp);
			messageHeader.getMessageData().setTimeToLive(timestamp);
		}
	}

	private AckRequested createAckRequested(String cpaId, EbMSMessageContext context)
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
	
	private SyncReply createSyncReply(String cpaId, Party fromParty, String action)
	{
		return EbMSMessageUtils.createSyncReply(cpaManager.getDefaultDeliveryChannel(cpaId,new CacheablePartyId(cpaManager.getEbMSPartyInfo(cpaId,fromParty).getPartyIds()),action));
	}
	
	private SyncReply createSyncReply(String cpaId, EbMSMessageContext context)
	{
		return EbMSMessageUtils.createSyncReply(cpaManager.getDefaultDeliveryChannel(cpaId,new CacheablePartyId(cpaManager.getFromPartyInfo(cpaId,context.getFromRole(),context.getService(),context.getAction()).getPartyIds()),context.getAction()));
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

	public void setCleoPatch(boolean cleoPatch)
	{
		this.cleoPatch = cleoPatch;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}
}
