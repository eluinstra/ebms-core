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
package nl.clockwork.ebms.common.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EbMSMessageStatusTest
{
	@Test
	void testEbMSMessageStatusValues()
	{
		assertThat(EbMSMessageStatus.UNAUTHORIZED.getId()).isEqualTo(0);
		assertThat(EbMSMessageStatus.NOT_RECOGNIZED.getId()).isEqualTo(1);
		assertThat(EbMSMessageStatus.RECEIVED.getId()).isEqualTo(2);
		assertThat(EbMSMessageStatus.PROCESSED.getId()).isEqualTo(3);
		assertThat(EbMSMessageStatus.FORWARDED.getId()).isEqualTo(4);
		assertThat(EbMSMessageStatus.FAILED.getId()).isEqualTo(5);
		assertThat(EbMSMessageStatus.CREATED.getId()).isEqualTo(10);
		assertThat(EbMSMessageStatus.DELIVERY_FAILED.getId()).isEqualTo(11);
		assertThat(EbMSMessageStatus.DELIVERED.getId()).isEqualTo(12);
		assertThat(EbMSMessageStatus.EXPIRED.getId()).isEqualTo(13);
	}

	@Test
	void testReceiveStatus()
	{
		// Test that receive status values are correct
		assertThat(EbMSMessageStatus.UNAUTHORIZED.getId()).isEqualTo(0);
		assertThat(EbMSMessageStatus.NOT_RECOGNIZED.getId()).isEqualTo(1);
		assertThat(EbMSMessageStatus.RECEIVED.getId()).isEqualTo(2);
		assertThat(EbMSMessageStatus.PROCESSED.getId()).isEqualTo(3);
		assertThat(EbMSMessageStatus.FORWARDED.getId()).isEqualTo(4);
		assertThat(EbMSMessageStatus.FAILED.getId()).isEqualTo(5);
	}

	@Test
	void testSendStatus()
	{
		// Test that send status values are correct
		assertThat(EbMSMessageStatus.CREATED.getId()).isEqualTo(10);
		assertThat(EbMSMessageStatus.DELIVERY_FAILED.getId()).isEqualTo(11);
		assertThat(EbMSMessageStatus.DELIVERED.getId()).isEqualTo(12);
		assertThat(EbMSMessageStatus.EXPIRED.getId()).isEqualTo(13);
	}
}
