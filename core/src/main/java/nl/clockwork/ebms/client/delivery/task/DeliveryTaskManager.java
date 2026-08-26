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

import java.time.Instant;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.client.EbMSDAO;
import nl.clockwork.ebms.client.delivery.client.EbMSUnrecoverableResponseException;
import nl.clockwork.ebms.common.cpa.CPAManager;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ReliableMessaging;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class DeliveryTaskManager
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	CPAManager cpaManager;
	int nrAutoRetries;
	long autoRetryInterval;

	public DeliveryTaskManager(
			@NonNull EbMSDAO ebMSDAO,
			@NonNull DeliveryTaskDAO deliveryTaskDAO,
			@NonNull CPAManager cpaManager,
			int nrAutoRetries,
			long autoRetryInterval)
	{
		this.ebMSDAO = ebMSDAO;
		this.deliveryTaskDAO = deliveryTaskDAO;
		this.cpaManager = cpaManager;
		this.nrAutoRetries = nrAutoRetries;
		this.autoRetryInterval = autoRetryInterval;
	}

	public abstract void insertTask(DeliveryTask task);

	public abstract void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status);

	public abstract void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage);

	public abstract void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage, Exception e);

	public abstract void deleteTask(String messageId);

	public DeliveryTask createNewTask(
			String cpaId,
			String sendDeliveryChannelId,
			String receiveDeliveryChannelId,
			String messageId,
			Instant timeToLive,
			Instant timestamp,
			boolean confidential)
	{
		return new DeliveryTask(cpaId, sendDeliveryChannelId, receiveDeliveryChannelId, messageId, timeToLive, timestamp, confidential, 0);
	}

	public DeliveryTask createNextTask(DeliveryTask task, DeliveryChannel deliveryChannel)
	{
		val rm = getReceiverReliableMessaging(deliveryChannel);
		val timestamp = task.getRetries() < rm.getRetries().intValue() ? Instant.now().plus(rm.getRetryInterval()) : task.getTimeToLive();
		return task.createNextTask(timestamp);
	}

	private ReliableMessaging getReceiverReliableMessaging(DeliveryChannel deliveryChannel)
	{
		return ((DocExchange)deliveryChannel.getDocExchangeId()).getEbXMLReceiverBinding().getReliableMessaging();
	}

	public DeliveryTask createNextTask(DeliveryTask task, long retryInterval)
	{
		return task.createNextTask(Instant.now().plusSeconds(60 * retryInterval));
	}

	public boolean shouldRetryUnreliable(DeliveryTask task, DeliveryTaskStatus status, Exception e, boolean reliableMessaging)
	{
		return status != DeliveryTaskStatus.SUCCEEDED
				&& !(e instanceof EbMSUnrecoverableResponseException)
				&& !reliableMessaging
				&& task.getRetries() < nrAutoRetries
				&& ebMSDAO.getMessageAction(task.getMessageId()).map(a -> a == EbMSAction.ACKNOWLEDGMENT || a == EbMSAction.MESSAGE_ERROR).orElse(false);
	}
}