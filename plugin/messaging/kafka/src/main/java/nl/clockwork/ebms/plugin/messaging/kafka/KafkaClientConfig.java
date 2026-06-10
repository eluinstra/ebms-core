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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaClientConfig
{
	@Value("${kafka.bootstrapServers}")
	String bootstrapServers;
	@Value("${kafka.clientId}")
	String clientId;
	@Value("${kafka.consumer.groupIdPrefix}")
	String consumerGroupIdPrefix;
	@Value("${kafka.consumer.autoOffsetReset}")
	String consumerAutoOffsetReset;
	@Value("${kafka.producer.acks}")
	String producerAcks;
	@Value("${kafka.producer.enableIdempotence}")
	boolean producerEnableIdempotence;

	@Bean("ebmsKafkaProducerProperties")
	public Map<String, Object> producerProperties()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId + "-producer");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JdkSerializationSerializer.class.getName());
		props.put(ProducerConfig.ACKS_CONFIG, producerAcks);
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producerEnableIdempotence);
		return props;
	}

	@Bean("ebmsKafkaConsumerProperties")
	public Map<String, Object> consumerProperties()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId + "-consumer");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JdkSerializationDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerAutoOffsetReset);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		return props;
	}

	@Bean("ebmsKafkaProducerFactory")
	public ProducerFactory<String, Object> producerFactory()
	{
		return new DefaultKafkaProducerFactory<>(producerProperties());
	}

	@Bean("ebmsKafkaTemplate")
	public KafkaTemplate<String, Object> kafkaTemplate()
	{
		return new KafkaTemplate<>(producerFactory());
	}

	@Bean("ebmsKafkaConsumerFactory")
	public ConsumerFactory<String, Object> consumerFactory()
	{
		return new DefaultKafkaConsumerFactory<>(consumerProperties());
	}

	public String getConsumerGroupIdPrefix()
	{
		return consumerGroupIdPrefix;
	}
}
