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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import nl.clockwork.ebms.common.event.MessageEventType;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
class KafkaMessageEventListenerIT
{
	@Container
	static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.8.0");

	@Test
	void simpleListenerEmitsMessageIdKeyedRecordWithHeader() throws Exception
	{
		final KafkaTemplate<String, Object> template = KafkaTestSupport.newTemplate(KAFKA.getBootstrapServers());
		final KafkaEventTopicMapper mapper = new KafkaEventTopicMapper("ebms-event-");
		final SimpleKafkaMessageEventListener listener = new SimpleKafkaMessageEventListener(template, mapper);

		listener.onMessageReceived("msg-evt-1");
		template.flush();

		try (KafkaConsumer<String, Object> consumer =
				new KafkaConsumer<>(KafkaTestSupport.consumerProps(KAFKA.getBootstrapServers(), KafkaTestSupport.randomGroupId("it-evt"))))
		{
			consumer.subscribe(Collections.singletonList("ebms-event-" + MessageEventType.RECEIVED.name()));
			final var record = KafkaTestSupport.awaitOne(consumer, Duration.ofSeconds(30));
			assertThat(record.key()).isEqualTo("msg-evt-1");
			assertThat(record.value()).isEqualTo("msg-evt-1");
			final var header = record.headers().lastHeader("messageId");
			assertThat(header).isNotNull();
			assertThat(new String(header.value(), StandardCharsets.UTF_8)).isEqualTo("msg-evt-1");
		}
	}
}
