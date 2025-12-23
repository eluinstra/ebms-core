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
package nl.clockwork.ebms.plugin.messaging.jms.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.jms.ConnectionFactory;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.deliverytask.JmsTaskHandlerType;

@Configuration
@ComponentScan(basePackageClasses = {nl.clockwork.ebms.delivery.task.DeliveryTaskJob.class, nl.clockwork.ebms.delivery.task.JMSJob.class})
@EnableAsync
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskHandlerConfig
{
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

	@Bean
	@Conditional(JmsTaskHandlerType.class)
	public DefaultMessageListenerContainer jmsTaskProcessor(
			ConnectionFactory connectionFactory,
			@Qualifier("jmsTransactionManager") PlatformTransactionManager jmsTransactionManager,
			DeliveryTaskHandler deliveryTaskHandler)
	{
		val result = new DefaultMessageListenerContainer();
		result.setConnectionFactory(connectionFactory);
		result.setTransactionManager(jmsTransactionManager);
		result.setSessionTransacted(true);
		result.setConcurrentConsumers(minThreads);
		result.setMaxConcurrentConsumers(maxThreads);
		result.setReceiveTimeout(receiveTimeout);
		result.setDestinationName(StringUtils.isEmpty(jmsDestinationName) ? JMSDeliveryTaskManager.JMS_DESTINATION_NAME : jmsDestinationName);
		result.setMessageListener(new JMSDeliveryTaskListener(deliveryTaskHandler));
		return result;
	}
}
