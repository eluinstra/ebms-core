/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.common.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationAdapterTest
{
	@Test
	void shouldMarshalDurationAsIso8601() throws Exception
	{
		var adapter = new DurationAdapter();
		assertEquals("PT2H30M", adapter.marshal(Duration.ofHours(2).plusMinutes(30)));
	}

	@Test
	void shouldNotFoldDurationsOver24HoursIntoDays() throws Exception
	{
		var adapter = new DurationAdapter();
		assertEquals("PT25H", adapter.marshal(Duration.ofDays(1).plusHours(1)));
	}

	@Test
	void shouldRoundTripDurationThroughAdapter() throws Exception
	{
		var adapter = new DurationAdapter();
		for (Duration duration : new Duration[]{Duration.ofMillis(1500), Duration.ofHours(2).plusMinutes(30), Duration.ofDays(1).plusHours(1), Duration.ZERO})
		{
			var marshalled = adapter.marshal(duration);
			assertEquals(duration, adapter.unmarshal(marshalled), "round trip failed for " + duration);
		}
	}
}
