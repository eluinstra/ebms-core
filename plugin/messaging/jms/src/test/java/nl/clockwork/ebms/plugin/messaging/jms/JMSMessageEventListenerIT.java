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
import static org.mockito.Mockito.when;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.common.EbMSMessageStatus;
import nl.clockwork.ebms.common.event.MessageEventType;
import nl.clockwork.ebms.common.model.EbMSMessageProperties;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.Test;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

class JMSMessageEventListenerIT
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
				"cpa-it",
				"fromParty-it",
				"fromRole-it",
				"toParty-it",
				"toRole-it",
				"service-it",
				"action-it",
				Instant.parse("2026-01-01T00:00:00Z"),
				"conversation-it",
				messageId,
				"ref-" + messageId,
				EbMSMessageStatus.RECEIVED);
	}

	@Test
	void simpleListenerPublishesMessageWithOnlyMessageIdProperty() throws Exception
	{
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("evt-simple-it"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);
		try
		{
			final JmsTemplate template = JmsTestSupport.newJmsTemplate(factory);
			final SimpleJMSMessageEventListener listener = new SimpleJMSMessageEventListener(template, queueDestinations());

			listener.onMessageReceived("msg-evt-simple");

			final Message received = template.receive(MessageEventType.RECEIVED.name());
			assertThat(received).isNotNull();
			assertThat(received.getStringProperty("messageId")).isEqualTo("msg-evt-simple");
		}
		finally
		{
			factory.destroy();
		}
	}

	@Test
	void jmsListenerPublishesMessageWithAllHeadersFromDao() throws Exception
	{
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("evt-jms-it"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);
		try
		{
			final EbMSDAO dao = mock(EbMSDAO.class);
			when(dao.getEbMSMessageProperties("msg-evt-full")).thenReturn(Optional.of(properties("msg-evt-full")));
			final JmsTemplate template = JmsTestSupport.newJmsTemplate(factory);
			final JMSMessageEventListener listener = new JMSMessageEventListener(dao, template, queueDestinations());

			listener.onMessageDelivered("msg-evt-full");

			final Message received = template.receive(MessageEventType.DELIVERED.name());
			assertThat(received).isNotNull();
			assertThat(received.getStringProperty("cpaId")).isEqualTo("cpa-it");
			assertThat(received.getStringProperty("fromPartyId")).isEqualTo("fromParty-it");
			assertThat(received.getStringProperty("fromRole")).isEqualTo("fromRole-it");
			assertThat(received.getStringProperty("toPartyId")).isEqualTo("toParty-it");
			assertThat(received.getStringProperty("toRole")).isEqualTo("toRole-it");
			assertThat(received.getStringProperty("service")).isEqualTo("service-it");
			assertThat(received.getStringProperty("action")).isEqualTo("action-it");
			assertThat(received.getStringProperty("conversationId")).isEqualTo("conversation-it");
			assertThat(received.getStringProperty("messageId")).isEqualTo("msg-evt-full");
			assertThat(received.getStringProperty("refToMessageId")).isEqualTo("ref-msg-evt-full");
		}
		finally
		{
			factory.destroy();
		}
	}

	@Test
	void textListenerPublishesTextMessageWithBodyAndHeaders() throws Exception
	{
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("evt-text-it"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);
		try
		{
			final EbMSDAO dao = mock(EbMSDAO.class);
			when(dao.getEbMSMessageProperties("msg-evt-text")).thenReturn(Optional.of(properties("msg-evt-text")));
			final JmsTemplate template = JmsTestSupport.newJmsTemplate(factory);
			final JMSTextMessageEventListener listener = new JMSTextMessageEventListener(dao, template, queueDestinations());

			listener.onMessageFailed("msg-evt-text");

			final Message received = template.receive(MessageEventType.FAILED.name());
			assertThat(received).isInstanceOf(TextMessage.class);
			final TextMessage textMessage = (TextMessage)received;
			assertThat(textMessage.getText()).isEqualTo("EbMS Message Context");
			assertThat(textMessage.getStringProperty("messageId")).isEqualTo("msg-evt-text");
			assertThat(textMessage.getStringProperty("cpaId")).isEqualTo("cpa-it");
			assertThat(textMessage.getStringProperty("refToMessageId")).isEqualTo("ref-msg-evt-text");
		}
		finally
		{
			factory.destroy();
		}
	}
}
