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
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import nl.clockwork.ebms.client.sync.EbMSDAO;
import nl.clockwork.ebms.common.event.MessageEventType;
import nl.clockwork.ebms.common.model.EbMSMessageProperties;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

class JMSTextMessageEventListenerTest
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
	void onMessageFailedPublishesTextMessageWithBodyAndAllHeaders() throws Exception
	{
		final EbMSDAO dao = mock(EbMSDAO.class);
		final JmsTemplate template = mock(JmsTemplate.class);
		final Map<String, Destination> destinations = queueDestinations();
		when(dao.getEbMSMessageProperties("msg-text")).thenReturn(Optional.of(properties("msg-text")));
		final JMSTextMessageEventListener listener = new JMSTextMessageEventListener(dao, template, destinations);

		listener.onMessageFailed("msg-text");

		final ArgumentCaptor<MessageCreator> creatorCaptor = ArgumentCaptor.forClass(MessageCreator.class);
		verify(template).send(eq(destinations.get(MessageEventType.FAILED.name())), creatorCaptor.capture());

		final Session session = mock(Session.class);
		final TextMessage textMessage = mock(TextMessage.class);
		when(session.createTextMessage()).thenReturn(textMessage);
		creatorCaptor.getValue().createMessage(session);

		verify(textMessage).setText("EbMS Message Context");
		verify(textMessage).setStringProperty("cpaId", "cpa-1");
		verify(textMessage).setStringProperty("fromPartyId", "fromParty");
		verify(textMessage).setStringProperty("fromRole", "fromRole");
		verify(textMessage).setStringProperty("toPartyId", "toParty");
		verify(textMessage).setStringProperty("toRole", "toRole");
		verify(textMessage).setStringProperty("service", "service");
		verify(textMessage).setStringProperty("action", "action");
		verify(textMessage).setStringProperty("conversationId", "conversation-1");
		verify(textMessage).setStringProperty("messageId", "msg-text");
		verify(textMessage).setStringProperty("refToMessageId", "ref-msg-text");
	}

	@Test
	void onMessageExpiredSkipsSendWhenDaoReturnsEmpty()
	{
		final EbMSDAO dao = mock(EbMSDAO.class);
		final JmsTemplate template = mock(JmsTemplate.class);
		when(dao.getEbMSMessageProperties("missing")).thenReturn(Optional.empty());
		final JMSTextMessageEventListener listener = new JMSTextMessageEventListener(dao, template, queueDestinations());

		listener.onMessageExpired("missing");

		verify(template, never()).send(any(Destination.class), any(MessageCreator.class));
	}

	@Test
	void onMessageReceivedSendsToReceivedDestination() throws Exception
	{
		final EbMSDAO dao = mock(EbMSDAO.class);
		final JmsTemplate template = mock(JmsTemplate.class);
		final Map<String, Destination> destinations = queueDestinations();
		when(dao.getEbMSMessageProperties("msg-r")).thenReturn(Optional.of(properties("msg-r")));
		final JMSTextMessageEventListener listener = new JMSTextMessageEventListener(dao, template, destinations);

		listener.onMessageReceived("msg-r");

		verify(template).send(eq(destinations.get(MessageEventType.RECEIVED.name())), any(MessageCreator.class));
	}
}
