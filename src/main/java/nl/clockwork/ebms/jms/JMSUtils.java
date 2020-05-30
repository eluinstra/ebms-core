/**
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
package nl.clockwork.ebms.jms;

import java.util.Map;
import java.util.stream.Collectors;

import javax.jms.Destination;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import lombok.val;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;

public class JMSUtils
{
	public static JmsTemplate createJmsTemplate(String jmsBrokerURL)
	{
		val pooledConnectionFactory = createConnectionFactory(jmsBrokerURL);
		val jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(pooledConnectionFactory);
		return jmsTemplate;
	}

	public static PooledConnectionFactory createConnectionFactory(String jmsBrokerURL)
	{
		val result = new PooledConnectionFactory();
		result.setConnectionFactory(createActiveMQConnectionFactory(jmsBrokerURL));
		return result;
	}

	public static ActiveMQConnectionFactory createActiveMQConnectionFactory(String jmsBrokerURL)
	{
		val result = new ActiveMQConnectionFactory();
		result.setBrokerURL(jmsBrokerURL);
		return result;
	}

	public static Map<String,Destination> createDestinations(JMSDestinationType jmsDestinationType)
	{
		return EbMSMessageEventType.stream()
				.collect(Collectors.toMap(e -> e.name(),e -> jmsDestinationType == JMSDestinationType.QUEUE ? new ActiveMQQueue(e.name()) : new ActiveMQTopic("VirtualTopic." + e.name())));
	}
}
