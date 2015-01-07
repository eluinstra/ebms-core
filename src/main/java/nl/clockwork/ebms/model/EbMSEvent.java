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

import java.util.Date;

import nl.clockwork.ebms.Constants.EbMSEventType;

public class EbMSEvent
{
	private String messageId;
	private Date time;
	private EbMSEventType type;
	private String uri;

	public EbMSEvent(String messageId, Date time, EbMSEventType type)
	{
		this(messageId,time,type,null);
	}

	public EbMSEvent(String messageId, Date time, EbMSEventType type, String uri)
	{
		this.messageId = messageId;
		this.time = time;
		this.type = type;
		this.uri = uri;
	}

	public String getMessageId()
	{
		return messageId;
	}

	public Date getTime()
	{
		return time;
	}

	public EbMSEventType getType()
	{
		return type;
	}

	public String getUri()
	{
		return uri;
	}

	public void setUri(String uri)
	{
		this.uri = uri;
	}
}
