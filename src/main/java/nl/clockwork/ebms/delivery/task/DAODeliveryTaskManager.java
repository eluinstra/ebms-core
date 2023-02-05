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


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.EbMSAction;
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
		val deliveryChannel = cpaManager.getDeliveryChannel(task.getCpaId(), task.getReceiveDeliveryChannelId())
				.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel", task.getCpaId(), task.getReceiveDeliveryChannelId()));
		deliveryTaskDAO.insertLog(task.getMessageId(), task.getTimestamp(), url, status, errorMessage);
		val reliableMessaging = CPAUtils.isReliableMessaging(deliveryChannel);
		if (task.getTimeToLive() != null && reliableMessaging)
			deliveryTaskDAO.updateTask(createNextTask(task, deliveryChannel));
		else if (mustUpdate(task, reliableMessaging))
			deliveryTaskDAO.updateTask(createNextTask(task, autoRetryInterval));
		else
			deliveryTaskDAO.deleteTask(task.getMessageId());
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
