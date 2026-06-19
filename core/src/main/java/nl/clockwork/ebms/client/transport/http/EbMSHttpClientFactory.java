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
package nl.clockwork.ebms.client.transport.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLParameters;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.client.EbMSClient;
import nl.clockwork.ebms.client.transport.ssl.SSLContextFactory;
import nl.clockwork.ebms.common.security.EbMSKeyStore;
import nl.clockwork.ebms.common.security.EbMSTrustStore;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSHttpClientFactory
{
	int connectTimeout;
	int readTimeout;
	int maxThreads;
	EbMSProxy proxy;
	String uuidHeader;
	@NonNull
	SSLParameters sslParameters;
	@NonNull
	EbMSKeyStore keyStore;
	EbMSTrustStore trustStore;
	HttpErrors httpErrors;
	@NonNull
	Map<String, EbMSClient> clients = new ConcurrentHashMap<>();

	@Builder
	public EbMSHttpClientFactory(
			int connectTimeout,
			int readTimeout,
			int maxThreads,
			EbMSProxy proxy,
			String uuidHeader,
			SSLParameters sslParameters,
			boolean verifyHostnames,
			@NonNull EbMSKeyStore keyStore,
			EbMSTrustStore trustStore,
			HttpErrors httpErrors)
	{
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.maxThreads = maxThreads;
		this.proxy = proxy;
		this.uuidHeader = uuidHeader;
		this.sslParameters = sslParameters;
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.httpErrors = httpErrors;
		System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.toString(!verifyHostnames));
	}

	public EbMSClient getEbMSClient(String clientAlias)
	{
		val key = clientAlias == null ? "" : clientAlias;
		return clients.computeIfAbsent(key, k -> createEbMSClient(clientAlias));
	}

	private EbMSClient createEbMSClient(String clientAlias)
	{
		try
		{
			val sslContextFactory = createSslContextFactory(clientAlias);
			return new EbMSHttpClient(
					sslParameters,
					sslContextFactory,
					connectTimeout,
					readTimeout,
					maxThreads,
					proxy,
					uuidHeader,
					httpErrors.getRecoverableHttpErrors(),
					httpErrors.getUnrecoverableHttpErrors());
		}
		catch (UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private SSLContextFactory createSslContextFactory(String clientAlias)
			throws UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		return SSLContextFactory.builder().keyStore(keyStore).trustStore(trustStore).clientAlias(clientAlias).build();
	}
}
