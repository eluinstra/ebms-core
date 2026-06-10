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
import jakarta.jms.Destination;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.common.event.MessageEventListener;
import nl.clockwork.ebms.common.event.MessageEventType;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JmsMessageEventListenerConfig
{
	@Value("${eventListener.jms.destinationType:QUEUE}")
	JMSDestinationType jmsDestinationType;

	@Bean
	@Conditional(SimpleJmsEventListenerType.class)
	public MessageEventListener simpleJmsMessageEventListener(ConnectionFactory connectionFactory)
	{
		val jmsTemplate = new JmsTemplate(Objects.requireNonNull(connectionFactory));
		return new SimpleJMSMessageEventListener(jmsTemplate, createMessageEventDestinations(jmsDestinationType));
	}

	@Bean
	@Conditional(JmsEventListenerType.class)
	public MessageEventListener jmsMessageEventListener(ConnectionFactory connectionFactory, EbMSDAO ebMSDAO)
	{
		val jmsTemplate = new JmsTemplate(Objects.requireNonNull(connectionFactory));
		return new JMSMessageEventListener(ebMSDAO, jmsTemplate, createMessageEventDestinations(jmsDestinationType));
	}

	@Bean
	@Conditional(JmsTextEventListenerType.class)
	public MessageEventListener jmsTextMessageEventListener(ConnectionFactory connectionFactory, EbMSDAO ebMSDAO)
	{
		val jmsTemplate = new JmsTemplate(Objects.requireNonNull(connectionFactory));
		return new JMSTextMessageEventListener(ebMSDAO, jmsTemplate, createMessageEventDestinations(jmsDestinationType));
	}

	private static Map<String, Destination> createMessageEventDestinations(JMSDestinationType jmsDestinationType)
	{
		return MessageEventType.stream().collect(Collectors.toMap(Enum::name, e -> createDestination(jmsDestinationType, e)));
	}

	private static Destination createDestination(JMSDestinationType jmsDestinationType, MessageEventType e)
	{
		return jmsDestinationType == JMSDestinationType.QUEUE ? new ActiveMQQueue(e.name()) : new ActiveMQTopic("VirtualTopic." + e.name());
	}

	public static class SimpleJmsEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return "SIMPLE_JMS".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}

	public static class JmsEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return "JMS".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}

	public static class JmsTextEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return "JMS_TEXT".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}
}
