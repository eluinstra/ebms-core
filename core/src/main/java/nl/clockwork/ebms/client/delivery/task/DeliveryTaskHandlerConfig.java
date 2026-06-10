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
package nl.clockwork.ebms.client.delivery.task;

import jakarta.annotation.PostConstruct;
import jakarta.jms.ConnectionFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.client.delivery.http.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.common.event.MessageEventListener;
import nl.clockwork.ebms.server.processor.EbMSMessageProcessor;
import org.jgroups.JChannel;
import org.jgroups.raft.RaftHandle;
import org.jgroups.raft.StateMachine;
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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskHandlerConfig
{
	private static final String DELIVERY_TASK_HANDLER_START = "deliveryTaskHandler.start";
	private static final String DELIVERY_TASK_HANDLER_TYPE = "deliveryTaskHandler.type";

	public enum DeliveryTaskHandlerType
	{
		DEFAULT, JMS;
	}

	@Value("${ebms.serverId:#{null}}")
	String serverId;
	@Value("${deliveryTaskHandler.start}")
	boolean startTaskHandler;
	@Value("${deliveryTaskHandler.type:DEFAULT}")
	String configuredTaskHandlerType;
	@Value("${deliveryTaskHandler.minThreads}")
	int minThreads;
	@Value("${deliveryTaskHandler.maxThreads}")
	int maxThreads;
	@Value("${deliveryTaskHandler.default.maxTasks}")
	int maxTasks;
	@Value("${deliveryTaskHandler.default.executionInterval}")
	int taskHandlerExecutionInterval;
	@Value("${deliveryTaskHandler.default.leaderCheckIntervalMillis:1000}")
	long leaderCheckIntervalMillis;
	@Value("${deliveryTaskHandler.default.taskAwaitTimeoutMillis:60000}")
	long taskAwaitTimeoutMillis;
	@Value("${deliveryTaskHandler.task.executionInterval}")
	int taskHandlerTaskExecutionInterval;
	@Value("${deliveryTaskHandler.jms.destinationName:DELIVERY_TASK}")
	String jmsDestinationName;
	@Value("${deliveryTaskHandler.jms.receiveTimeout:3000}")
	long jmsReceiveTimeout;
	@Value("${deliveryTaskHandler.jms.concurrentConsumers:1}")
	int jmsConcurrentConsumers;
	@Value("${deliveryTaskHandler.jms.maxConcurrentConsumers:8}")
	int jmsMaxConcurrentConsumers;
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	@Value("${http.uuid.headerName}")
	String uuidHeader;
	@Value("${raft.configLocation:ebms-raft.xml}")
	String raftConfigLocation;
	@Value("${raft.clusterName:ebms-cluster}")
	String raftClusterName;

	@PostConstruct
	public void validateConfiguration()
	{
		try
		{
			DeliveryTaskHandlerType.valueOf(configuredTaskHandlerType);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalStateException(
					"Unsupported value '"
							+ configuredTaskHandlerType
							+ "' for property '"
							+ DELIVERY_TASK_HANDLER_TYPE
							+ "'. Supported values are DEFAULT (Raft-leader DAO executor sending directly via HTTP) and JMS (Raft-leader DAO executor enqueuing tasks on a JMS queue consumed by all nodes).");
		}
	}

	@Bean("deliveryTaskExecutor")
	@Conditional(TaskHandlerActive.class)
	public ThreadPoolTaskExecutor defaultTaskProcessor()
	{
		val result = new ThreadPoolTaskExecutor();
		result.setCorePoolSize(minThreads);
		result.setMaxPoolSize(maxThreads);
		result.setQueueCapacity(maxTasks);
		result.setWaitForTasksToCompleteOnShutdown(true);
		return result;
	}

	@Bean(destroyMethod = "close")
	@Conditional(TaskHandlerActive.class)
	public JChannel raftChannel() throws Exception
	{
		val ch = new JChannel(raftConfigLocation);
		ch.connect(raftClusterName);
		return ch;
	}

	@Bean
	@Conditional(TaskHandlerActive.class)
	public RaftHandle raftHandle(JChannel raftChannel)
	{
		return new RaftHandle(raftChannel, new NoOpStateMachine());
	}

	@Bean
	@Conditional(DefaultTaskHandlerType.class)
	public DeliveryTaskDispatcher directDispatcher(DeliveryTaskHandler deliveryTaskHandler)
	{
		return new DirectDeliveryTaskDispatcher(deliveryTaskHandler);
	}

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

	@Bean
	@Conditional(TaskHandlerActive.class)
	public DAODeliveryTaskExecutor taskExecutor(DeliveryTaskDAO deliveryTaskDAO, DeliveryTaskDispatcher dispatcher, RaftHandle raftHandle)
	{
		return DAODeliveryTaskExecutor.builder()
				.deliveryTaskDAO(deliveryTaskDAO)
				.dispatcher(dispatcher)
				.raftHandle(raftHandle)
				.timedTask(new TimedTask(taskHandlerExecutionInterval))
				.maxTasks(maxTasks)
				.serverId(serverId)
				.leaderCheckIntervalMillis(leaderCheckIntervalMillis)
				.taskAwaitTimeoutMillis(taskAwaitTimeoutMillis)
				.build();
	}

	@Bean
	public DeliveryTaskHandler deliveryTaskHandler(
			MessageEventListener messageEventListener,
			EbMSDAO ebMSDAO,
			CPAManager cpaManager,
			URLMappingRepository urlMappingRepository,
			DeliveryTaskManager deliveryTaskManager,
			EbMSHttpClientFactory ebMSClientFactory,
			EbMSMessageEncrypter messageEncrypter,
			EbMSMessageProcessor messageProcessor)
	{
		return DeliveryTaskHandler.builder()
				.messageEventListener(messageEventListener)
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.urlMappingRepository(urlMappingRepository)
				.deliveryTaskManager(deliveryTaskManager)
				.ebMSClientFactory(ebMSClientFactory)
				.messageEncrypter(messageEncrypter)
				.messageProcessor(messageProcessor)
				.timedTask(new TimedTask(taskHandlerTaskExecutionInterval))
				.uuidHeader(uuidHeader)
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}

	public static class DefaultTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_START, Boolean.class, true)
					&& "DEFAULT".equalsIgnoreCase(context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, "DEFAULT"));
		}
	}

	public static class JmsTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_START, Boolean.class, true)
					&& "JMS".equalsIgnoreCase(context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, "DEFAULT"));
		}
	}

	public static class TaskHandlerActive implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_START, Boolean.class, true);
		}
	}

	private static final class NoOpStateMachine implements StateMachine
	{
		@Override
		public byte[] apply(byte[] data, int offset, int length, boolean serializeResponse)
		{
			return new byte[0];
		}

		@Override
		public void readContentFrom(java.io.DataInput in)
		{
			// no state to read
		}

		@Override
		public void writeContentTo(java.io.DataOutput out)
		{
			// no state to write
		}
	}
}
