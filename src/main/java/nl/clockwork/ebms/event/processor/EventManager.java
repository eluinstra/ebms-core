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

import java.util.Calendar;
import java.util.Date;

import nl.clockwork.ebms.EbMSEventStatus;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSEvent;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;

public class EventManager
{
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;

	public void createEvent(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Date timeToLive, Date timestamp, boolean isConfidential)
	{
		ebMSDAO.insertEvent(new EbMSEvent(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(), messageId, timeToLive, timestamp, isConfidential, 0));
	}

	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status)
	{
		updateEvent(event,url,status,null);
	}

	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status, final String errorMessage)
	{
		final DeliveryChannel deliveryChannel = cpaManager.getDeliveryChannel(
				event.getCpaId(),
				event.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",event.getCpaId(),event.getReceiveDeliveryChannelId()));
		ebMSDAO.executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					ebMSDAO.insertEventLog(event.getMessageId(),event.getTimestamp(), url, status, errorMessage);
					if (event.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
					{
						ebMSDAO.updateEvent(createNewEvent(event,deliveryChannel));
					}
					else
					{
						ebMSDAO.deleteEvent(event.getMessageId());
					}
				}
			}
		);
	}

	public void deleteEvent(String messageId)
	{
		ebMSDAO.deleteEvent(messageId);
	}
	
	protected EbMSEvent retryEvent(EbMSEvent event, int retryInterval)
	{
		Calendar timestamp = Calendar.getInstance();
		timestamp.add(Calendar.MINUTE, retryInterval);
		return new EbMSEvent(
				event.getCpaId(),
				event.getSendDeliveryChannelId(),
				event.getReceiveDeliveryChannelId(),
				event.getMessageId(),
				event.getTimeToLive(),
				timestamp.getTime(),
				event.isConfidential(),
				event.getRetries() + 1);
	}

	protected EbMSEvent createNewEvent(EbMSEvent event, DeliveryChannel deliveryChannel)
	{
		ReliableMessaging rm = CPAUtils.getReceiverReliableMessaging(deliveryChannel);
		Date timestamp = new Date();
		if (event.getRetries() < rm.getRetries().intValue())
			rm.getRetryInterval().addTo(timestamp);
		else
			timestamp = event.getTimeToLive();
		return new EbMSEvent(
				event.getCpaId(),
				event.getSendDeliveryChannelId(),
				event.getReceiveDeliveryChannelId(),
				event.getMessageId(),
				event.getTimeToLive(),
				timestamp,
				event.isConfidential(),
				event.getRetries() + 1);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public EbMSDAO getEbMSDAO()
	{
		return this.ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public CPAManager getCpaManager()
	{
		return this.cpaManager;
	}
}
