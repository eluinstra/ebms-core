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

import nl.clockwork.ebms.Constants.EbMSMessageStatus;

public class EbMSMessageContext implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String cpaId;
	private Role fromRole;
	private Role toRole;
	private String service;
	private String action;
	private Date timestamp;
	private Date timeToLive;
	private String conversationId;
	private String messageId;
	private String refToMessageId;
	private Long sequenceNr;
	private EbMSMessageStatus messageStatus;

	public EbMSMessageContext()
	{
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

	@XmlElement(required=true)
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
	public Date getTimeToLive()
	{
		return timeToLive;
	}

	public void setTimeToLive(Date timeToLive)
	{
		this.timeToLive = timeToLive;
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
	public Long getSequenceNr()
	{
		return sequenceNr;
	}

	public void setSequenceNr(Long sequenceNr)
	{
		this.sequenceNr = sequenceNr;
	}

	@XmlElement
	public EbMSMessageStatus getMessageStatus()
	{
		return messageStatus;
	}

	public void setMessageStatus(EbMSMessageStatus messageStatus)
	{
		this.messageStatus = messageStatus;
	}
}
