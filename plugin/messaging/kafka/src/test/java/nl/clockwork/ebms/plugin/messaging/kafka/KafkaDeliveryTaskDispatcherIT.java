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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import nl.clockwork.ebms.client.client.DeliveryTask;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
class KafkaDeliveryTaskDispatcherIT
{
	@Container
	static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.8.0");

	@Test
	void dispatchedTaskIsConsumableFromTopic()
	{
		final String topic = "ebms-delivery-task";
		final KafkaTemplate<String, Object> template = KafkaTestSupport.newTemplate(KAFKA.getBootstrapServers());
		final KafkaDeliveryTaskDispatcher dispatcher = new KafkaDeliveryTaskDispatcher(template, topic);

		final DeliveryTask task =
				DeliveryTask.builder().cpaId("cpa-it").receiveDeliveryChannelId("rcv").messageId("msg-it-1").timestamp(Instant.parse("2026-01-01T00:00:00Z")).build();

		dispatcher.dispatch(task);
		template.flush();

		try (KafkaConsumer<String, Object> consumer =
				new KafkaConsumer<>(KafkaTestSupport.consumerProps(KAFKA.getBootstrapServers(), KafkaTestSupport.randomGroupId("it"))))
		{
			consumer.subscribe(Collections.singletonList(topic));
			final var record = KafkaTestSupport.awaitOne(consumer, Duration.ofSeconds(30));
			assertThat(record.key()).isEqualTo("msg-it-1");
			assertThat(record.value()).isInstanceOf(DeliveryTask.class);
			final DeliveryTask received = (DeliveryTask)record.value();
			assertThat(received.getMessageId()).isEqualTo("msg-it-1");
			assertThat(received.getCpaId()).isEqualTo("cpa-it");
		}
	}
}
