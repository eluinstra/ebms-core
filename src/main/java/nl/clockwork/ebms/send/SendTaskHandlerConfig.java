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
package nl.clockwork.ebms.send;

import javax.jms.ConnectionFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

@Configuration
@EnableAsync
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendTaskHandlerConfig
{
	public enum SendTaskHandlerType
	{
		DEFAULT, JMS;
	}
	@Autowired
	SendTaskDAO sendTaskDAO;
	@Autowired
	CPAManager cpaManager;
	@Value("${ebms.serverId}")
	String serverId;
	@Value("${taskHandler.jms.destinationName}")
	String jmsDestinationName;
	@Value("${taskHandler.jms.receiveTimeout}")
	long receiveTimeout;
	@Value("${taskHandler.start}")
	boolean startTaskHandler;
	@Value("${taskHandler.minThreads}")
	int minThreads;
	@Value("${taskHandler.maxThreads}")
	int maxThreads;
	@Value("${taskHandler.maxTasks}")
	int maxTaksks;
	@Value("${taskHandler.executionInterval}")
	int taskHandlerExecutionInterval;
	@Value("${taskHandler.task.executionInterval}")
	int taskHandlerTaskExecutionInterval;
	@Autowired
	MessageEventListener messageEventListener;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	URLMapper urlMapper;
	@Autowired
	SendTaskManager sendTaskManager;
	@Autowired
	EbMSHttpClientFactory ebMSClientFactory;
	@Autowired
	EbMSMessageEncrypter messageEncrypter;
	@Autowired
	EbMSMessageProcessor messageProcessor;
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	@Autowired
	ConnectionFactory connectionFactory;
	@Autowired
	@Qualifier("dataSourceTransactionManager")
	PlatformTransactionManager dataSourceTransactionManager;
	@Autowired
	@Qualifier("jmsTransactionManager")
	PlatformTransactionManager jmsTransactionManager;
	@Value("${taskHandler.jms.destinationName}")
	String destinationName;

	@Bean("sendTaskExecutor")
	@Conditional(DefaultTaskHandler.class)
	public ThreadPoolTaskExecutor defaultTaskProcessor()
	{
		val result = new ThreadPoolTaskExecutor();
		result.setCorePoolSize(minThreads);
		result.setMaxPoolSize(maxThreads);
		result.setQueueCapacity(maxTaksks);
		result.setWaitForTasksToCompleteOnShutdown(true);
		return result;
	}

	@Bean
	@Conditional(DefaultTaskHandler.class)
	public SendTaskExecutor sendTaskExecutor()
	{
		return SendTaskExecutor.builder()
				.transactionManager(dataSourceTransactionManager)
				.sendTaskDAO(sendTaskDAO)
				.sendTaskHandler(sendTaskHandler())
				.timedTask(new TimedTask(taskHandlerExecutionInterval))
				.maxTasks(maxTaksks)
				.serverId(serverId)
				.build();
	}

	@Bean
	@Conditional(JmsTaskHandler.class)
	public DefaultMessageListenerContainer jmsTaskProcessor()
	{
		val result = new DefaultMessageListenerContainer();
		result.setConnectionFactory(connectionFactory);
		result.setTransactionManager(jmsTransactionManager);
		result.setSessionTransacted(true);
		result.setConcurrentConsumers(minThreads);
		result.setMaxConcurrentConsumers(maxThreads);
		result.setReceiveTimeout(receiveTimeout);
		result.setDestinationName(StringUtils.isEmpty(jmsDestinationName) ? JMSSendTaskManager.JMS_DESTINATION_NAME : jmsDestinationName);
		result.setMessageListener(new JMSSendTaskListener(sendTaskHandler()));
		return result;
	}

	@Bean
  public SendTaskHandler sendTaskHandler()
	{
		return SendTaskHandler.builder()
				.transactionManager(dataSourceTransactionManager)
				.messageEventListener(messageEventListener)
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.urlMapper(urlMapper)
				.sendTaskManager(sendTaskManager)
				.ebMSClientFactory(ebMSClientFactory)
				.messageEncrypter(messageEncrypter)
				.messageProcessor(messageProcessor)
				.timedTask(new TimedTask(taskHandlerTaskExecutionInterval))
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}

	public static class DefaultTaskHandler implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("taskHandler.start",Boolean.class,true)
					&& context.getEnvironment().getProperty("taskHandler.type",SendTaskHandlerType.class,SendTaskHandlerType.DEFAULT) == SendTaskHandlerType.DEFAULT;
		}
	}
	public static class JmsTaskHandler implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("taskHandler.start",Boolean.class,true)
					&& context.getEnvironment().getProperty("taskHandler.type",SendTaskHandlerType.class,SendTaskHandlerType.DEFAULT) == SendTaskHandlerType.JMS;
		}
	}
	public static class DefaultTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("taskHandler.type",SendTaskHandlerType.class,SendTaskHandlerType.DEFAULT) == SendTaskHandlerType.DEFAULT;
		}
	}
	public static class JmsTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("taskHandler.type",SendTaskHandlerType.class,SendTaskHandlerType.DEFAULT) == SendTaskHandlerType.JMS;
		}
	}
}
