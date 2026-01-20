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
package nl.clockwork.ebms.client.delivery;

import jakarta.jms.ConnectionFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.delivery.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageServiceHandlerConfig
{
	public enum MessageServiceHandlerType
	{
		DEFAULT, JMS;
	}

	@Value("${messageServiceHandler.minThreads}")
	Integer minThreads;
	@Value("${messageServiceHandler.maxThreads}")
	Integer maxThreads;
	@Value("${messageQueue.maxEntries}")
	int maxEntries;
	@Value("${messageQueue.timeout}")
	int timeout;
	@Autowired
	CPAManager cpaManager;
	@Autowired
	EbMSHttpClientFactory ebMSClientFactory;
	@Autowired
	@Qualifier("jmsTransactionManager")
	PlatformTransactionManager transactionManager;

	@Bean("messageServiceHandlerTaskExecutor")
	public ThreadPoolTaskExecutor messageServiceHandlerTaskExecutor()
	{
		val result = new ThreadPoolTaskExecutor();
		result.setCorePoolSize(minThreads);
		result.setMaxPoolSize(maxThreads);
		result.setQueueCapacity(maxThreads * 2);
		result.setWaitForTasksToCompleteOnShutdown(true);
		return result;
	}

	@Bean
	@Conditional(DefaultMessageServiceHandlerType.class)
	public MessageServiceHandler defaultMessageServiceHandler()
	{
		return DefaultMessageServiceHandler.builder()
				.messageQueue(new EbMSMessageQueue(maxEntries, timeout))
				.cpaManager(cpaManager)
				.ebMSClientFactory(ebMSClientFactory)
				.build();
	}

	@Bean
	@Conditional(JmsMessageServiceHandlerType.class)
	public MessageServiceHandler jmsMessageServiceHandler(ConnectionFactory connectionFactory)
	{
		return JMSMessageServiceHandler.jmsMessageServiceHandlerBuilder()
				.cpaManager(cpaManager)
				.ebMSClientFactory(ebMSClientFactory)
				.transactionManager(transactionManager)
				.jmsTemplate(new JmsTemplate(connectionFactory))
				.build();
	}

	public static class DefaultMessageServiceHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("messageServiceHandler.type", MessageServiceHandlerType.class, MessageServiceHandlerType.DEFAULT)
					== MessageServiceHandlerType.DEFAULT;
		}
	}

	public static class JmsMessageServiceHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("messageServiceHandler.type", MessageServiceHandlerType.class, MessageServiceHandlerType.DEFAULT) == MessageServiceHandlerType.JMS;
		}
	}
}
