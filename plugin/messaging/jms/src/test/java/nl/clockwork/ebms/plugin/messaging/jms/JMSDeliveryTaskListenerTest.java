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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;
import java.time.Instant;
import nl.clockwork.ebms.client.client.DeliveryTask;
import nl.clockwork.ebms.client.delivery.handler.DeliveryTaskHandler;
import org.junit.jupiter.api.Test;

class JMSDeliveryTaskListenerTest
{
	@Test
	void onMessageInvokesHandlerWhenPayloadIsDeliveryTask() throws Exception
	{
		final DeliveryTaskHandler handler = mock(DeliveryTaskHandler.class);
		final JMSDeliveryTaskListener listener = new JMSDeliveryTaskListener(handler);
		final DeliveryTask task = DeliveryTask.builder()
				.cpaId("cpa-1")
				.receiveDeliveryChannelId("rcv")
				.messageId("msg-listener-1")
				.timestamp(Instant.parse("2026-01-01T00:00:00Z"))
				.build();
		final ObjectMessage objectMessage = mock(ObjectMessage.class);
		when(objectMessage.getObject()).thenReturn(task);

		listener.onMessage(objectMessage);

		verify(handler).handle(task);
	}

	@Test
	void onMessageIgnoresNonObjectMessage()
	{
		final DeliveryTaskHandler handler = mock(DeliveryTaskHandler.class);
		final JMSDeliveryTaskListener listener = new JMSDeliveryTaskListener(handler);
		final TextMessage textMessage = mock(TextMessage.class);

		listener.onMessage(textMessage);

		verifyNoInteractions(handler);
	}

	@Test
	void onMessageIgnoresObjectMessageWithUnexpectedPayload() throws Exception
	{
		final DeliveryTaskHandler handler = mock(DeliveryTaskHandler.class);
		final JMSDeliveryTaskListener listener = new JMSDeliveryTaskListener(handler);
		final ObjectMessage objectMessage = mock(ObjectMessage.class);
		when(objectMessage.getObject()).thenReturn("not-a-delivery-task");

		listener.onMessage(objectMessage);

		verify(handler, never()).handle(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void onMessageIgnoresObjectMessageWithNullPayload() throws Exception
	{
		final DeliveryTaskHandler handler = mock(DeliveryTaskHandler.class);
		final JMSDeliveryTaskListener listener = new JMSDeliveryTaskListener(handler);
		final ObjectMessage objectMessage = mock(ObjectMessage.class);
		when(objectMessage.getObject()).thenReturn(null);

		listener.onMessage(objectMessage);

		verifyNoInteractions(handler);
	}
}
