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

import nl.clockwork.ebms.Constants.EbMSMessageEventType;

public class EbMSMessageEvent implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String messageId;
	private EbMSMessageEventType type;
	
	public EbMSMessageEvent()
	{
	}
	public EbMSMessageEvent(String messageId, EbMSMessageEventType type)
	{
		this.messageId = messageId;
		this.type = type;
	}
	public String getMessageId()
	{
		return messageId;
	}
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	public EbMSMessageEventType getType()
	{
		return type;
	}
	public void setType(EbMSMessageEventType type)
	{
		this.type = type;
	}
}
