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

class EbMSActionTest
{
	@Test
	void testEbMSActionValues()
	{
		assertThat(EbMSAction.MESSAGE_ERROR.getAction()).isEqualTo("MessageError");
		assertThat(EbMSAction.ACKNOWLEDGMENT.getAction()).isEqualTo("Acknowledgment");
		assertThat(EbMSAction.STATUS_REQUEST.getAction()).isEqualTo("StatusRequest");
		assertThat(EbMSAction.STATUS_RESPONSE.getAction()).isEqualTo("StatusResponse");
		assertThat(EbMSAction.PING.getAction()).isEqualTo("Ping");
		assertThat(EbMSAction.PONG.getAction()).isEqualTo("Pong");
	}

	@Test
	void testGetExistingAction()
	{
		assertThat(EbMSAction.get("MessageError")).isPresent().hasValue(EbMSAction.MESSAGE_ERROR);
		assertThat(EbMSAction.get("Acknowledgment")).isPresent().hasValue(EbMSAction.ACKNOWLEDGMENT);
		assertThat(EbMSAction.get("StatusRequest")).isPresent().hasValue(EbMSAction.STATUS_REQUEST);
		assertThat(EbMSAction.get("StatusResponse")).isPresent().hasValue(EbMSAction.STATUS_RESPONSE);
		assertThat(EbMSAction.get("Ping")).isPresent().hasValue(EbMSAction.PING);
		assertThat(EbMSAction.get("Pong")).isPresent().hasValue(EbMSAction.PONG);
	}

	@Test
	void testGetNonExistingAction()
	{
		assertThat(EbMSAction.get("NonExistingAction")).isEmpty();
	}

	@Test
	void testStream()
	{
		assertThat(EbMSAction.stream()).hasSize(6);
	}
}
