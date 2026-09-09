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
package nl.clockwork.ebms.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import nl.clockwork.ebms.validation.ValidationException;
import org.junit.jupiter.api.Test;

class LimitedInputStreamTest
{
	@Test
	void readsWithinLimit() throws IOException
	{
		var stream = new LimitedInputStream(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), 10);
		byte[] buffer = new byte[10];
		assertEquals(5, stream.read(buffer));
		assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), new String(buffer, 0, 5).getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void rejectsReadsOverLimit()
	{
		var stream = new LimitedInputStream(new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)), 5);
		assertThrows(ValidationException.class, () -> stream.read(new byte[10], 0, 10));
	}

	@Test
	void rejectsSingleByteReadsOverLimit() throws IOException
	{
		var stream = new LimitedInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}), 2);
		assertEquals(1, stream.read());
		assertEquals(2, stream.read());
		assertThrows(ValidationException.class, stream::read);
	}

	@Test
	void rejectsNegativeLimit()
	{
		assertThrows(IllegalArgumentException.class, () -> new LimitedInputStream(new ByteArrayInputStream(new byte[0]), -1));
	}
}
