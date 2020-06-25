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

import java.time.Instant;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class EbMSSendEventListener implements MessageListener
{
	@NonNull
	HandleEventTask.HandleEventTaskBuilder handleEventTaskBuilder;

	@Override
	public void onMessage(Message message)
	{
		try
		{
			val event = createEvent(message);
			val task = createTask(event);
			task.run();
		}
		catch (JMSException e)
		{
			throw new RuntimeException(e);
		}
	}

	private HandleEventTask createTask(final nl.clockwork.ebms.event.processor.EbMSEvent event)
	{
		synchronized (this)
		{
			return handleEventTaskBuilder.event(event).build();
		}
	}

	private EbMSEvent createEvent(Message message) throws JMSException
	{
		val result = EbMSEvent.builder()
				.cpaId(message.getStringProperty("cpaId"))
				.sendDeliveryChannelId(message.getStringProperty("sendDeliveryChannelId"))
				.receiveDeliveryChannelId(message.getStringProperty("receiveDeliveryChannelId"))
				.messageId(message.getStringProperty("messageId"))
				.timeToLive(message.getStringProperty("timeToLive") != null ? Instant.parse(message.getStringProperty("timeToLive")) : null)
				.timestamp(Instant.parse(message.getStringProperty("timestamp")))
				.confidential(message.getBooleanProperty("confidential"))
				.retries(message.getIntProperty("retries"))
				.build();
		return result;
	}
}
