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
package nl.clockwork.ebms.client.delivery.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.Test;

class SSLParametersFactoryTest
{
	private static final String[] PROTOCOLS = {"TLSv1.2", "TLSv1.3"};
	private static final String[] CIPHER_SUITES = {
		"TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256", "TLS_CHACHA20_POLY1305_SHA256",
		"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
	};

	@Test
	void protocolsAndCipherSuites()
	{
		SSLParameters result = new SSLParametersFactory(PROTOCOLS, CIPHER_SUITES).getObject();
		assertArrayEquals(PROTOCOLS, result.getProtocols());
		assertArrayEquals(CIPHER_SUITES, result.getCipherSuites());
	}

	@Test
	void cipherSuitesWithoutProtocols()
	{
		SSLParameters result = new SSLParametersFactory(new String[]{}, CIPHER_SUITES).getObject();
		assertNull(result.getProtocols());
		assertArrayEquals(CIPHER_SUITES, result.getCipherSuites());
	}

	@Test
	void protocolsWithoutCipherSuites()
	{
		SSLParameters result = new SSLParametersFactory(PROTOCOLS, new String[]{}).getObject();
		assertArrayEquals(PROTOCOLS, result.getProtocols());
		assertNull(result.getCipherSuites());
	}

	@Test
	void noProtocolsNoCipherSuites()
	{
		SSLParameters result = new SSLParametersFactory(null, null).getObject();
		assertNull(result.getProtocols());
		assertNull(result.getCipherSuites());
	}

	@Test
	void objectIsJdkSSLParameters()
	{
		assertSame(SSLParameters.class, new SSLParametersFactory(PROTOCOLS, CIPHER_SUITES).getObjectType());
	}
}
