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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class SSLFactoryManager
{
	@NonNull
	EbMSKeyStore keyStore;
	@NonNull
	EbMSTrustStore trustStore;
	@NonNull
	String[] enabledProtocols;
	@NonNull
	String[] enabledCipherSuites;
	boolean requireClientAuthentication;
	@Getter
	SSLSocketFactory sslSocketFactory;

	@Builder
	public SSLFactoryManager(
			@NonNull EbMSKeyStore keyStore,
			@NonNull EbMSTrustStore trustStore,
			String[] enabledProtocols,
			String[] enabledCipherSuites,
			boolean requireClientAuthentication) throws Exception
	{
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.enabledProtocols = enabledProtocols == null ? new String[]{} : enabledProtocols;
		this.enabledCipherSuites = enabledCipherSuites == null ? new String[]{} : enabledCipherSuites;
		this.requireClientAuthentication = requireClientAuthentication;
		val kmf = createKeyManagerFactory(keyStore);
		val tmf = createTrustManagerFactory(trustStore);
		val sslContext = createSSLContext(kmf, tmf);
		createEngine(requireClientAuthentication, sslContext);
		sslSocketFactory = sslContext.getSocketFactory();
	}

	private KeyManagerFactory createKeyManagerFactory(EbMSKeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
	{
		// KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		val result = KeyManagerFactory.getInstance("SunX509");
		result.init(keyStore.getKeyStore(), keyStore.getKeyPassword().toCharArray());
		return result;
	}

	private TrustManagerFactory createTrustManagerFactory(EbMSTrustStore trustStore) throws NoSuchAlgorithmException, KeyStoreException
	{
		// TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		val result = TrustManagerFactory.getInstance("SunX509");
		result.init(trustStore.getKeyStore());
		return result;
	}

	private SSLContext createSSLContext(final KeyManagerFactory kmf, final TrustManagerFactory tmf) throws NoSuchAlgorithmException, KeyManagementException
	{
		val result = SSLContext.getInstance("TLS");
		result.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return result;
	}

	private SSLEngine createEngine(boolean requireClientAuthentication, final SSLContext sslContext)
	{
		// val result = sslContext.createSSLEngine(hostname,port);
		val result = sslContext.createSSLEngine();
		result.setUseClientMode(false);
		result.setSSLParameters(createSSLParameters());
		result.setNeedClientAuth(requireClientAuthentication);
		return result;
	}

	private SSLParameters createSSLParameters()
	{
		val result = new SSLParameters();
		if (enabledProtocols.length > 0)
			result.setProtocols(enabledProtocols);
		if (enabledProtocols.length > 0)
			result.setCipherSuites(enabledCipherSuites);
		result.setNeedClientAuth(requireClientAuthentication);
		return result;
	}
}
