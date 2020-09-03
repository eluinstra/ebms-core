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
package nl.clockwork.ebms.delivery.task;

import java.time.Instant;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
class DAODeliveryTaskManager implements DeliveryTaskManager
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	CPAManager cpaManager;
	String serverId;
	int nrAutoRetries;
	int autoRetryInterval;

	@Override
	public void createTask(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential)
	{
		deliveryTaskDAO.insertTask(new DeliveryTask(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(), messageId, timeToLive, timestamp, isConfidential, 0),serverId);
	}

	@Override
	public void updateTask(final DeliveryTask task, final String url, final DeliveryTaskStatus status)
	{
		updateTask(task,url,status,null);
	}

	@Override
	public void updateTask(final DeliveryTask task, final String url, final DeliveryTaskStatus status, final String errorMessage)
	{
		val deliveryChannel = cpaManager.getDeliveryChannel(
				task.getCpaId(),
				task.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",task.getCpaId(),task.getReceiveDeliveryChannelId()));
		deliveryTaskDAO.insertLog(task.getMessageId(),task.getTimestamp(),url,status,errorMessage);
		val reliableMessaging = CPAUtils.isReliableMessaging(deliveryChannel);
		if (task.getTimeToLive() != null && reliableMessaging)
			deliveryTaskDAO.updateTask(createNextTask(task,deliveryChannel));
		else
		{
			switch(ebMSDAO.getMessageAction(task.getMessageId()).orElse(null))
			{
				case ACKNOWLEDGMENT:
				case MESSAGE_ERROR:
					if (!reliableMessaging && task.getRetries() < nrAutoRetries)
					{
						deliveryTaskDAO.updateTask(createNextTask(task,autoRetryInterval));
						break;
					}
				default:
					deliveryTaskDAO.deleteTask(task.getMessageId());
			}
		}
	}

	@Override
	public void deleteTask(String messageId)
	{
		deliveryTaskDAO.deleteTask(messageId);
	}
}
