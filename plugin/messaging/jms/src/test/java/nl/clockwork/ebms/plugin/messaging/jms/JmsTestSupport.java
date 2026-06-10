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

import jakarta.jms.ConnectionFactory;
import java.util.UUID;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Test helpers for embedded-ActiveMQ integration tests. Each test gets its own broker instance via a unique broker name embedded in the {@code vm://} URL so
 * tests can run in parallel without sharing queues.
 */
final class JmsTestSupport
{
	private JmsTestSupport()
	{
	}

	static String randomBrokerName(String prefix)
	{
		return prefix + "-" + UUID.randomUUID();
	}

	static ConnectionFactory newConnectionFactory(String brokerName)
	{
		final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://" + brokerName + "?broker.persistent=false&broker.useJmx=false&create=true");
		factory.setTrustAllPackages(true);
		return factory;
	}

	static JmsTemplate newJmsTemplate(ConnectionFactory connectionFactory)
	{
		final JmsTemplate template = new JmsTemplate(connectionFactory);
		template.setReceiveTimeout(5_000L);
		return template;
	}
}
