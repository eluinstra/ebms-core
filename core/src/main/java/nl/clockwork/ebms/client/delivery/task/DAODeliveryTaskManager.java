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
package nl.clockwork.ebms.client.delivery.task;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.EbMSDAO;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPAUtils;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
class DAODeliveryTaskManager extends DeliveryTaskManager
{
	@NonNull
	String serverId;

	public DAODeliveryTaskManager(
			@NonNull EbMSDAO ebMSDAO,
			@NonNull DeliveryTaskDAO deliveryTaskDAO,
			@NonNull CPAManager cpaManager,
			String serverId,
			int nrAutoRetries,
			long autoRetryInterval)
	{
		super(ebMSDAO, deliveryTaskDAO, cpaManager, nrAutoRetries, autoRetryInterval);
		this.serverId = serverId;
	}

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
		updateTask(task, url, status, errorMessage, null);
	}

	@Override
	public void updateTask(final DeliveryTask task, final String url, final DeliveryTaskStatus status, final String errorMessage, final Exception e)
	{
		val deliveryChannel = cpaManager.getDeliveryChannel(task.getCpaId(), task.getReceiveDeliveryChannelId())
				.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel", task.getCpaId(), task.getReceiveDeliveryChannelId()));
		deliveryTaskDAO.insertLog(task.getMessageId(), task.getTimestamp(), url, status, errorMessage);
		val reliableMessaging = CPAUtils.isReliableMessaging(deliveryChannel);
		if (task.getTimeToLive() != null && reliableMessaging)
			deliveryTaskDAO.updateTask(createNextTask(task, deliveryChannel));
		else if (shouldRetryUnreliable(task, status, e, reliableMessaging))
			deliveryTaskDAO.updateTask(createNextTask(task, autoRetryInterval));
		else
			deliveryTaskDAO.deleteTask(task.getMessageId());
	}

	@Override
	public void deleteTask(String messageId)
	{
		deliveryTaskDAO.deleteTask(messageId);
	}
}