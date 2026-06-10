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

import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.common.event.LoggingMessageEventListener;
import nl.clockwork.ebms.common.event.MessageEventException;
import nl.clockwork.ebms.common.event.MessageEventType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SimpleKafkaMessageEventListener extends LoggingMessageEventListener
{
	@NonNull
	KafkaTemplate<String, Object> kafkaTemplate;
	@NonNull
	KafkaEventTopicMapper topicMapper;

	private void send(MessageEventType type, String messageId) throws MessageEventException
	{
		try
		{
			final ProducerRecord<String, Object> record = new ProducerRecord<>(topicMapper.topicFor(type), messageId, messageId);
			record.headers().add(new RecordHeader("messageId", messageId.getBytes(StandardCharsets.UTF_8)));
			kafkaTemplate.send(record);
		}
		catch (KafkaException e)
		{
			throw new MessageEventException(e);
		}
	}

	@Override
	public void onMessageReceived(String messageId) throws MessageEventException
	{
		send(MessageEventType.RECEIVED, messageId);
		super.onMessageReceived(messageId);
	}

	@Override
	public void onMessageDelivered(String messageId) throws MessageEventException
	{
		send(MessageEventType.DELIVERED, messageId);
		super.onMessageDelivered(messageId);
	}

	@Override
	public void onMessageFailed(String messageId) throws MessageEventException
	{
		send(MessageEventType.FAILED, messageId);
		super.onMessageFailed(messageId);
	}

	@Override
	public void onMessageExpired(String messageId) throws MessageEventException
	{
		send(MessageEventType.EXPIRED, messageId);
		super.onMessageExpired(messageId);
	}
}
