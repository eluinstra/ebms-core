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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.api.DeliveryTask;
import nl.clockwork.ebms.client.api.DeliveryTaskDispatcher;
import org.springframework.kafka.core.KafkaTemplate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class KafkaDeliveryTaskDispatcher implements DeliveryTaskDispatcher
{
	@NonNull
	KafkaTemplate<String, Object> kafkaTemplate;
	@NonNull
	String topic;

	@Override
	public Future<Void> dispatch(DeliveryTask task)
	{
		kafkaTemplate.send(topic, task.getMessageId(), task);
		return CompletableFuture.completedFuture(null);
	}
}
