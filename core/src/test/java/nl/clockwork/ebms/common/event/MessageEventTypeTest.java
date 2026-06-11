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
package nl.clockwork.ebms.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessageEventTypeTest
{
	@Test
	void testMessageEventTypeValues()
	{
		assertThat(MessageEventType.RECEIVED.getId()).isEqualTo(0);
		assertThat(MessageEventType.DELIVERED.getId()).isEqualTo(1);
		assertThat(MessageEventType.FAILED.getId()).isEqualTo(2);
		assertThat(MessageEventType.EXPIRED.getId()).isEqualTo(3);
	}

	@Test
	void testGetExistingId()
	{
		assertThat(MessageEventType.get(0)).isPresent().hasValue(MessageEventType.RECEIVED);
		assertThat(MessageEventType.get(1)).isPresent().hasValue(MessageEventType.DELIVERED);
		assertThat(MessageEventType.get(2)).isPresent().hasValue(MessageEventType.FAILED);
		assertThat(MessageEventType.get(3)).isPresent().hasValue(MessageEventType.EXPIRED);
	}

	@Test
	void testGetNonExistingId()
	{
		assertThat(MessageEventType.get(99)).isEmpty();
	}

	@Test
	void testStream()
	{
		assertThat(MessageEventType.stream()).hasSize(4);
	}
}
