/**
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
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
		//KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		val kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore.getKeyStore(),keyStore.getKeyPassword().toCharArray());

		//TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		val tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(trustStore.getKeyStore());

		val sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);

		//val engine = sslContext.createSSLEngine(hostname,port);
		val engine = sslContext.createSSLEngine();
		engine.setUseClientMode(false);
		engine.setSSLParameters(createSSLParameters());
		engine.setNeedClientAuth(requireClientAuthentication);

		sslSocketFactory = sslContext.getSocketFactory();
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
