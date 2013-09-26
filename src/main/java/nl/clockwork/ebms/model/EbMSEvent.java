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
package nl.clockwork.ebms.model;

import java.util.Date;

import nl.clockwork.ebms.Constants.EbMSEventType;

public class EbMSEvent
{
	private Long ebMSMessageId;
	private Date time;
	private EbMSEventType type;

	public EbMSEvent(Long ebMSMessageId, Date time, EbMSEventType type)
	{
		this.ebMSMessageId = ebMSMessageId;
		this.time = time;
		this.type = type;
	}

	public Long getEbMSMessageId()
	{
		return ebMSMessageId;
	}
	
	public Date getTime()
	{
		return time;
	}

	public EbMSEventType getType()
	{
		return type;
	}
}
