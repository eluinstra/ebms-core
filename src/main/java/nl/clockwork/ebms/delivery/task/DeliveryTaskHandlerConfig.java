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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

@Configuration
@EnableAsync
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskHandlerConfig
{
	public static final String DELIVERY_TASK_HANDLER_TYPE = "DEFAULT";

	@Value("${ebms.serverId:#{null}}")
	String serverId;
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
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	@Value("${deliveryTaskManager.nrAutoRetries}")
	int nrAutoRetries;
	@Value("${deliveryTaskManager.autoRetryInterval}")
	int autoRetryInterval;

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
	public DAODeliveryTaskExecutor taskExecutor(DeliveryTaskDAO deliveryTaskDAO, DeliveryTaskHandler deliveryTaskHandler)
	{
		return DAODeliveryTaskExecutor.builder()
				.deliveryTaskDAO(deliveryTaskDAO)
				.deliveryTaskHandler(deliveryTaskHandler)
				.timedTask(new TimedTask(taskHandlerExecutionInterval))
				.maxTasks(maxTasks)
				.serverId(serverId)
				.build();
	}

	@Bean
  public DeliveryTaskHandler deliveryTaskHandler(
		MessageEventListener messageEventListener,
		EbMSDAO ebMSDAO,
		CPAManager cpaManager,
		URLMapper urlMapper,
		DeliveryTaskManager deliveryTaskManager,
		EbMSHttpClientFactory ebMSClientFactory,
		EbMSMessageEncrypter messageEncrypter,
		EbMSMessageProcessor messageProcessor)
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

	@Bean
	@Conditional(DefaultTaskManagerType.class)
	public DeliveryTaskManager defaultDeliveryTaskManager(EbMSDAO ebMSDAO, DeliveryTaskDAO deliveryTaskDAO, CPAManager cpaManager)
	{
		return new DAODeliveryTaskManager(ebMSDAO,deliveryTaskDAO,cpaManager,serverId,nrAutoRetries,autoRetryInterval);
	}

	@Bean
	public DeliveryTaskDAO deliveryTaskDAO(DataSource dataSource)
	{
		return new DeliveryTaskDAOImpl(new JdbcTemplate(dataSource));
	}

	public static class DefaultTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.start",Boolean.class,true)
					&& context.getEnvironment().getProperty("deliveryTaskHandler.type",String.class,"").equals(DELIVERY_TASK_HANDLER_TYPE);
		}
	}
	public static class DefaultTaskManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.type",String.class,"").equals(DELIVERY_TASK_HANDLER_TYPE);
		}
	}
}
