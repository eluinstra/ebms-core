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
package nl.clockwork.ebms.model;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

public class EbMSMessageContext implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String cpaId;
	private Role fromRole;
	private Role toRole;
	private String service;
	private String action;
	private Date timestamp;
	private String conversationId;
	private String messageId;
	private String refToMessageId;
	private Integer sequenceNr;

	public EbMSMessageContext()
	{
	}
	
	public EbMSMessageContext(MessageHeader messageHeader)
	{
		cpaId = messageHeader.getCPAId();
		fromRole = new Role(CPAUtils.toString(messageHeader.getFrom().getPartyId().get(0)),messageHeader.getFrom().getRole());
		toRole = new Role(CPAUtils.toString(messageHeader.getTo().getPartyId().get(0)),messageHeader.getTo().getRole());
		service = CPAUtils.toString(messageHeader.getService());
		action = messageHeader.getAction();
		timestamp = messageHeader.getMessageData().getTimestamp();
		conversationId = messageHeader.getConversationId();
		messageId = messageHeader.getMessageData().getMessageId();
		refToMessageId = messageHeader.getMessageData().getRefToMessageId();
	}

	@XmlElement(required=true)
	public String getCpaId()
	{
		return cpaId;
	}
	
	public void setCpaId(String cpaId)
	{
		this.cpaId = cpaId;
	}

	@XmlElement
	public Role getFromRole()
	{
		return fromRole;
	}
	
	public void setFromRole(Role fromRole)
	{
		this.fromRole = fromRole;
	}
	
	@XmlElement
	public Role getToRole()
	{
		return toRole;
	}
	
	public void setToRole(Role toRole)
	{
		this.toRole = toRole;
	}
	
	
	@XmlElement(required=true)
	public String getService()
	{
		return service;
	}
	
	public void setService(String service)
	{
		this.service = service;
	}
	
	@XmlElement(required=true)
	public String getAction()
	{
		return action;
	}
	
	public void setAction(String action)
	{
		this.action = action;
	}
	
	@XmlElement
	public Date getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}
	
	@XmlElement
	public String getConversationId()
	{
		return conversationId;
	}
	
	public void setConversationId(String conversationId)
	{
		this.conversationId = conversationId;
	}
	
	@XmlElement
	public String getMessageId()
	{
		return messageId;
	}
	
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	
	@XmlElement
	public String getRefToMessageId()
	{
		return refToMessageId;
	}
	
	public void setRefToMessageId(String refToMessageId)
	{
		this.refToMessageId = refToMessageId;
	}
	
	@XmlElement
	public Integer getSequenceNr()
	{
		return sequenceNr;
	}
	
	public void setSequenceNr(Integer sequenceNr)
	{
		this.sequenceNr = sequenceNr;
	}
}
