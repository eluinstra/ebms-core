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

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.event.MessageEventType;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaTopicConfig
{
	@Value("${kafka.bootstrapServers}")
	String bootstrapServers;
	@Value("${kafka.admin.numPartitions:1}")
	int numPartitions;
	@Value("${kafka.admin.replicationFactor:1}")
	short replicationFactor;
	@Value("${kafka.topic.deliveryTask}")
	String deliveryTaskTopic;
	@Value("${kafka.topic.messageReplies}")
	String messageRepliesTopic;
	@Value("${kafka.topic.eventPrefix}")
	String eventTopicPrefix;

	@Bean("ebmsKafkaAdmin")
	@Conditional(AutoCreateTopicsCondition.class)
	public KafkaAdmin kafkaAdmin()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		final KafkaAdmin admin = new KafkaAdmin(props);
		admin.setFatalIfBrokerNotAvailable(false);
		return admin;
	}

	@Bean
	@Conditional(AutoCreateTopicsCondition.class)
	public NewTopic ebmsDeliveryTaskTopic()
	{
		return new NewTopic(deliveryTaskTopic, numPartitions, replicationFactor);
	}

	@Bean
	@Conditional(AutoCreateTopicsCondition.class)
	public NewTopic ebmsMessageRepliesTopic()
	{
		return new NewTopic(messageRepliesTopic, numPartitions, replicationFactor);
	}

	@Bean
	@Conditional(AutoCreateEventTopicsCondition.class)
	public KafkaTopicSet ebmsEventTopics()
	{
		final KafkaTopicSet set = new KafkaTopicSet();
		for (final MessageEventType type : MessageEventType.values())
			set.add(new NewTopic(eventTopicPrefix + type.name(), numPartitions, replicationFactor));
		return set;
	}

	public static class AutoCreateTopicsCondition implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("kafka.admin.autoCreate", Boolean.class, true);
		}
	}

	public static class AutoCreateEventTopicsCondition implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			if (!context.getEnvironment().getProperty("kafka.admin.autoCreate", Boolean.class, true))
				return false;
			final String type = context.getEnvironment().getProperty("eventListener.type", "DEFAULT");
			return "SIMPLE_KAFKA".equalsIgnoreCase(type) || "KAFKA".equalsIgnoreCase(type) || "KAFKA_TEXT".equalsIgnoreCase(type);
		}
	}
}
