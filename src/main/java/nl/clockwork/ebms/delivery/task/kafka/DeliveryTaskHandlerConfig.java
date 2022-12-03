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
package nl.clockwork.ebms.delivery.task.kafka;

import javax.jms.ConnectionFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.delivery.task.jms.JMSDeliveryTaskListener;
import nl.clockwork.ebms.delivery.task.jms.JMSDeliveryTaskManager;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

@Configuration
@ComponentScan(basePackageClasses = {nl.clockwork.ebms.delivery.task.quartz.DeliveryTaskJob.class})
@EnableAsync
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskHandlerConfig
{
	public static final String DELIVERY_TASK_HANDLER_TYPE = "KAFKA";

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
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	@Value("${deliveryTaskHandler.jms.destinationName}")
	String destinationName;

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
