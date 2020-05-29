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

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventManagerRetryAck extends EventManager
{
	int nrAutoRetries;
	int autoRetryInterval;

	public EventManagerRetryAck(EbMSEventDAO ebMSEventDAO, CPAManager cpaManager, String serverId, int nrAutoRetries, int autoRetryInterval)
	{
		super(ebMSEventDAO,cpaManager,serverId);
		this.nrAutoRetries = nrAutoRetries;
		this.autoRetryInterval = autoRetryInterval;
	}

	@Override
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
					ebMSeventDAO.insertEventLog(event.getMessageId(),event.getTimestamp(),url,status,errorMessage);
					if (event.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
					{
						ebMSeventDAO.updateEvent(createNewEvent(event,deliveryChannel));
					}
					else
					{
						switch(ebMSeventDAO.getMessageAction(event.getMessageId()).orElse(null))
						{
							case ACKNOWLEDGMENT:
							case MESSAGE_ERROR:
								if (event.getRetries() < nrAutoRetries)
								{
									ebMSeventDAO.updateEvent(retryEvent(event, autoRetryInterval));
									break;
								}
								else
								{
									ebMSeventDAO.insertEventLog(event.getMessageId(),event.getTimestamp(),url,status, "Stopped retrying acknowledge");
								}

							default:
								ebMSeventDAO.deleteEvent(event.getMessageId());
						}
					}
				}
			}
		);
	}
}
