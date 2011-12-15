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

import java.util.GregorianCalendar;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Description;
import nl.clockwork.mule.ebms.model.ebxml.Error;
import nl.clockwork.mule.ebms.model.ebxml.From;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageData;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.PartyId;
import nl.clockwork.mule.ebms.model.ebxml.Service;
import nl.clockwork.mule.ebms.model.ebxml.SeverityType;
import nl.clockwork.mule.ebms.model.ebxml.To;

public class EbMSMessageUtils
{

	public static MessageHeader createMessageHeader(CollaborationProtocolAgreement cpa, EbMSMessageContext context, String hostname) throws DatatypeConfigurationException
	{
		String uuid = UUID.randomUUID().toString();//nameUUIDFromBytes(hostname.getBytes()).toString();
		PartyInfo partyInfo = CPAUtils.getSendingPartyInfo(cpa,context.getFrom(),context.getService(),context.getAction());
		PartyInfo otherPartyInfo = CPAUtils.getReceivingPartyInfo(cpa,context.getTo(),context.getService(),context.getAction());
		//PartyInfo otherPartyInfo = CPAUtils.getOtherReceivingPartyInfo(cpa,context.getFrom(),context.getService(),context.getAction());

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
		messageHeader.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

		messageHeader.setDuplicateElimination("");
		return messageHeader;
	}
	
	public static AckRequested createAckRequested()
	{
		AckRequested ackRequested = new AckRequested();
		ackRequested.setVersion(Constants.EBMS_VERSION);
		ackRequested.setMustUnderstand(true);
		ackRequested.setSigned(false);
		ackRequested.setActor(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value());
		return ackRequested;
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
		error.setCodeContext("urn:oasis:names:tc:ebxml-msg:service:errors");
		error.setLocation(location);
		error.setErrorCode(errorCode);
		error.setDescription(new Description());
		error.getDescription().setLang(language);
		error.getDescription().setValue(description);
		error.setSeverity(severity);
		return error;
	}

}
