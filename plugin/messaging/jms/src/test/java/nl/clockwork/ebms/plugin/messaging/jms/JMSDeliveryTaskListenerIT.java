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
import static org.mockito.Mockito.mock;

import jakarta.jms.ConnectionFactory;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import nl.clockwork.ebms.client.client.DeliveryTask;
import nl.clockwork.ebms.client.delivery.handler.DeliveryTaskHandler;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

class JMSDeliveryTaskListenerIT
{
	@Test
	void taskQueuedOnDestinationIsDispatchedToHandlerByListenerContainer() throws Exception
	{
		final String destination = "DELIVERY_TASK";
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("listener-it"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<DeliveryTask> handledTask = new AtomicReference<>();
		final DeliveryTaskHandler handler = mock(DeliveryTaskHandler.class);
		org.mockito.Mockito.doAnswer((InvocationOnMock inv) ->
		{
			handledTask.set(inv.getArgument(0));
			latch.countDown();
			return null;
		}).when(handler).handle(org.mockito.ArgumentMatchers.any(DeliveryTask.class));

		final DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.setDestinationName(destination);
		container.setMessageListener(new JMSDeliveryTaskListener(handler));
		container.setConcurrentConsumers(1);
		container.setReceiveTimeout(500L);
		container.afterPropertiesSet();
		container.start();

		try
		{
			final JMSDeliveryTaskDispatcher dispatcher = new JMSDeliveryTaskDispatcher(JmsTestSupport.newJmsTemplate(factory), destination);
			final DeliveryTask task = DeliveryTask.builder()
					.cpaId("cpa-it")
					.receiveDeliveryChannelId("rcv")
					.messageId("msg-listener-it")
					.timestamp(Instant.parse("2026-01-01T00:00:00Z"))
					.build();
			dispatcher.dispatch(task);

			assertThat(latch.await(10, TimeUnit.SECONDS)).as("handler invoked within timeout").isTrue();
			assertThat(handledTask.get()).isNotNull();
			assertThat(handledTask.get().getMessageId()).isEqualTo("msg-listener-it");
		}
		finally
		{
			container.stop();
			container.destroy();
			factory.destroy();
		}
	}

	@Test
	void nonDeliveryTaskPayloadIsSilentlyDiscarded() throws Exception
	{
		final String destination = "DELIVERY_TASK";
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("listener-noise-it"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);

		final DeliveryTaskHandler handler = mock(DeliveryTaskHandler.class);

		final DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.setDestinationName(destination);
		container.setMessageListener(new JMSDeliveryTaskListener(handler));
		container.setConcurrentConsumers(1);
		container.setReceiveTimeout(500L);
		container.afterPropertiesSet();
		container.start();

		try
		{
			final JmsTemplate template = JmsTestSupport.newJmsTemplate(factory);
			template.convertAndSend(destination, "not-a-delivery-task");

			Thread.sleep(1_500L);
			org.mockito.Mockito.verifyNoInteractions(handler);
		}
		finally
		{
			container.stop();
			container.destroy();
			factory.destroy();
		}
	}
}
