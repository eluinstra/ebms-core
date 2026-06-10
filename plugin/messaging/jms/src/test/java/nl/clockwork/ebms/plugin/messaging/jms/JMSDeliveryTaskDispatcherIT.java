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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.ObjectMessage;
import java.time.Instant;
import nl.clockwork.ebms.client.delivery.task.DeliveryTask;
import org.junit.jupiter.api.Test;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

class JMSDeliveryTaskDispatcherIT
{
	@Test
	void dispatchedTaskIsReceivedFromQueueWithMatchingCorrelationId() throws Exception
	{
		final String destination = "DELIVERY_TASK";
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("dispatcher-it"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);
		try
		{
			final JmsTemplate template = JmsTestSupport.newJmsTemplate(factory);
			final JMSDeliveryTaskDispatcher dispatcher = new JMSDeliveryTaskDispatcher(template, destination);

			final DeliveryTask task = DeliveryTask.builder()
					.cpaId("cpa-it")
					.receiveDeliveryChannelId("rcv")
					.messageId("msg-it-dispatch")
					.timestamp(Instant.parse("2026-01-01T00:00:00Z"))
					.build();

			assertThat(dispatcher.dispatch(task).get()).isNull();

			final var received = template.receive(destination);
			assertThat(received).isInstanceOf(ObjectMessage.class);
			final ObjectMessage objectMessage = (ObjectMessage)received;
			assertThat(objectMessage).isNotNull();
			assertThat(objectMessage.getJMSCorrelationID()).isEqualTo("msg-it-dispatch");
			final Object payload = objectMessage.getObject();
			assertThat(payload).isInstanceOf(DeliveryTask.class);
			final DeliveryTask receivedTask = (DeliveryTask)payload;
			assertThat(receivedTask.getMessageId()).isEqualTo("msg-it-dispatch");
			assertThat(receivedTask.getCpaId()).isEqualTo("cpa-it");
		}
		finally
		{
			factory.destroy();
		}
	}
}
