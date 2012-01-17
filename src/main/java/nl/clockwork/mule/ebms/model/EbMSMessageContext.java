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
package nl.clockwork.mule.ebms.model;

import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

public class EbMSMessageContext
{
	private String cpaId;
	private String fromRole;
	private String toRole;
	private String serviceType;
	private String service;
	private String action;
	private String conversationId;
	private String messageId;
	private String refToMessageId;

	public EbMSMessageContext(MessageHeader messageHeader)
	{
		this(messageHeader.getCPAId(),messageHeader.getFrom().getRole(),messageHeader.getTo().getRole(),messageHeader.getService().getType(),messageHeader.getService().getValue(),messageHeader.getAction(),messageHeader.getConversationId(),messageHeader.getMessageData().getMessageId(),messageHeader.getMessageData().getRefToMessageId());		
	}
	
	public EbMSMessageContext(String cpaId, String service, String action)
	{
		this(cpaId,null,null,service,action,null);
	}

	public EbMSMessageContext(String cpaId, String service, String action, String conversationId)
	{
		this(cpaId,null,null,service,action,conversationId);
	}

	public EbMSMessageContext(String cpaId, String from, String to, String service, String action, String conversationId)
	{
		this(cpaId,from,to,null,service,action,conversationId,null,null);
	}
	
	public EbMSMessageContext(String cpaId, String fromRole, String toRole, String serviceType, String service, String action, String conversationId, String messageId, String refToMessageId)
	{
		this.cpaId = cpaId;
		this.fromRole = fromRole;
		this.toRole = toRole;
		this.serviceType = serviceType;
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

	public String getFromRole()
	{
		return fromRole;
	}
	
	public String getToRole()
	{
		return toRole;
	}
	
	public String getServiceType()
	{
		return serviceType;
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
