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
package nl.clockwork.mule.ebms.bridge.tcp.model;

import java.io.Serializable;

import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

public class EbMSMessageContext implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String cpaId;
	private String from;
	private String to;
	private String service;
	private String action;
	private String conversationId;
	private String messageId;
	private String refToMessageId;
	//private boolean relateMessage;
	//private String channelId;
	//private String refChannelId;

	public EbMSMessageContext(MessageHeader messageHeader)
	{
		this(messageHeader.getCPAId(),messageHeader.getFrom().getRole(),messageHeader.getTo().getRole(),messageHeader.getService().getValue(),messageHeader.getAction(),messageHeader.getConversationId(),messageHeader.getMessageData().getMessageId(),messageHeader.getMessageData().getRefToMessageId());		
	}
	
/*	public EbMSMessageContext(String conversationId, String messageId)
	{
		this.conversationId = conversationId;
		this.messageId = messageId;
	}
*/	
	public EbMSMessageContext(String cpaId, String from, String to, String service, String action, String conversationId, String messageId, String refToMessageId)
	{
		this.cpaId = cpaId;
		this.from = from;
		this.to = to;
		this.service = service;
		this.action = action;
		this.conversationId = conversationId;
		this.messageId = messageId;
		this.refToMessageId = refToMessageId;
	}

	public String getCpaId()
	{
		return cpaId;
	}

	public String getFrom()
	{
		return from;
	}
	
	public String getTo()
	{
		return to;
	}
	
	public String getService()
	{
		return service;
	}
	
	public String getAction()
	{
		return action;
	}
	
	public String getConversationId()
	{
		return conversationId;
	}
	
	public String getMessageId()
	{
		return messageId;
	}
	
	public String getRefToMessageId()
	{
		return refToMessageId;
	}
}
