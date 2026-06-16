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
import nl.clockwork.ebms.client.sync.EbMSDAO;
import nl.clockwork.ebms.common.event.MessageEventListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaMessageEventListenerConfig
{
	@Value("${kafka.topic.eventPrefix}")
	String eventTopicPrefix;

	@Bean
	public KafkaEventTopicMapper kafkaEventTopicMapper()
	{
		return new KafkaEventTopicMapper(eventTopicPrefix);
	}

	@Bean
	@Conditional(SimpleKafkaEventListenerType.class)
	public
			MessageEventListener
			simpleKafkaMessageEventListener(@Qualifier("ebmsKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate, KafkaEventTopicMapper topicMapper)
	{
		return new SimpleKafkaMessageEventListener(kafkaTemplate, topicMapper);
	}

	@Bean
	@Conditional(KafkaEventListenerType.class)
	public
			MessageEventListener
			kafkaMessageEventListener(@Qualifier("ebmsKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate, KafkaEventTopicMapper topicMapper, EbMSDAO ebMSDAO)
	{
		return new KafkaMessageEventListener(ebMSDAO, kafkaTemplate, topicMapper);
	}

	@Bean
	@Conditional(KafkaTextEventListenerType.class)
	public MessageEventListener kafkaTextMessageEventListener(
			@Qualifier("ebmsKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
			KafkaEventTopicMapper topicMapper,
			EbMSDAO ebMSDAO)
	{
		return new KafkaTextMessageEventListener(ebMSDAO, kafkaTemplate, topicMapper);
	}

	public static class SimpleKafkaEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return "SIMPLE_KAFKA".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}

	public static class KafkaEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return "KAFKA".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}

	public static class KafkaTextEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return "KAFKA_TEXT".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}
}
