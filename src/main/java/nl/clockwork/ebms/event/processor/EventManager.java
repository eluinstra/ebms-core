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
import java.util.List;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.event.processor.dao.EbMSEventDAO;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class EventManager
{
	@NonNull
	EbMSEventDAO ebMSeventDAO;
	@NonNull
	CPAManager cpaManager;

	public void createEvent(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential)
	{
		ebMSeventDAO.insertEvent(new EbMSEvent(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(), messageId, timeToLive, timestamp, isConfidential, 0));
	}

	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status)
	{
		updateEvent(event,url,status,null);
	}

	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status, final String errorMessage)
	{
		val deliveryChannel = cpaManager.getDeliveryChannel(
				event.getCpaId(),
				event.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",event.getCpaId(),event.getReceiveDeliveryChannelId()));
		ebMSeventDAO.executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					ebMSeventDAO.insertEventLog(event.getMessageId(),event.getTimestamp(), url, status, errorMessage);
					if (event.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
					{
						ebMSeventDAO.updateEvent(createNewEvent(event,deliveryChannel));
					}
					else
					{
						ebMSeventDAO.deleteEvent(event.getMessageId());
					}
				}
			}
		);
	}

	public void deleteEvent(String messageId)
	{
		ebMSeventDAO.deleteEvent(messageId);
	}
	
	protected EbMSEvent retryEvent(EbMSEvent event, int retryInterval)
	{
		val timestamp = Instant.now().plusSeconds(60 * retryInterval);
		return EbMSEvent.builder()
				.cpaId(event.getCpaId())
				.sendDeliveryChannelId(event.getSendDeliveryChannelId())
				.receiveDeliveryChannelId(event.getReceiveDeliveryChannelId())
				.messageId(event.getMessageId())
				.timeToLive(event.getTimeToLive())
				.timestamp(timestamp)
				.isConfidential(event.isConfidential())
				.retries(event.getRetries() + 1)
				.build();
	}

	protected EbMSEvent createNewEvent(EbMSEvent event, DeliveryChannel deliveryChannel)
	{
		val rm = CPAUtils.getReceiverReliableMessaging(deliveryChannel);
		var timestamp = Instant.now();
		if (event.getRetries() < rm.getRetries().intValue())
			timestamp.plus(rm.getRetryInterval());
		else
			timestamp = event.getTimeToLive();
		return EbMSEvent.builder()
				.cpaId(event.getCpaId())
				.sendDeliveryChannelId(event.getSendDeliveryChannelId())
				.receiveDeliveryChannelId(event.getReceiveDeliveryChannelId())
				.messageId(event.getMessageId())
				.timeToLive(event.getTimeToLive())
				.timestamp(timestamp)
				.isConfidential(event.isConfidential())
				.retries(event.getRetries() + 1)
				.build();
	}

	public List<EbMSEvent> getEventsBefore(Instant timestamp)
	{
		return ebMSeventDAO.getEventsBefore(timestamp);
	}

	public List<EbMSEvent> getEventsBefore(Instant timestamp, int maxEvents)
	{
		return ebMSeventDAO.getEventsBefore(timestamp,maxEvents);
	}
}
