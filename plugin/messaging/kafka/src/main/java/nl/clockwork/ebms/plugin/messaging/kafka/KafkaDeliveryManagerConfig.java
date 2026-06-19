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

import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.client.DeliveryManager;
import nl.clockwork.ebms.client.delivery.DeliveryManagerConfig.DeliveryManagerType;
import nl.clockwork.ebms.client.transport.http.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
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
public class KafkaDeliveryManagerConfig
{
	@Value("${kafka.topic.messageReplies}")
	String messageRepliesTopic;
	@Value("${kafka.consumer.groupIdPrefix}")
	String consumerGroupIdPrefix;
	@Value("${deliveryManager.kafka.replyTimeout:180000}")
	long replyTimeoutMs;

	@Bean
	@Conditional(KafkaDeliveryManagerType.class)
	public KafkaDeliveryManager kafkaDeliveryManager(
			CPAManager cpaManager,
			EbMSHttpClientFactory ebMSClientFactory,
			@Qualifier("ebmsKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate)
	{
		return KafkaDeliveryManager.kafkaDeliveryManagerBuilder()
				.cpaManager(cpaManager)
				.ebMSClientFactory(ebMSClientFactory)
				.kafkaTemplate(kafkaTemplate)
				.replyTopic(messageRepliesTopic)
				.replyTimeoutMs(replyTimeoutMs)
				.build();
	}

	@Bean
	@Conditional(KafkaDeliveryManagerType.class)
	public DeliveryManager kafkaDeliveryManagerAlias(KafkaDeliveryManager kafkaDeliveryManager)
	{
		return kafkaDeliveryManager;
	}

	@Bean(destroyMethod = "stop")
	@Conditional(KafkaDeliveryManagerType.class)
	public ConcurrentMessageListenerContainer<String, Object> kafkaDeliveryManagerReplyContainer(
			@Qualifier("ebmsKafkaConsumerFactory") ConsumerFactory<String, Object> consumerFactory,
			KafkaDeliveryManager kafkaDeliveryManager)
	{
		val containerProps = new ContainerProperties(messageRepliesTopic);
		// Unique per-instance group id so every instance receives every reply and filters in-app by refToMessageId.
		containerProps.setGroupId(consumerGroupIdPrefix + "-replies-" + UUID.randomUUID());
		containerProps.setMessageListener(kafkaDeliveryManager);
		val container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProps);
		container.setConcurrency(1);
		container.start();
		return container;
	}

	public static class KafkaDeliveryManagerType implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryManager.type", DeliveryManagerType.class, DeliveryManagerType.DEFAULT) == DeliveryManagerType.KAFKA;
		}
	}
}
