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
package nl.clockwork.ebms.client;

import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.cpa.CertificateMapper;
import nl.clockwork.ebms.metrics.MetricsService;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSHttpClientFactory
{
	public enum EbMSHttpClientType
	{
		DEFAULT, APACHE;
	}

	@NonNull
	@Default
	EbMSHttpClientType type = EbMSHttpClientType.DEFAULT;
	int connectTimeout;
	boolean chunkedStreamingMode;
	boolean base64Writer;
	EbMSProxy proxy;
	String[] enabledProtocols;
	String[] enabledCipherSuites;
	boolean verifyHostnames;
	@NonNull
	EbMSKeyStore keyStore;
	EbMSTrustStore trustStore;
	HttpErrors httpErrors;
	@NonNull
	CertificateMapper certificateMapper;
	boolean useClientCertificate;
	@NonNull
	MetricsService metricsService;
	@NonNull
	@Default
	Map<String,EbMSClient> clients = new ConcurrentHashMap<String,EbMSClient>();

	private EbMSClient createEbMSClient(String clientAlias)
	{
		try
		{
			val sslFactoryManager = createSslFactoryManager(getClientAlias(clientAlias));
			val client = EbMSHttpClientType.APACHE.equals(type)
				? new nl.clockwork.ebms.client.apache.EbMSHttpClient(sslFactoryManager,enabledProtocols,enabledCipherSuites,verifyHostnames,connectTimeout,chunkedStreamingMode,proxy)
				: new EbMSHttpClient(sslFactoryManager,connectTimeout,chunkedStreamingMode,base64Writer,proxy,httpErrors.getRecoverableHttpErrors(),httpErrors.getUnrecoverableHttpErrors());
			return new EbMSClientMetricsWrapper(metricsService,client);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public EbMSClient getEbMSClient(String clientAlias)
	{
		val key = clientAlias == null ? "" : clientAlias;
		if (!clients.containsKey(key))
			clients.put(key,createEbMSClient(clientAlias));
		return clients.get(key);
	}

	public EbMSClient getEbMSClient(String cpaId, DeliveryChannel sendDeliveryChannel)
	{
		try
		{
			val clientCertificate = getClientCertificate(cpaId,sendDeliveryChannel);
			val clientAlias = clientCertificate != null ? keyStore.getCertificateAlias(clientCertificate) : null;
			return getEbMSClient(clientAlias);
		}
		catch (CertificateException | KeyStoreException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getClientAlias(String clientAlias)
	{
		return clientAlias == null && StringUtils.isNotEmpty(keyStore.getDefaultAlias()) ? keyStore.getDefaultAlias() : clientAlias;
	}

	private X509Certificate getClientCertificate(String cpaId, DeliveryChannel deliveryChannel) throws CertificateException
	{
		return useClientCertificate && deliveryChannel != null ? certificateMapper.getCertificate(CPAUtils.getX509Certificate(CPAUtils.getClientCertificate(deliveryChannel)),cpaId) : null;
	}

	private SSLFactoryManager createSslFactoryManager(String clientAlias) throws Exception
	{
		return SSLFactoryManager.builder()
				.keyStore(keyStore)
				.trustStore(trustStore)
				.verifyHostnames(verifyHostnames)
				.enabledProtocols(enabledProtocols)
				.enabledCipherSuites(enabledCipherSuites)
				.clientAlias(clientAlias)
				.build();
	}
}
