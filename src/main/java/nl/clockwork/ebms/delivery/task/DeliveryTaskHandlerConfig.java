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
package nl.clockwork.ebms.delivery.task;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;

@Configuration
@ComponentScan(basePackageClasses = {nl.clockwork.ebms.delivery.task.DeliveryTaskJob.class,nl.clockwork.ebms.delivery.task.JMSJob.class})
@EnableAsync
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskHandlerConfig
{
	public static enum DeliveryTaskHandlerType
	{
		DEFAULT, JMS, QUARTZ, QUARTZ_JMS, QUARTZ_KAFKA;
	}
	@Autowired
	DeliveryTaskDAO deliveryTaskDAO;
	@Autowired
	CPAManager cpaManager;
	@Value("${ebms.serverId:#{null}}")
	String serverId;
	@Value("${deliveryTaskHandler.jms.destinationName}")
	String jmsDestinationName;
	@Value("${deliveryTaskHandler.jms.receiveTimeout}")
	long receiveTimeout;
	@Value("${deliveryTaskHandler.start}")
	boolean startTaskHandler;
	@Value("${deliveryTaskHandler.minThreads}")
	int minThreads;
	@Value("${deliveryTaskHandler.maxThreads}")
	int maxThreads;
	@Value("${deliveryTaskHandler.default.maxTasks}")
	int maxTasks;
	@Value("${deliveryTaskHandler.default.executionInterval}")
	int taskHandlerExecutionInterval;
	@Value("${deliveryTaskHandler.task.executionInterval}")
	int taskHandlerTaskExecutionInterval;
	@Autowired
	MessageEventListener messageEventListener;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	URLMapper urlMapper;
	@Autowired
	DeliveryTaskManager deliveryTaskManager;
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
	@Qualifier("jmsTransactionManager")
	PlatformTransactionManager jmsTransactionManager;
	@Value("${deliveryTaskHandler.jms.destinationName}")
	String destinationName;

	@Bean("deliveryTaskExecutor")
	@Conditional(DefaultTaskHandlerType.class)
	public ThreadPoolTaskExecutor defaultTaskProcessor()
	{
		val result = new ThreadPoolTaskExecutor();
		result.setCorePoolSize(minThreads);
		result.setMaxPoolSize(maxThreads);
		result.setQueueCapacity(maxTasks);
		result.setWaitForTasksToCompleteOnShutdown(true);
		return result;
	}

	@Bean
	@Conditional(DefaultTaskHandlerType.class)
	public DAODeliveryTaskExecutor taskExecutor()
	{
		return DAODeliveryTaskExecutor.builder()
				.deliveryTaskDAO(deliveryTaskDAO)
				.deliveryTaskHandler(deliveryTaskHandler())
				.timedTask(new TimedTask(taskHandlerExecutionInterval))
				.maxTasks(maxTasks)
				.serverId(serverId)
				.build();
	}

	@Bean
	@Conditional(JmsTaskHandlerType.class)
	public DefaultMessageListenerContainer jmsTaskProcessor()
	{
		val result = new DefaultMessageListenerContainer();
		result.setConnectionFactory(connectionFactory);
		result.setTransactionManager(jmsTransactionManager);
		result.setSessionTransacted(true);
		result.setConcurrentConsumers(minThreads);
		result.setMaxConcurrentConsumers(maxThreads);
		result.setReceiveTimeout(receiveTimeout);
		result.setDestinationName(StringUtils.isEmpty(jmsDestinationName) ? JMSDeliveryTaskManager.JMS_DESTINATION_NAME : jmsDestinationName);
		result.setMessageListener(new JMSDeliveryTaskListener(deliveryTaskHandler()));
		return result;
	}

	@Bean
  public DeliveryTaskHandler deliveryTaskHandler()
	{
		return DeliveryTaskHandler.builder()
				.messageEventListener(messageEventListener)
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.urlMapper(urlMapper)
				.deliveryTaskManager(deliveryTaskManager)
				.ebMSClientFactory(ebMSClientFactory)
				.messageEncrypter(messageEncrypter)
				.messageProcessor(messageProcessor)
				.timedTask(new TimedTask(taskHandlerTaskExecutionInterval))
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}

	public static class DefaultTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.start",Boolean.class,true)
					&& context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.DEFAULT;
		}
	}
	public static class JmsTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.start",Boolean.class,true)
					&& (context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.JMS
					|| context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ_JMS);
		}
	}
	public static class QuartzTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.start",Boolean.class,true)
					&& (context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ
					|| context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ_JMS
					|| context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ_KAFKA);
		}
	}
	public static class KafkaTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.start",Boolean.class,true)
					&& context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ_KAFKA;
		}
	}
}
