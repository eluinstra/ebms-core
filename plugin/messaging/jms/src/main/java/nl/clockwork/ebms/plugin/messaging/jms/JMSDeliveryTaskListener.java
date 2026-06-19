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
package nl.clockwork.ebms.plugin.messaging.jms;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.client.client.DeliveryTask;
import nl.clockwork.ebms.client.delivery.handler.DeliveryTaskHandler;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JMSDeliveryTaskListener implements MessageListener
{
	@NonNull
	DeliveryTaskHandler deliveryTaskHandler;

	@Override
	public void onMessage(Message message)
	{
		try
		{
			if (!(message instanceof ObjectMessage objectMessage))
			{
				log.warn("Discarding unsupported JMS message type: {}", message.getClass().getName());
				return;
			}
			val payload = objectMessage.getObject();
			if (!(payload instanceof DeliveryTask task))
			{
				log.warn("Discarding JMS message with unexpected payload: {}", payload == null ? "null" : payload.getClass().getName());
				return;
			}
			deliveryTaskHandler.handle(task);
		}
		catch (JMSException e)
		{
			throw new org.springframework.jms.UncategorizedJmsException("Failed to read DeliveryTask from JMS message", e);
		}
	}
}
