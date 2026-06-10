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
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.common.event.LoggingMessageEventListener;
import nl.clockwork.ebms.common.event.MessageEventException;
import nl.clockwork.ebms.common.event.MessageEventType;
import nl.clockwork.ebms.common.model.EbMSMessageProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class KafkaTextMessageEventListener extends LoggingMessageEventListener
{
	private static final String TEXT_PAYLOAD = "EbMS Message Context";

	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	KafkaTemplate<String, Object> kafkaTemplate;
	@NonNull
	KafkaEventTopicMapper topicMapper;

	private void send(MessageEventType type, EbMSMessageProperties p) throws MessageEventException
	{
		try
		{
			final ProducerRecord<String, Object> record = new ProducerRecord<>(topicMapper.topicFor(type), p.getMessageId(), TEXT_PAYLOAD);
			KafkaMessageEventListener.addHeaders(record.headers(), p);
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
		ebMSDAO.getEbMSMessageProperties(messageId).ifPresent(p -> sendQuiet(MessageEventType.RECEIVED, p));
		super.onMessageReceived(messageId);
	}

	@Override
	public void onMessageDelivered(String messageId) throws MessageEventException
	{
		ebMSDAO.getEbMSMessageProperties(messageId).ifPresent(p -> sendQuiet(MessageEventType.DELIVERED, p));
		super.onMessageDelivered(messageId);
	}

	@Override
	public void onMessageFailed(String messageId) throws MessageEventException
	{
		ebMSDAO.getEbMSMessageProperties(messageId).ifPresent(p -> sendQuiet(MessageEventType.FAILED, p));
		super.onMessageFailed(messageId);
	}

	@Override
	public void onMessageExpired(String messageId) throws MessageEventException
	{
		ebMSDAO.getEbMSMessageProperties(messageId).ifPresent(p -> sendQuiet(MessageEventType.EXPIRED, p));
		super.onMessageExpired(messageId);
	}

	private void sendQuiet(MessageEventType type, EbMSMessageProperties p)
	{
		try
		{
			send(type, p);
		}
		catch (MessageEventException e)
		{
			throw new KafkaException(e.getMessage(), e);
		}
	}
}
