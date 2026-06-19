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
import nl.clockwork.ebms.client.client.DeliveryManager;
import nl.clockwork.ebms.client.transport.http.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.model.EbMSPong;
import nl.clockwork.ebms.common.model.EbMSResponseMessage;
import org.junit.jupiter.api.Test;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageData;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Verifies the reply-correlation path of {@link JMSDeliveryManager}: replies handed off via {@code handleResponseMessage} land on the {@code MESSAGE} queue
 * with a JMSCorrelationID equal to the original message's {@code refToMessageId}, and can be picked up by a correlation-selector receive.
 */
class JMSDeliveryManagerIT
{
	private static final String MESSAGE_DESTINATION = "MESSAGE";

	@Test
	void handleResponseMessagePublishesReplyWithCorrelationIdEqualToRefToMessageId() throws Exception
	{
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("manager-it"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);
		try
		{
			final JmsTemplate managerTemplate = new JmsTemplate(factory);
			final DeliveryManager manager = JMSDeliveryManager.jmsDeliveryManagerBuilder()
					.cpaManager(mock(CPAManager.class))
					.ebMSClientFactory(mock(EbMSHttpClientFactory.class))
					.transactionManager(new JmsTransactionManager(factory))
					.jmsTemplate(managerTemplate)
					.build();

			final EbMSPong pong = EbMSPong.builder().messageHeader(newMessageHeader("msg-it-ref-1")).build();
			manager.handleResponseMessage(pong);

			final JmsTemplate receiver = JmsTestSupport.newJmsTemplate(factory);
			final Object received = receiver.receiveSelectedAndConvert(MESSAGE_DESTINATION, "JMSCorrelationID='msg-it-ref-1'");
			assertThat(received).isInstanceOf(EbMSResponseMessage.class);
			assertThat(received).isInstanceOf(EbMSPong.class);
		}
		finally
		{
			factory.destroy();
		}
	}

	@Test
	void receiveSelectedDoesNotReturnMessagesWithDifferentCorrelationId() throws Exception
	{
		final ConnectionFactory rawFactory = JmsTestSupport.newConnectionFactory(JmsTestSupport.randomBrokerName("manager-it-noise"));
		final SingleConnectionFactory factory = new SingleConnectionFactory(rawFactory);
		try
		{
			final JmsTemplate managerTemplate = new JmsTemplate(factory);
			final DeliveryManager manager = JMSDeliveryManager.jmsDeliveryManagerBuilder()
					.cpaManager(mock(CPAManager.class))
					.ebMSClientFactory(mock(EbMSHttpClientFactory.class))
					.transactionManager(new JmsTransactionManager(factory))
					.jmsTemplate(managerTemplate)
					.build();

			manager.handleResponseMessage(EbMSPong.builder().messageHeader(newMessageHeader("msg-it-other")).build());

			final JmsTemplate receiver = JmsTestSupport.newJmsTemplate(factory);
			receiver.setReceiveTimeout(500L);
			final Object received = receiver.receiveSelectedAndConvert(MESSAGE_DESTINATION, "JMSCorrelationID='msg-it-not-present'");
			assertThat(received).isNull();
		}
		finally
		{
			factory.destroy();
		}
	}

	private static MessageHeader newMessageHeader(String refToMessageId)
	{
		final MessageHeader header = new MessageHeader();
		final MessageData data = new MessageData();
		data.setRefToMessageId(refToMessageId);
		header.setMessageData(data);
		return header;
	}
}
