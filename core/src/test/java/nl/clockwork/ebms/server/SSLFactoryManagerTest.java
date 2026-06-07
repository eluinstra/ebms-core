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
package nl.clockwork.ebms.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import javax.net.ssl.SSLParameters;
import lombok.val;
import nl.clockwork.ebms.common.security.EbMSKeyStore;
import nl.clockwork.ebms.common.security.EbMSTrustStore;
import nl.clockwork.ebms.common.security.KeyStoreType;
import org.junit.jupiter.api.Test;

class SSLFactoryManagerTest
{
	private static final KeyStoreType KEY_STORE_TYPE = KeyStoreType.PKCS12;
	private static final String KEY_STORE_PATH = "nl/clockwork/ebms/keystore.p12";
	private static final String KEY_STORE_PASSWORD = "password";

	@Test
	void shouldSetOnlyCipherSuitesWhenProtocolsAreNotConfigured() throws Exception
	{
		val manager = SSLFactoryManager.builder()
				.keyStore(EbMSKeyStore.of(KEY_STORE_TYPE, KEY_STORE_PATH, KEY_STORE_PASSWORD, KEY_STORE_PASSWORD))
				.trustStore(EbMSTrustStore.of(KEY_STORE_TYPE, KEY_STORE_PATH, KEY_STORE_PASSWORD))
				.enabledProtocols(new String[]{})
				.enabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"})
				.requireClientAuthentication(false)
				.build();

		val parameters = invokeCreateSslParameters(manager);
		assertNull(parameters.getProtocols());
		assertArrayEquals(new String[]{"TLS_AES_128_GCM_SHA256"}, parameters.getCipherSuites());
	}

	@Test
	void shouldSetConfiguredProtocolsAndCipherSuites() throws Exception
	{
		val manager = SSLFactoryManager.builder()
				.keyStore(EbMSKeyStore.of(KEY_STORE_TYPE, KEY_STORE_PATH, KEY_STORE_PASSWORD, KEY_STORE_PASSWORD))
				.trustStore(EbMSTrustStore.of(KEY_STORE_TYPE, KEY_STORE_PATH, KEY_STORE_PASSWORD))
				.enabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"})
				.enabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"})
				.requireClientAuthentication(false)
				.build();

		val parameters = invokeCreateSslParameters(manager);
		assertArrayEquals(new String[]{"TLSv1.3", "TLSv1.2"}, parameters.getProtocols());
		assertArrayEquals(new String[]{"TLS_AES_128_GCM_SHA256"}, parameters.getCipherSuites());
	}

	private SSLParameters invokeCreateSslParameters(SSLFactoryManager manager) throws Exception
	{
		Method method = SSLFactoryManager.class.getDeclaredMethod("createSSLParameters");
		method.setAccessible(true);
		return (SSLParameters)method.invoke(manager);
	}
}
