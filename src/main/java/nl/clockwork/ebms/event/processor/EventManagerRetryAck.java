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

import java.util.Date;

import nl.clockwork.ebms.StreamUtils;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

public class EventManagerRetryAck extends EventManager
{
	private int nrAutoRetries;
	private int autoRetryInterval;

	@Override
	public void createEvent(String cpaId, DeliveryChannel deliveryChannel, String messageId, Date timeToLive, Date timestamp, boolean isConfidential)
	{
		if (deliveryChannel != null)
		{
			getEbMSDAO().deleteEvent(messageId);
			getEbMSDAO().insertEvent(new EbMSEvent(cpaId,deliveryChannel.getChannelId(),messageId,timeToLive,timestamp,isConfidential,0));
		}
		else
			getEbMSDAO().insertEventLog(messageId,timestamp,null,EbMSEventStatus.FAILED, "Could not resolve endpoint!");
	}

	@Override
	public void updateEvent(final EbMSEvent event, final String url, final EbMSEventStatus status, final String errorMessage)
	{
		final DeliveryChannel deliveryChannel = getCpaManager().getDeliveryChannel(
				event.getCpaId(),
				event.getDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",event.getCpaId(),event.getDeliveryChannelId()));
		getEbMSDAO().executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					getEbMSDAO().insertEventLog(event.getMessageId(),event.getTimestamp(),url,status,errorMessage);
					if (event.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
					{
						getEbMSDAO().updateEvent(createNewEvent(event,deliveryChannel));
					}
					else
					{
						switch(getEbMSDAO().getMessageAction(event.getMessageId()).orElse(null))
						{
							case ACKNOWLEDGMENT:
							case MESSAGE_ERROR:
								if (event.getRetries() < nrAutoRetries)
								{
									getEbMSDAO().updateEvent(retryEvent(event, autoRetryInterval));
									break;
								}
								else
								{
									getEbMSDAO().insertEventLog(event.getMessageId(),event.getTimestamp(),url,status, "Stopped retrying acknowledge");
								}

							default:
								getEbMSDAO().deleteEvent(event.getMessageId());
						}
					}
				}
			}
		);
	}

	public int getNrAutoRetries()
	{
		return nrAutoRetries;
	}

	public void setNrAutoRetries(int nrAutoRetries)
	{
		this.nrAutoRetries = nrAutoRetries;
	}

	public int getAutoRetryInterval()
	{
		return autoRetryInterval;
	}

	public void setAutoRetryInterval(int autoRetryInterval)
	{
		this.autoRetryInterval = autoRetryInterval;
	}

}
