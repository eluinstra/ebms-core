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
package nl.clockwork.ebms.job;

import java.util.Date;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;

public class EventManager
{
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;

	public void createEvent(String cpaId, DeliveryChannel deliveryChannel, String messageId, Date timeToLive, Date timestamp, boolean isConfidential)
	{
		if (deliveryChannel != null)
			ebMSDAO.insertEvent(new EbMSEvent(cpaId,deliveryChannel.getChannelId(),messageId,timeToLive,timestamp,isConfidential,0));
		else
			ebMSDAO.insertEventLog(messageId,timestamp,null,EbMSEventStatus.FAILED,"Could not resolve endpoint!");
	}

	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status)
	{
		ebMSDAO.executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					ebMSDAO.insertEventLog(event.getMessageId(),event.getTimestamp(),url,status,null);
					ebMSDAO.deleteEvent(event.getMessageId());
				}
			}
		);
	}

	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status, final String errorMessage)
	{
		final DeliveryChannel deliveryChannel = cpaManager.getDeliveryChannel(event.getCpaId(),event.getDeliveryChannelId());
		ebMSDAO.executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					ebMSDAO.insertEventLog(event.getMessageId(),event.getTimestamp(),url,status,errorMessage);
					if (event.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
						ebMSDAO.updateEvent(createNewEvent(event,deliveryChannel));
					else
						ebMSDAO.deleteEvent(event.getMessageId());
				}
			}
		);
	}

	public void deleteEvent(String messageId)
	{
		ebMSDAO.deleteEvent(messageId);
	}

	private EbMSEvent createNewEvent(EbMSEvent event, DeliveryChannel deliveryChannel)
	{
		ReliableMessaging rm = CPAUtils.getReliableMessaging(deliveryChannel);
		Date timestamp = new Date();
		if (event.getRetries() < rm.getRetries().intValue() - 1)
			rm.getRetryInterval().addTo(timestamp);
		else //if (event.getRetries() < rm.getRetries().intValue())
			timestamp = event.getTimeToLive();
		return new EbMSEvent(event.getCpaId(),event.getDeliveryChannelId(),event.getMessageId(),event.getTimeToLive(),timestamp,event.isConfidential(),event.getRetries() + 1);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

}
