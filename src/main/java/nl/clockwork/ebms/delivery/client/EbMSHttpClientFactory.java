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


import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLParameters;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.cpa.certificate.CertificateMapper;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;
import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSHttpClientFactory
{
	int connectTimeout;
	int readTimeout;
	EbMSProxy proxy;
	@NonNull
	SSLParameters sslParameters;
	@NonNull
	EbMSKeyStore keyStore;
	EbMSTrustStore trustStore;
	HttpErrors httpErrors;
	@NonNull
	CertificateMapper certificateMapper;
	boolean useClientCertificate;
	@NonNull
	Map<String, EbMSClient> clients = new ConcurrentHashMap<>();

	@Builder
	public EbMSHttpClientFactory(
			int connectTimeout,
			int readTimeout,
			EbMSProxy proxy,
			SSLParameters sslParameters,
			boolean verifyHostnames,
			@NonNull EbMSKeyStore keyStore,
			EbMSTrustStore trustStore,
			HttpErrors httpErrors,
			@NonNull CertificateMapper certificateMapper,
			boolean useClientCertificate)
	{
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.proxy = proxy;
		this.sslParameters = sslParameters;
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.httpErrors = httpErrors;
		this.certificateMapper = certificateMapper;
		this.useClientCertificate = useClientCertificate;
		System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.toString(verifyHostnames));
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
			val sslContextFactory = createSslContextFactory(getClientAlias(clientAlias));
			return new EbMSHttpClient(
					sslParameters,
					sslContextFactory,
					connectTimeout,
					readTimeout,
					proxy,
					httpErrors.getRecoverableHttpErrors(),
					httpErrors.getUnrecoverableHttpErrors());
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	private String getClientAlias(String clientAlias)
	{
		return clientAlias == null && StringUtils.isNotEmpty(keyStore.getDefaultAlias()) ? keyStore.getDefaultAlias() : clientAlias;
	}

	private SSLContextFactory createSslContextFactory(String clientAlias)
			throws UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		return SSLContextFactory.builder().keyStore(keyStore).trustStore(trustStore).clientAlias(clientAlias).build();
	}

	public EbMSClient getEbMSClient(String cpaId, DeliveryChannel sendDeliveryChannel)
	{
		try
		{
			val clientCertificate = getClientCertificate(cpaId, sendDeliveryChannel);
			val clientAlias = clientCertificate != null ? keyStore.getCertificateAlias(clientCertificate) : null;
			return getEbMSClient(clientAlias);
		}
		catch (KeyStoreException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private X509Certificate getClientCertificate(String cpaId, DeliveryChannel deliveryChannel)
	{
		return useClientCertificate && deliveryChannel != null
				? certificateMapper.getCertificate(CPAUtils.getX509Certificate(CPAUtils.getClientCertificate(deliveryChannel)), cpaId)
				: null;
	}
}
