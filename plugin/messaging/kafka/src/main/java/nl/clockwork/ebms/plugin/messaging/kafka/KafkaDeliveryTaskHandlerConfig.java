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
package nl.clockwork.ebms.plugin.messaging.kafka;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.api.DeliveryTaskDispatcher;
import nl.clockwork.ebms.client.async.handler.DeliveryTaskHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaDeliveryTaskHandlerConfig
{
	private static final String DELIVERY_TASK_HANDLER_START = "deliveryTaskHandler.start";
	private static final String DELIVERY_TASK_HANDLER_TYPE = "deliveryTaskHandler.type";

	@Value("${kafka.topic.deliveryTask}")
	String deliveryTaskTopic;
	@Value("${kafka.consumer.groupIdPrefix}")
	String consumerGroupIdPrefix;
	@Value("${deliveryTaskHandler.kafka.concurrency:1}")
	int concurrency;
	@Value("${deliveryTaskHandler.kafka.pollTimeout:3000}")
	long pollTimeout;

	@Bean
	@Conditional(KafkaTaskHandlerType.class)
	public DeliveryTaskDispatcher kafkaDeliveryTaskDispatcher(@Qualifier("ebmsKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate)
	{
		return new KafkaDeliveryTaskDispatcher(kafkaTemplate, deliveryTaskTopic);
	}

	@Bean(destroyMethod = "stop")
	@Conditional(KafkaTaskHandlerType.class)
	public ConcurrentMessageListenerContainer<String, Object> kafkaDeliveryTaskListenerContainer(
			@Qualifier("ebmsKafkaConsumerFactory") ConsumerFactory<String, Object> consumerFactory,
			DeliveryTaskHandler deliveryTaskHandler)
	{
		val containerProps = new ContainerProperties(deliveryTaskTopic);
		containerProps.setGroupId(consumerGroupIdPrefix + "-delivery-task");
		containerProps.setMessageListener(new KafkaDeliveryTaskListener(deliveryTaskHandler));
		containerProps.setPollTimeout(pollTimeout);
		val container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProps);
		container.setConcurrency(concurrency);
		container.start();
		return container;
	}

	public static class KafkaTaskHandlerType implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_START, Boolean.class, true)
					&& "KAFKA".equalsIgnoreCase(context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, "DEFAULT"));
		}
	}
}
