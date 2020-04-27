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

public class EbMSEvent
{
	private String cpaId;
	private String sendDeliveryChannelId;
	private String receiveDeliveryChannelId;
	private String messageId;
	private Date timeToLive;
	private Date timestamp;
	private boolean isConfidential;
	private int retries;

	public EbMSEvent(String cpaId, String sendDeliveryChannelId, String receiveDeliveryChannelId, String messageId, Date timeToLive, Date timestamp, boolean isConfidential, int retries)
	{
		this.cpaId = cpaId;
		this.sendDeliveryChannelId = sendDeliveryChannelId;
		this.receiveDeliveryChannelId = receiveDeliveryChannelId;
		this.messageId = messageId;
		this.timeToLive = timeToLive;
		this.timestamp = timestamp;
		this.isConfidential = isConfidential;
		this.retries = retries;
	}

	public String getCpaId()
	{
		return cpaId;
	}

	public String getSendDeliveryChannelId()
	{
		return sendDeliveryChannelId;
	}

	public String getReceiveDeliveryChannelId()
	{
		return receiveDeliveryChannelId;
	}

	public String getMessageId()
	{
		return messageId;
	}

	public Date getTimeToLive()
	{
		return timeToLive;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public boolean isConfidential()
	{
		return isConfidential;
	}

	public int getRetries()
	{
		return retries;
	}

}
