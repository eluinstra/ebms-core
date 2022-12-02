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
package nl.clockwork.ebms.delivery.client;

import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SSLContextFactory
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	public class EbMSX509KeyManager implements X509KeyManager
	{
		@NonNull
		X509KeyManager standardKeyManager;
		String clientAlias;

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
		{
			return standardKeyManager.chooseServerAlias(keyType,issuers,socket);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
		{
			return clientAlias == null ? standardKeyManager.chooseClientAlias(keyType,issuers,socket) : clientAlias;
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers)
		{
			return standardKeyManager.getServerAliases(keyType,issuers);
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers)
		{
			return standardKeyManager.getClientAliases(keyType,issuers);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias)
		{
			return standardKeyManager.getCertificateChain(alias);
		}

		@Override
		public PrivateKey getPrivateKey(String alias)
		{
			return standardKeyManager.getPrivateKey(alias);
		}
	}

	@NonNull
	EbMSKeyStore keyStore;
	@NonNull
	EbMSTrustStore trustStore;
	@Getter
	SSLContext sslContext;

	@Builder
	public SSLContextFactory(
			@NonNull EbMSKeyStore keyStore,
			@NonNull EbMSTrustStore trustStore,
			String clientAlias) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		val kmf = createKeyManagerFactory(keyStore);
		setClientAlias(kmf,clientAlias);
		val tmf = createTrustManagerFactory(trustStore);
		sslContext = createSSLContext(kmf,tmf);
		createEngine(sslContext);
	}

	private KeyManagerFactory createKeyManagerFactory(EbMSKeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
	{
		val result = KeyManagerFactory.getInstance("SunX509");
		result.init(keyStore.getKeyStore(),keyStore.getKeyPassword().toCharArray());
		return result;
	}

	private KeyManagerFactory setClientAlias(final KeyManagerFactory kmf, String clientAlias)
	{
		val keyManagers = kmf.getKeyManagers();
		for (var i = 0; i < keyManagers.length; i++)
			if (keyManagers[i] instanceof X509KeyManager)
				keyManagers[i] = new EbMSX509KeyManager((X509KeyManager)keyManagers[i],clientAlias);
		return kmf;
	}

	private TrustManagerFactory createTrustManagerFactory(EbMSTrustStore trustStore) throws NoSuchAlgorithmException, KeyStoreException
	{
		val result = TrustManagerFactory.getInstance("SunX509");
		result.init(trustStore.getKeyStore());
		return result;
	}

	private SSLContext createSSLContext(final KeyManagerFactory kmf, final TrustManagerFactory tmf) throws NoSuchAlgorithmException, KeyManagementException
	{
		val result = SSLContext.getInstance("TLS");
		result.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);
		return result;
	}

	private SSLEngine createEngine(final javax.net.ssl.SSLContext sslContext)
	{
		val result = sslContext.createSSLEngine();
		result.setUseClientMode(true);
		//result.setSSLParameters(createSSLParameters());
		return result;
	}
}
