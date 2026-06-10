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

import nl.clockwork.ebms.common.event.MessageEventType;
import org.junit.jupiter.api.Test;

class KafkaEventTopicMapperTest
{
	@Test
	void prependsConfiguredPrefixToEventTypeName()
	{
		final KafkaEventTopicMapper mapper = new KafkaEventTopicMapper("ebms-event-");
		assertThat(mapper.topicFor(MessageEventType.RECEIVED)).isEqualTo("ebms-event-RECEIVED");
		assertThat(mapper.topicFor(MessageEventType.DELIVERED)).isEqualTo("ebms-event-DELIVERED");
		assertThat(mapper.topicFor(MessageEventType.FAILED)).isEqualTo("ebms-event-FAILED");
		assertThat(mapper.topicFor(MessageEventType.EXPIRED)).isEqualTo("ebms-event-EXPIRED");
	}

	@Test
	void supportsEmptyPrefix()
	{
		assertThat(new KafkaEventTopicMapper("").topicFor(MessageEventType.RECEIVED)).isEqualTo("RECEIVED");
	}
}
