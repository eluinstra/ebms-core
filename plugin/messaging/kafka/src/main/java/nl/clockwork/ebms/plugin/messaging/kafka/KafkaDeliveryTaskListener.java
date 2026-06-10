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
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.client.delivery.task.DeliveryTask;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class KafkaDeliveryTaskListener implements MessageListener<String, Object>
{
	@NonNull
	DeliveryTaskHandler deliveryTaskHandler;

	@Override
	public void onMessage(ConsumerRecord<String, Object> record)
	{
		final Object value = record.value();
		if (value instanceof DeliveryTask task)
			deliveryTaskHandler.handle(task);
		else
			log.warn("Ignoring Kafka record on topic {} with unexpected payload type {}", record.topic(), value == null ? "null" : value.getClass().getName());
	}
}
