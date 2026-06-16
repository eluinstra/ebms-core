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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaDeliveryManagerReplyDispatchTest
{
	@Test
	@SuppressWarnings("unchecked")
	void ignoresReplyWithNoMatchingPendingMessageId()
	{
		final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
		final KafkaDeliveryManager manager = KafkaDeliveryManager.kafkaDeliveryManagerBuilder()
				.cpaManager(mock(nl.clockwork.ebms.common.cpa.CPAManager.class))
				.ebMSClientFactory(mock(nl.clockwork.ebms.client.transport.http.EbMSHttpClientFactory.class))
				.kafkaTemplate(template)
				.replyTopic("ebms-message-replies")
				.replyTimeoutMs(1000)
				.build();

		// No pendingReplies registered -> onMessage must complete without throwing.
		manager.onMessage(new ConsumerRecord<>("ebms-message-replies", 0, 0L, "unknown-id", null));
	}

	@Test
	@SuppressWarnings("unchecked")
	void ignoresReplyWithNullKey()
	{
		final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
		final KafkaDeliveryManager manager = KafkaDeliveryManager.kafkaDeliveryManagerBuilder()
				.cpaManager(mock(nl.clockwork.ebms.common.cpa.CPAManager.class))
				.ebMSClientFactory(mock(nl.clockwork.ebms.client.transport.http.EbMSHttpClientFactory.class))
				.kafkaTemplate(template)
				.replyTopic("ebms-message-replies")
				.replyTimeoutMs(1000)
				.build();

		manager.onMessage(new ConsumerRecord<>("ebms-message-replies", 0, 0L, null, null));
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerAndRemovePendingAreSymmetric()
	{
		final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
		final KafkaDeliveryManager manager = KafkaDeliveryManager.kafkaDeliveryManagerBuilder()
				.cpaManager(mock(nl.clockwork.ebms.common.cpa.CPAManager.class))
				.ebMSClientFactory(mock(nl.clockwork.ebms.client.transport.http.EbMSHttpClientFactory.class))
				.kafkaTemplate(template)
				.replyTopic("ebms-message-replies")
				.replyTimeoutMs(1000)
				.build();

		final var future = manager.registerPending("msg-1");
		assertThat(future).isNotNull();
		assertThat(future.isDone()).isFalse();
		manager.removePending("msg-1");
		// Subsequent onMessage with that id should be a no-op since the entry is gone.
		manager.onMessage(new ConsumerRecord<>("ebms-message-replies", 0, 0L, "msg-1", null));
		assertThat(future.isDone()).isFalse();
	}
}
