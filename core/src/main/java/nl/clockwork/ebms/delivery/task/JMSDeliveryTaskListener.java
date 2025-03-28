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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
class JMSDeliveryTaskListener implements MessageListener
{
	@NonNull
	DeliveryTaskHandler deliveryTaskHandler;

	@Override
	public void onMessage(Message message)
	{
		try
		{
			val task = createDeliveryTask(message);
			deliveryTaskHandler.handle(task);
		}
		catch (JMSException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private DeliveryTask createDeliveryTask(Message message) throws JMSException
	{
		return DeliveryTask.builder()
				.cpaId(message.getStringProperty("cpaId"))
				.sendDeliveryChannelId(message.getStringProperty("sendDeliveryChannelId"))
				.receiveDeliveryChannelId(message.getStringProperty("receiveDeliveryChannelId"))
				.messageId(message.getStringProperty("messageId"))
				.timeToLive(message.getStringProperty("timeToLive") != null ? Instant.parse(message.getStringProperty("timeToLive")) : null)
				.timestamp(Instant.parse(message.getStringProperty("timestamp")))
				.confidential(message.getBooleanProperty("confidential"))
				.retries(message.getIntProperty("retries"))
				.build();
	}
}
