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
package nl.clockwork.ebms.plugin.messaging.jms;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.client.client.DeliveryTask;
import nl.clockwork.ebms.client.client.DeliveryTaskDispatcher;
import org.springframework.jms.core.JmsTemplate;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JMSDeliveryTaskDispatcher implements DeliveryTaskDispatcher
{
	@NonNull
	JmsTemplate jmsTemplate;
	@NonNull
	String destinationName;

	@Override
	public Future<Void> dispatch(DeliveryTask task)
	{
		log.debug("Enqueuing delivery task {} on {}", task.getMessageId(), destinationName);
		jmsTemplate.convertAndSend(destinationName, task, message ->
		{
			message.setJMSCorrelationID(task.getMessageId());
			return message;
		});
		return CompletableFuture.completedFuture(null);
	}
}
