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
package nl.clockwork.ebms.client.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JMSDeliveryManagerTest
{
	@Test
	void shouldCreateSelectorForSimpleCorrelationId()
	{
		assertEquals("JMSCorrelationID='abc-123'", JMSDeliveryManager.createCorrelationIdSelector("abc-123"));
	}

	@Test
	void shouldEscapeSingleQuotesInCorrelationIdSelector()
	{
		assertEquals("JMSCorrelationID='ab''c'", JMSDeliveryManager.createCorrelationIdSelector("ab'c"));
	}

	@Test
	void shouldRejectBlankCorrelationId()
	{
		assertThrows(IllegalArgumentException.class, () -> JMSDeliveryManager.createCorrelationIdSelector("  "));
	}

	@Test
	void shouldRejectCorrelationIdContainingNewLine()
	{
		assertThrows(IllegalArgumentException.class, () -> JMSDeliveryManager.createCorrelationIdSelector("ab\nc"));
	}
}
