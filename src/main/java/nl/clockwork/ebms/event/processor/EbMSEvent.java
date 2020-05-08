/**
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
package nl.clockwork.ebms.event.processor;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
@AllArgsConstructor
public class EbMSEvent
{
	@NonNull
	String cpaId;
	@NonNull
	String sendDeliveryChannelId;
	@NonNull
	String receiveDeliveryChannelId;
	@NonNull
	String messageId;
	Instant timeToLive;
	@NonNull
	Instant timestamp;
	boolean isConfidential;
	int retries;
}
