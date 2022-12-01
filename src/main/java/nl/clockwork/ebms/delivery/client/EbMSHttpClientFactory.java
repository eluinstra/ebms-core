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

import java.security.KeyStoreException;
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
import nl.clockwork.ebms.cpa.certificate.CertificateMapper;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSHttpClientFactory
{
	int connectTimeout;
	int readTimeout;
	boolean chunkedStreamingMode;
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
	@Default
	Map<String,EbMSClient> clients = new ConcurrentHashMap<>();

	private EbMSClient createEbMSClient(String clientAlias)
	{
		try
		{
			val sslFactoryManager = createSslFactoryManager(getClientAlias(clientAlias));
			return new EbMSHttpClient(sslFactoryManager,connectTimeout,readTimeout,chunkedStreamingMode,proxy,httpErrors.getRecoverableHttpErrors(),httpErrors.getUnrecoverableHttpErrors());
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	public EbMSClient getEbMSClient(String clientAlias)
	{
		val key = clientAlias == null ? "" : clientAlias;
		clients.computeIfAbsent(key, k -> createEbMSClient(clientAlias));
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
		catch (KeyStoreException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private String getClientAlias(String clientAlias)
	{
		return clientAlias == null && StringUtils.isNotEmpty(keyStore.getDefaultAlias())
				? keyStore.getDefaultAlias()
				: clientAlias;
	}

	private X509Certificate getClientCertificate(String cpaId, DeliveryChannel deliveryChannel)
	{
		return useClientCertificate && deliveryChannel != null
				? certificateMapper.getCertificate(CPAUtils.getX509Certificate(CPAUtils.getClientCertificate(deliveryChannel)),cpaId)
				: null;
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
