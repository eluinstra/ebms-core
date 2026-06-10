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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Test helpers for the Kafka Testcontainers-based integration tests.
 */
final class KafkaTestSupport
{
	private KafkaTestSupport()
	{
	}

	static Map<String, Object> producerProps(String bootstrap)
	{
		final Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JdkSerializationSerializer.class.getName());
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		return props;
	}

	static Map<String, Object> consumerProps(String bootstrap, String groupId)
	{
		final Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JdkSerializationDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		return props;
	}

	static KafkaTemplate<String, Object> newTemplate(String bootstrap)
	{
		return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps(bootstrap)));
	}

	static DefaultKafkaConsumerFactory<String, Object> newConsumerFactory(String bootstrap, String groupId)
	{
		return new DefaultKafkaConsumerFactory<>(consumerProps(bootstrap, groupId));
	}

	static ConsumerRecord<String, Object> awaitOne(KafkaConsumer<String, Object> consumer, Duration timeout)
	{
		final long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline)
		{
			final ConsumerRecords<String, Object> polled = consumer.poll(Duration.ofMillis(500));
			if (!polled.isEmpty())
				return polled.iterator().next();
		}
		throw new AssertionError("Timed out waiting for a Kafka record");
	}

	static String randomGroupId(String prefix)
	{
		return prefix + "-" + UUID.randomUUID();
	}

	static ProducerRecord<String, Object> recordOf(String topic, String key, Object value)
	{
		return new ProducerRecord<>(topic, key, value);
	}
}
