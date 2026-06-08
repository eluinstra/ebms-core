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
package nl.clockwork.ebms.client.delivery.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.val;
import org.junit.jupiter.api.Test;

class SSLParametersFactoryTest
{
	@Test
	void shouldSetOnlyCipherSuitesWhenProtocolsAreNotConfigured()
	{
		val cipherSuites = new String[]{"TLS_AES_256_GCM_SHA384"};
		val result = new SSLParametersFactory(null, cipherSuites).getObject();

		assertNull(result.getProtocols());
		assertArrayEquals(cipherSuites, result.getCipherSuites());
	}

	@Test
	void shouldSetConfiguredProtocolsAndCipherSuites()
	{
		val protocols = new String[]{"TLSv1.3", "TLSv1.2"};
		val cipherSuites = new String[]{"TLS_AES_128_GCM_SHA256"};
		val result = new SSLParametersFactory(protocols, cipherSuites).getObject();

		assertArrayEquals(protocols, result.getProtocols());
		assertArrayEquals(cipherSuites, result.getCipherSuites());
	}
}
