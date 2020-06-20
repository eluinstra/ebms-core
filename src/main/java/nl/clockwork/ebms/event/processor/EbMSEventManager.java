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
package nl.clockwork.ebms.event.processor;

import java.time.Instant;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Action;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class EbMSEventManager implements EventManager
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	EbMSEventDAO ebMSEventDAO;
	@NonNull
	CPAManager cpaManager;
	String serverId;
	int nrAutoRetries;
	int autoRetryInterval;

	@Override
	public void createEvent(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential)
	{
		ebMSEventDAO.insertEvent(new EbMSEvent(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(), messageId, timeToLive, timestamp, isConfidential, 0),serverId);
	}

	@Override
	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status)
	{
		updateEvent(event,url,status,null);
	}

	@Override
	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status, final String errorMessage)
	{
		val deliveryChannel = cpaManager.getDeliveryChannel(
				event.getCpaId(),
				event.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",event.getCpaId(),event.getReceiveDeliveryChannelId()));
		Action action = () ->
		{
			ebMSEventDAO.insertEventLog(event.getMessageId(),event.getTimestamp(),url,status,errorMessage);
			if (event.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
				ebMSEventDAO.updateEvent(createNextEvent(event,deliveryChannel));
			else
			{
				switch(ebMSDAO.getMessageAction(event.getMessageId()).orElse(null))
				{
					case ACKNOWLEDGMENT:
					case MESSAGE_ERROR:
						if (event.getRetries() < nrAutoRetries)
						{
							ebMSEventDAO.updateEvent(createNextEvent(event,autoRetryInterval));
							break;
						}
					default:
						ebMSEventDAO.deleteEvent(event.getMessageId());
				}
			}
		};
		ebMSEventDAO.executeTransaction(action);
	}

	@Override
	public void deleteEvent(String messageId)
	{
		ebMSEventDAO.deleteEvent(messageId);
	}
}
