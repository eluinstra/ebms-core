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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import nl.clockwork.ebms.common.CertificateManager;
import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.KeyStoreManager.KeyStoreType;
import nl.clockwork.ebms.common.util.Utils;
import nl.clockwork.ebms.util.CPAUtils;

public class EbMSHttpClientFactory
{
	public enum EbMSHttpClientType
	{
		DEFAULT, APACHE;
	}

	private EbMSHttpClientType type;
	private int connectTimeout;
	private boolean chunkedStreamingMode;
	private boolean base64Writer;
	private EbMSProxy proxy;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean verifyHostnames;
	private KeyStoreType keyStoreType;
	private String keyStorePath;
	private String keyStorePassword;
	private KeyStoreType trustStoreType;
	private String trustStorePath;
	private String trustStorePassword;
	private List<Integer> recoverableHttpErrors = new ArrayList<>();
	private List<Integer> unrecoverableHttpErrors = new ArrayList<>();
	private String defaultClientCertificateAlias;
	private CertificateManager certificateManager;
	private boolean useClientCertificate;
	private Map<String,EbMSClient> clients = new ConcurrentHashMap<String,EbMSClient>();
	private KeyStore clientKeyStore;

	public EbMSClient createEbMSClient(String clientAlias)
	{
		try
		{
			SSLFactoryManager sslFactoryManager = createSslFactoryManager(getClientAlias(clientAlias));
			if (EbMSHttpClientType.APACHE.equals(type))
				return new nl.clockwork.ebms.client.apache.EbMSHttpClient(sslFactoryManager,enabledProtocols,enabledCipherSuites,verifyHostnames,connectTimeout,chunkedStreamingMode,proxy);
			else
				return new EbMSHttpClient(sslFactoryManager,connectTimeout,chunkedStreamingMode,base64Writer,proxy,recoverableHttpErrors,unrecoverableHttpErrors);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public EbMSClient getEbMSClient(String clientAlias)
	{
		String key = clientAlias == null ? "" : clientAlias;
		if (!clients.containsKey(key))
			clients.put(key,createEbMSClient(clientAlias));
		return clients.get(key);
	}

	public EbMSClient getEbMSClient(DeliveryChannel sendDeliveryChannel)
	{
		try
		{
			X509Certificate clientCertificate = getClientCertificate(sendDeliveryChannel);
			String clientAlias = clientCertificate != null ? getClientKeyStore().getCertificateAlias(clientCertificate) : null;
			return getEbMSClient(clientAlias);
		}
		catch (GeneralSecurityException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getClientAlias(String clientAlias)
	{
		return clientAlias == null && StringUtils.isNotEmpty(defaultClientCertificateAlias) ? defaultClientCertificateAlias : clientAlias;
	}

	private X509Certificate getClientCertificate(DeliveryChannel deliveryChannel) throws CertificateException
	{
		return useClientCertificate && deliveryChannel != null ? certificateManager.getCertificate(CPAUtils.getX509Certificate(CPAUtils.getClientCertificate(deliveryChannel))) : null;
	}

	private KeyStore getClientKeyStore() throws GeneralSecurityException, IOException
	{
		if (clientKeyStore == null)
			clientKeyStore = KeyStoreManager.getKeyStore(null,keyStorePath,keyStorePassword);
		return clientKeyStore;
	}

	private SSLFactoryManager createSslFactoryManager(String clientAlias) throws Exception
	{
		SSLFactoryManager result = new SSLFactoryManager();
		result.setKeyStoreType(keyStoreType);
		result.setKeyStorePath(keyStorePath);
		result.setKeyStorePassword(keyStorePassword);
		result.setTrustStoreType(trustStoreType);
		result.setTrustStorePath(trustStorePath);
		result.setTrustStorePassword(trustStorePassword);
		result.setVerifyHostnames(verifyHostnames);
		result.setEnabledProtocols(enabledProtocols);
		result.setEnabledCipherSuites(enabledCipherSuites);
		result.setClientAlias(clientAlias);
		result.afterPropertiesSet();
		return result;
	}

	public void setType(String type)
	{
		try
		{
			this.type = EbMSHttpClientType.valueOf(type);
		}
		catch (IllegalArgumentException e)
		{
			this.type = EbMSHttpClientType.DEFAULT;
		}
	}
	public void setConnectTimeout(int connectTimeout)
	{
		this.connectTimeout = connectTimeout;
	}
	public void setChunkedStreamingMode(boolean chunkedStreamingMode)
	{
		this.chunkedStreamingMode = chunkedStreamingMode;
	}
	public void setBase64Writer(boolean base64Writer)
	{
		this.base64Writer = base64Writer;
	}
	public void setProxy(EbMSProxy proxy)
	{
		this.proxy = proxy;
	}
	public void setEnabledProtocols(String[] enabledProtocols)
	{
		this.enabledProtocols = enabledProtocols;
	}
	public void setEnabledProtocols(String enabledProtocols)
	{
		this.enabledProtocols = StringUtils.split(enabledProtocols,",");
	}
	public void setEnabledCipherSuites(String[] enabledCipherSuites)
	{
		this.enabledCipherSuites = enabledCipherSuites;
	}
	public void setEnabledCipherSuites(String enabledCipherSuites)
	{
		this.enabledCipherSuites = StringUtils.split(enabledCipherSuites,",");
	}
	public void setVerifyHostnames(boolean verifyHostnames)
	{
		this.verifyHostnames = verifyHostnames;
	}
	public void setKeyStoreType(KeyStoreType keyStoreType)
	{
		this.keyStoreType = keyStoreType;
	}
	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}
	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}
	public void setTrustStoreType(KeyStoreType trustStoreType)
	{
		this.trustStoreType = trustStoreType;
	}
	public void setTrustStorePath(String trustStorePath)
	{
		this.trustStorePath = trustStorePath;
	}
	public void setTrustStorePassword(String trustStorePassword)
	{
		this.trustStorePassword = trustStorePassword;
	}
	public void setRecoverableInformationalHttpErrors(String recoverableInformationalHttpErrors)
	{
		this.recoverableHttpErrors.addAll(Utils.getIntegerList(recoverableInformationalHttpErrors));
	}
	public void setRecoverableRedirectionHttpErrors(String recoverableRedirectionHttpErrors)
	{
		this.recoverableHttpErrors.addAll(Utils.getIntegerList(recoverableRedirectionHttpErrors));
	}
	public void setRecoverableClientHttpErrors(String recoverableClientHttpErrors)
	{
		this.recoverableHttpErrors.addAll(Utils.getIntegerList(recoverableClientHttpErrors));
	}
	public void setUnrecoverableServerHttpErrors(String unrecoverableServerHttpErrors)
	{
		this.unrecoverableHttpErrors = Utils.getIntegerList(unrecoverableServerHttpErrors);
	}
	public void setDefaultClientCertificateAlias(String defaultClientCertificateAlias)
	{
		this.defaultClientCertificateAlias = defaultClientCertificateAlias;
	}
	public void setCertificateManager(CertificateManager certificateManager)
	{
		this.certificateManager = certificateManager;
	}
	public void setUseClientCertificate(boolean useClientCertificate)
	{
		this.useClientCertificate = useClientCertificate;
	}
}
