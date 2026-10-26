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

import java.time.Instant;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class InstantConverterTest
{
	@Test
	void shouldMarshalInstantWithUtcOffsetRegardlessOfDefaultTimeZone()
	{
		Instant instant = Instant.parse("2023-09-18T12:00:00Z");
		assertMarshaledAsUtc(instant, TimeZone.getTimeZone("Asia/Kolkata"));
		assertMarshaledAsUtc(instant, TimeZone.getTimeZone("America/New_York"));
	}

	@Test
	void shouldUnmarshalUtcDateTimeToInstant()
	{
		assertEquals(Instant.parse("2023-09-18T12:00:00Z"), InstantConverter.parseDateTime("2023-09-18T12:00:00Z"));
	}

	@Test
	void shouldRoundTripThroughAdapterWithNonUtcDefaultTimeZone() throws Exception
	{
		Instant instant = Instant.parse("2023-09-18T12:00:00Z");
		var original = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
		try
		{
			var adapter = new InstantAdapter();
			String marshalled = adapter.marshal(instant);
			assertEquals("2023-09-18T12:00:00Z", marshalled);
			assertEquals(instant, adapter.unmarshal(marshalled));
		}
		finally
		{
			TimeZone.setDefault(original);
		}
	}

	private static void assertMarshaledAsUtc(Instant instant, TimeZone timeZone)
	{
		var original = TimeZone.getDefault();
		TimeZone.setDefault(timeZone);
		try
		{
			assertEquals("2023-09-18T12:00:00Z", InstantConverter.printDateTime(instant));
		}
		finally
		{
			TimeZone.setDefault(original);
		}
	}
}
