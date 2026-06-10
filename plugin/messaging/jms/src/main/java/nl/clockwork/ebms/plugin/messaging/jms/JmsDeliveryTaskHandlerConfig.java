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
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JmsDeliveryTaskHandlerConfig
{
	private static final String DELIVERY_TASK_HANDLER_START = "deliveryTaskHandler.start";
	private static final String DELIVERY_TASK_HANDLER_TYPE = "deliveryTaskHandler.type";

	@Value("${deliveryTaskHandler.jms.destinationName:DELIVERY_TASK}")
	String jmsDestinationName;
	@Value("${deliveryTaskHandler.jms.receiveTimeout:3000}")
	long jmsReceiveTimeout;
	@Value("${deliveryTaskHandler.jms.concurrentConsumers:1}")
	int jmsConcurrentConsumers;
	@Value("${deliveryTaskHandler.jms.maxConcurrentConsumers:8}")
	int jmsMaxConcurrentConsumers;

	@Bean("deliveryTaskJmsTemplate")
	@Conditional(JmsTaskHandlerType.class)
	public JmsTemplate deliveryTaskJmsTemplate(ConnectionFactory connectionFactory)
	{
		val template = new JmsTemplate(connectionFactory);
		template.setDeliveryPersistent(true);
		return template;
	}

	@Bean
	@Conditional(JmsTaskHandlerType.class)
	public DeliveryTaskDispatcher jmsDispatcher(@Qualifier("deliveryTaskJmsTemplate") JmsTemplate deliveryTaskJmsTemplate)
	{
		return new JMSDeliveryTaskDispatcher(deliveryTaskJmsTemplate, jmsDestinationName);
	}

	@Bean(destroyMethod = "destroy")
	@Conditional(JmsTaskHandlerType.class)
	public DefaultMessageListenerContainer deliveryTaskListenerContainer(ConnectionFactory connectionFactory, DeliveryTaskHandler deliveryTaskHandler)
	{
		val container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setDestinationName(jmsDestinationName);
		container.setMessageListener(new JMSDeliveryTaskListener(deliveryTaskHandler));
		container.setSessionTransacted(true);
		container.setConcurrentConsumers(jmsConcurrentConsumers);
		container.setMaxConcurrentConsumers(jmsMaxConcurrentConsumers);
		container.setReceiveTimeout(jmsReceiveTimeout);
		return container;
	}

	public static class JmsTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_START, Boolean.class, true)
					&& "JMS".equalsIgnoreCase(context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, "DEFAULT"));
		}
	}
}
