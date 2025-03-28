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
package nl.clockwork.ebms.delivery.task;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
@AllArgsConstructor
public class DeliveryTask
{
	@NonNull
	String cpaId;
	String sendDeliveryChannelId;
	@NonNull
	String receiveDeliveryChannelId;
	@NonNull
	String messageId;
	Instant timeToLive;
	@NonNull
	Instant timestamp;
	boolean confidential;
	int retries;

	public DeliveryTask createNextTask(Instant timestamp)
	{
		return DeliveryTask.builder()
				.cpaId(cpaId)
				.sendDeliveryChannelId(sendDeliveryChannelId)
				.receiveDeliveryChannelId(receiveDeliveryChannelId)
				.messageId(messageId)
				.timeToLive(timeToLive)
				.timestamp(timestamp)
				.confidential(confidential)
				.retries(retries + 1)
				.build();
	}
}
