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
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import nl.clockwork.ebms.client.delivery.http.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.model.EbMSPong;
import nl.clockwork.ebms.common.model.EbMSResponseMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageData;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Verifies the reply-correlation path of {@link KafkaDeliveryManager}: registered futures are completed when a record with the matching key lands on the reply
 * topic, and outbound replies produced via {@code handleResponseMessage} are keyed by {@code refToMessageId} on the configured topic.
 */
@Testcontainers
class KafkaDeliveryManagerIT
{
	@Container
	static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.8.0");

	@Test
	void onMessageCompletesPendingFutureForMatchingRefToMessageId() throws Exception
	{
		final KafkaTemplate<String, Object> template = KafkaTestSupport.newTemplate(KAFKA.getBootstrapServers());
		final KafkaDeliveryManager manager = KafkaDeliveryManager.kafkaDeliveryManagerBuilder()
				.cpaManager(mock(CPAManager.class))
				.ebMSClientFactory(mock(EbMSHttpClientFactory.class))
				.kafkaTemplate(template)
				.replyTopic("ebms-message-replies")
				.replyTimeoutMs(10_000)
				.build();

		final CompletableFuture<EbMSResponseMessage> future = manager.registerPending("msg-ref-1");
		final EbMSPong pong = EbMSPong.builder().messageHeader(newMessageHeader("msg-ref-1")).build();
		manager.onMessage(new ConsumerRecord<>("ebms-message-replies", 0, 0L, "msg-ref-1", pong));

		assertThat(future.get(2, TimeUnit.SECONDS)).isSameAs(pong);
	}

	@Test
	void handleResponseMessagePublishesToReplyTopic() throws Exception
	{
		final KafkaTemplate<String, Object> template = KafkaTestSupport.newTemplate(KAFKA.getBootstrapServers());
		final KafkaDeliveryManager manager = KafkaDeliveryManager.kafkaDeliveryManagerBuilder()
				.cpaManager(mock(CPAManager.class))
				.ebMSClientFactory(mock(EbMSHttpClientFactory.class))
				.kafkaTemplate(template)
				.replyTopic("ebms-message-replies")
				.replyTimeoutMs(10_000)
				.build();

		final EbMSPong pong = EbMSPong.builder().messageHeader(newMessageHeader("msg-ref-2")).build();
		manager.handleResponseMessage(pong);

		try (KafkaConsumer<String, Object> consumer =
				new KafkaConsumer<>(KafkaTestSupport.consumerProps(KAFKA.getBootstrapServers(), KafkaTestSupport.randomGroupId("it-mgr"))))
		{
			consumer.subscribe(Collections.singletonList("ebms-message-replies"));
			final var record = KafkaTestSupport.awaitOne(consumer, Duration.ofSeconds(30));
			assertThat(record.key()).isEqualTo("msg-ref-2");
			assertThat(record.value()).isInstanceOf(EbMSPong.class);
		}
	}

	private static MessageHeader newMessageHeader(String refToMessageId)
	{
		final MessageHeader header = new MessageHeader();
		final MessageData data = new MessageData();
		data.setRefToMessageId(refToMessageId);
		header.setMessageData(data);
		return header;
	}
}
