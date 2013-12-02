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

import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

public class EbMSMessageContext implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String cpaId;
	private String fromRole;
	private String toRole;
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
		this(messageHeader.getCPAId(),messageHeader.getFrom().getRole(),messageHeader.getTo().getRole(),EbMSMessageUtils.toString(messageHeader.getService()),messageHeader.getAction(),messageHeader.getMessageData().getTimestamp().toGregorianCalendar().getTime(),messageHeader.getConversationId(),messageHeader.getMessageData().getMessageId(),messageHeader.getMessageData().getRefToMessageId(),null);		
	}
	
	public EbMSMessageContext(String cpaId, String fromRole, String toRole, String service, String action, Date timestamp, String conversationId, String messageId, String refToMessageId, Integer sequenceNr)
	{
		this.cpaId = cpaId;
		this.fromRole = fromRole;
		this.toRole = toRole;
		this.service = service;
		this.action = action;
		this.timestamp = timestamp;
		this.conversationId = conversationId;
		this.messageId = messageId;
		this.refToMessageId = refToMessageId;
		this.sequenceNr = sequenceNr;
	}

	public String getCpaId()
	{
		return cpaId;
	}
	
	public void setCpaId(String cpaId)
	{
		this.cpaId = cpaId;
	}

	public String getFromRole()
	{
		return fromRole;
	}
	
	public void setFromRole(String fromRole)
	{
		this.fromRole = fromRole;
	}
	
	public String getToRole()
	{
		return toRole;
	}
	
	public void setToRole(String toRole)
	{
		this.toRole = toRole;
	}
	
	public String getService()
	{
		return service;
	}
	
	public void setService(String service)
	{
		this.service = service;
	}
	
	public String getAction()
	{
		return action;
	}
	
	public void setAction(String action)
	{
		this.action = action;
	}
	
	public Date getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}
	
	public String getConversationId()
	{
		return conversationId;
	}
	
	public void setConversationId(String conversationId)
	{
		this.conversationId = conversationId;
	}
	
	public String getMessageId()
	{
		return messageId;
	}
	
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	
	public String getRefToMessageId()
	{
		return refToMessageId;
	}
	
	public void setRefToMessageId(String refToMessageId)
	{
		this.refToMessageId = refToMessageId;
	}
	
	public Integer getSequenceNr()
	{
		return sequenceNr;
	}
	
	public void setSequenceNr(Integer sequenceNr)
	{
		this.sequenceNr = sequenceNr;
	}
}
