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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.jms.Message;
import java.time.Instant;
import nl.clockwork.ebms.client.api.DeliveryTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

class JMSDeliveryTaskDispatcherTest
{
	@Test
	void dispatchSendsTaskToConfiguredDestination() throws Exception
	{
		final JmsTemplate template = mock(JmsTemplate.class);
		final JMSDeliveryTaskDispatcher dispatcher = new JMSDeliveryTaskDispatcher(template, "DELIVERY_TASK");
		final DeliveryTask task =
				DeliveryTask.builder().cpaId("cpa-1").receiveDeliveryChannelId("rcv").messageId("msg-42").timestamp(Instant.parse("2026-01-01T00:00:00Z")).build();

		assertThat(dispatcher.dispatch(task).get()).isNull();
		verify(template).convertAndSend(eq("DELIVERY_TASK"), eq(task), any(MessagePostProcessor.class));
	}

	@Test
	void messagePostProcessorSetsCorrelationIdToMessageId() throws Exception
	{
		final JmsTemplate template = mock(JmsTemplate.class);
		final JMSDeliveryTaskDispatcher dispatcher = new JMSDeliveryTaskDispatcher(template, "DELIVERY_TASK");
		final DeliveryTask task = DeliveryTask.builder()
				.cpaId("cpa-1")
				.receiveDeliveryChannelId("rcv")
				.messageId("msg-correlate")
				.timestamp(Instant.parse("2026-01-01T00:00:00Z"))
				.build();

		dispatcher.dispatch(task);

		final ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
		verify(template).convertAndSend(eq("DELIVERY_TASK"), eq(task), captor.capture());

		final Message jmsMessage = mock(Message.class);
		captor.getValue().postProcessMessage(jmsMessage);
		verify(jmsMessage).setJMSCorrelationID("msg-correlate");
	}
}
