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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.common.EbMSMessageStatus;
import nl.clockwork.ebms.common.event.MessageEventType;
import nl.clockwork.ebms.common.model.EbMSMessageProperties;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

class JMSMessageEventListenerTest
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

	private static EbMSMessageProperties properties(String messageId)
	{
		return new EbMSMessageProperties(
				"cpa-1",
				"fromParty",
				"fromRole",
				"toParty",
				"toRole",
				"service",
				"action",
				Instant.parse("2026-01-01T00:00:00Z"),
				"conversation-1",
				messageId,
				"ref-" + messageId,
				EbMSMessageStatus.RECEIVED);
	}

	@Test
	void onMessageReceivedPublishesAllHeadersFromDao() throws Exception
	{
		final EbMSDAO dao = mock(EbMSDAO.class);
		final JmsTemplate template = mock(JmsTemplate.class);
		final Map<String, Destination> destinations = queueDestinations();
		when(dao.getEbMSMessageProperties("msg-1")).thenReturn(Optional.of(properties("msg-1")));
		final JMSMessageEventListener listener = new JMSMessageEventListener(dao, template, destinations);

		listener.onMessageReceived("msg-1");

		final ArgumentCaptor<MessageCreator> creatorCaptor = ArgumentCaptor.forClass(MessageCreator.class);
		verify(template).send(eq(destinations.get(MessageEventType.RECEIVED.name())), creatorCaptor.capture());

		final Session session = mock(Session.class);
		final Message message = mock(Message.class);
		when(session.createMessage()).thenReturn(message);
		creatorCaptor.getValue().createMessage(session);

		verify(message).setStringProperty("cpaId", "cpa-1");
		verify(message).setStringProperty("fromPartyId", "fromParty");
		verify(message).setStringProperty("fromRole", "fromRole");
		verify(message).setStringProperty("toPartyId", "toParty");
		verify(message).setStringProperty("toRole", "toRole");
		verify(message).setStringProperty("service", "service");
		verify(message).setStringProperty("action", "action");
		verify(message).setStringProperty("conversationId", "conversation-1");
		verify(message).setStringProperty("messageId", "msg-1");
		verify(message).setStringProperty("refToMessageId", "ref-msg-1");
	}

	@Test
	void onMessageDeliveredSkipsSendWhenDaoReturnsEmpty()
	{
		final EbMSDAO dao = mock(EbMSDAO.class);
		final JmsTemplate template = mock(JmsTemplate.class);
		when(dao.getEbMSMessageProperties("missing")).thenReturn(Optional.empty());
		final JMSMessageEventListener listener = new JMSMessageEventListener(dao, template, queueDestinations());

		listener.onMessageDelivered("missing");

		verify(template, never()).send(any(Destination.class), any(MessageCreator.class));
	}

	@Test
	void onMessageFailedSendsToFailedDestination() throws Exception
	{
		final EbMSDAO dao = mock(EbMSDAO.class);
		final JmsTemplate template = mock(JmsTemplate.class);
		final Map<String, Destination> destinations = queueDestinations();
		when(dao.getEbMSMessageProperties("msg-f")).thenReturn(Optional.of(properties("msg-f")));
		final JMSMessageEventListener listener = new JMSMessageEventListener(dao, template, destinations);

		listener.onMessageFailed("msg-f");

		verify(template).send(eq(destinations.get(MessageEventType.FAILED.name())), any(MessageCreator.class));
	}

	@Test
	void onMessageExpiredSendsToExpiredDestination() throws Exception
	{
		final EbMSDAO dao = mock(EbMSDAO.class);
		final JmsTemplate template = mock(JmsTemplate.class);
		final Map<String, Destination> destinations = queueDestinations();
		when(dao.getEbMSMessageProperties("msg-e")).thenReturn(Optional.of(properties("msg-e")));
		final JMSMessageEventListener listener = new JMSMessageEventListener(dao, template, destinations);

		listener.onMessageExpired("msg-e");

		verify(template).send(eq(destinations.get(MessageEventType.EXPIRED.name())), any(MessageCreator.class));
	}
}
