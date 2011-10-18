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
package nl.clockwork.mule.ebms.transformer;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.Channel;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.From;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageData;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.PartyId;
import nl.clockwork.mule.ebms.model.ebxml.Reference;
import nl.clockwork.mule.ebms.model.ebxml.Service;
import nl.clockwork.mule.ebms.model.ebxml.To;
import nl.clockwork.mule.ebms.util.CPAUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageContentToEbMSMessage extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;
	private String hostname;

  public EbMSMessageContentToEbMSMessage()
	{
		registerSourceType(EbMSMessageContent.class);
	}
  
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			GregorianCalendar calendar = new GregorianCalendar();

			EbMSMessageContent content = (EbMSMessageContent)message.getPayload();
			Channel channel = ebMSDAO.getChannel((String)message.getProperty(Constants.EBMS_CHANNEL_ID));
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(channel.getCpaId());
			PartyInfo partyInfo = CPAUtils.getPartyInfoSend(cpa,channel.getActionId());
			PartyInfo otherPartyInfo = CPAUtils.getOtherPartyInfo(cpa,channel.getActionId());
			
			MessageHeader messageHeader = new MessageHeader();

			messageHeader.setVersion(Constants.EBMS_VERSION);
			messageHeader.setMustUnderstand(true);

			messageHeader.setCPAId(channel.getCpaId());
			messageHeader.setConversationId(content.getContext() != null ? content.getContext().getConversationId() : new Date().getTime() + message.getCorrelationId());
			
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
			messageHeader.getMessageData().setMessageId(new Date().getTime() + message.getCorrelationId() + "@" + hostname);
			messageHeader.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));

			messageHeader.setDuplicateElimination("");

			AckRequested ackRequested = new AckRequested();
			ackRequested.setVersion(Constants.EBMS_VERSION);
			ackRequested.setMustUnderstand(true);
			ackRequested.setSigned(false);
			ackRequested.setActor(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value());
			
			Manifest manifest = new Manifest();
			manifest.setVersion(Constants.EBMS_VERSION);

			Reference reference = new Reference();
			reference.setHref("cid:1");
			reference.setType("simple");
			//reference.setRole("XLinkRole");

			manifest.getReference().add(reference);
			
			message.setPayload(new Object[]{messageHeader,ackRequested,manifest});

			return message;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}

}
