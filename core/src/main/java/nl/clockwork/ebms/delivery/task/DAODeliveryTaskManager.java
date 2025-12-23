/*
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.EbMSAction;
import nl.clockwork.ebms.common.StreamUtils;
import nl.clockwork.ebms.common.deliverytask.DeliveryChannel;
import nl.clockwork.ebms.common.deliverytask.DeliveryChannelManager;
import nl.clockwork.ebms.common.deliverytask.DeliveryTask;
import nl.clockwork.ebms.common.deliverytask.DeliveryTaskDAO;
import nl.clockwork.ebms.common.deliverytask.DeliveryTaskManager;
import nl.clockwork.ebms.common.deliverytask.DeliveryTaskStatus;
import nl.clockwork.ebms.dao.EbMSDAO;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
class DAODeliveryTaskManager implements DeliveryTaskManager
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	DeliveryChannelManager deliveryChannelManager;
	String serverId;
	int nrAutoRetries;
	int autoRetryInterval;

	@Override
	public void insertTask(DeliveryTask task)
	{
		deliveryTaskDAO.insertTask(task, serverId);
	}

	@Override
	public void updateTask(final DeliveryTask task, final String url, final DeliveryTaskStatus status)
	{
		updateTask(task, url, status, null);
	}

	@Override
	public void updateTask(final DeliveryTask task, final String url, final DeliveryTaskStatus status, final String errorMessage)
	{
		val deliveryChannel = deliveryChannelManager.getDeliveryChannel(task.getCpaId(), task.getReceiveDeliveryChannelId())
				.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel", task.getCpaId(), task.getReceiveDeliveryChannelId()));
		deliveryTaskDAO.insertLog(task.getMessageId(), task.getTimestamp(), url, status, errorMessage);
		if (task.getTimeToLive() != null && deliveryChannel.isReliableMessaging())
			deliveryTaskDAO.updateTask(createNextTask(task, createTimestamp(task, deliveryChannel)));
		else if (mustUpdate(task, deliveryChannel.isReliableMessaging()))
			deliveryTaskDAO.updateTask(createNextTask(task, autoRetryInterval));
		else
			deliveryTaskDAO.deleteTask(task.getMessageId());
	}

	private Instant createTimestamp(DeliveryTask task, DeliveryChannel deliveryChannel)
	{
		return task.getRetries() < deliveryChannel.getRetries() ? Instant.now().plus(deliveryChannel.getRetryInterval()) : task.getTimeToLive();
	}

	private boolean mustUpdate(DeliveryTask event, boolean reliableMessaging)
	{
		return ebMSDAO.getMessageAction(event.getMessageId())
				.map(a -> (a.equals(EbMSAction.ACKNOWLEDGMENT) || a.equals(EbMSAction.MESSAGE_ERROR)) && !reliableMessaging && event.getRetries() < nrAutoRetries)
				.orElse(false);
	}

	@Override
	public void deleteTask(String messageId)
	{
		deliveryTaskDAO.deleteTask(messageId);
	}
}
