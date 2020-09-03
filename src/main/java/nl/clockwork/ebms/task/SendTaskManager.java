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
package nl.clockwork.ebms.task;

import java.time.Instant;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import lombok.val;
import nl.clockwork.ebms.cpa.CPAUtils;

public interface SendTaskManager
{
	void createTask(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential);
	void updateTask(final SendTask task, final String url, final SendTaskStatus status);
	void updateTask(final SendTask task, final String url, final SendTaskStatus status, final String errorMessage);
	void deleteTask(String messageId);

	default SendTask createNextTask(SendTask task, DeliveryChannel deliveryChannel)
	{
		val rm = CPAUtils.getReceiverReliableMessaging(deliveryChannel);
		val timestamp = task.getRetries() < rm.getRetries().intValue() ? Instant.now().plus(rm.getRetryInterval()) : task.getTimeToLive();
		return task.createNextTask(timestamp);
	}

	default SendTask createNextTask(SendTask task, long retryInterval)
	{
		val timestamp = Instant.now().plusSeconds(60 * retryInterval);
		return task.createNextTask(timestamp);
	}
}
