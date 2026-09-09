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
package nl.clockwork.ebms.server.servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class BasicAuthenticationFilterTest
{
	private String generateHash(String password) throws Exception
	{
		var out = System.out;
		var buffer = new ByteArrayOutputStream();
		try
		{
			System.setOut(new PrintStream(buffer, true));
			BasicAuthenticationFilter.main(new String[]{password});
		}
		finally
		{
			System.setOut(out);
		}
		return buffer.toString().trim();
	}

	@Test
	void generatedHashVerifiesForCorrectPassword() throws Exception
	{
		var filter = new BasicAuthenticationFilter();
		var hash = generateHash("s3cr3t");
		assertTrue(filter.checkPassword(hash, "s3cr3t"), "correct password must verify");
	}

	@Test
	void generatedHashRejectsWrongPassword() throws Exception
	{
		var filter = new BasicAuthenticationFilter();
		var hash = generateHash("s3cr3t");
		assertFalse(filter.checkPassword(hash, "wrong"), "wrong password must not verify");
	}

	@Test
	void generatedHashesAreSalted() throws Exception
	{
		assertFalse(generateHash("s3cr3t").equals(generateHash("s3cr3t")), "two hashes of the same password must differ (salt)");
	}

	@Test
	void rejectsLegacyWeakFormats()
	{
		var filter = new BasicAuthenticationFilter();
		assertFalse(filter.checkPassword("MD5:5d41402abc4b2a76b9719d911017c592", "password"));
		assertFalse(filter.checkPassword("OBF:1g2a5z", "password"));
		assertFalse(filter.checkPassword("CRYPT:password", "password"));
		assertFalse(filter.checkPassword("plaintext", "plaintext"));
	}
}
