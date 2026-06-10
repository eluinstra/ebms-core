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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.util.Map;
import nl.clockwork.ebms.common.event.MessageEventType;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

class SimpleJMSMessageEventListenerTest
{
	private static Map<String, Destination> queueDestinations()
	{
		return Map.of(
				MessageEventType.RECEIVED.name(),
				new ActiveMQQueue(MessageEventType.RECEIVED.name()),
				MessageEventType.DELIVERED.name(),
				new ActiveMQQueue(MessageEventType.DELIVERED.name()),
				MessageEventType.FAILED.name(),
				new ActiveMQQueue(MessageEventType.FAILED.name()),
				MessageEventType.EXPIRED.name(),
				new ActiveMQQueue(MessageEventType.EXPIRED.name()));
	}

	@Test
	void onMessageReceivedSendsToReceivedDestinationWithMessageIdProperty() throws Exception
	{
		assertSendsToDestination(MessageEventType.RECEIVED, (l, id) -> l.onMessageReceived(id));
	}

	@Test
	void onMessageDeliveredSendsToDeliveredDestination() throws Exception
	{
		assertSendsToDestination(MessageEventType.DELIVERED, (l, id) -> l.onMessageDelivered(id));
	}

	@Test
	void onMessageFailedSendsToFailedDestination() throws Exception
	{
		assertSendsToDestination(MessageEventType.FAILED, (l, id) -> l.onMessageFailed(id));
	}

	@Test
	void onMessageExpiredSendsToExpiredDestination() throws Exception
	{
		assertSendsToDestination(MessageEventType.EXPIRED, (l, id) -> l.onMessageExpired(id));
	}

	@FunctionalInterface
	private interface ListenerInvocation
	{
		void invoke(SimpleJMSMessageEventListener listener, String messageId) throws Exception;
	}

	private static void assertSendsToDestination(MessageEventType type, ListenerInvocation invocation) throws Exception
	{
		final JmsTemplate template = mock(JmsTemplate.class);
		final Map<String, Destination> destinations = queueDestinations();
		final SimpleJMSMessageEventListener listener = new SimpleJMSMessageEventListener(template, destinations);

		invocation.invoke(listener, "msg-evt-1");

		final ArgumentCaptor<MessageCreator> creatorCaptor = ArgumentCaptor.forClass(MessageCreator.class);
		verify(template).send(org.mockito.ArgumentMatchers.eq(destinations.get(type.name())), creatorCaptor.capture());

		final Session session = mock(Session.class);
		final Message message = mock(Message.class);
		when(session.createMessage()).thenReturn(message);
		creatorCaptor.getValue().createMessage(session);
		verify(message).setStringProperty("messageId", "msg-evt-1");
		verify(session).createMessage();
		verify(message, org.mockito.Mockito.times(1)).setStringProperty(any(), any());
	}
}
