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
package nl.clockwork.ebms.job;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.client.EbMSHttpClientFactory.EbMSHttpClientType;
import nl.clockwork.ebms.client.EbMSProxy;
import nl.clockwork.ebms.client.SSLFactoryManager;

public class EbMSEventMICProcessor extends EbMSEventProcessor
{
	private EbMSHttpClientType type;
	private boolean chunkedStreamingMode;
	private boolean base64Writer;
	private EbMSProxy proxy;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean verifyHostnames;
	private String keyStorePath;
	private String keyStorePassword;
	private String trustStorePath;
	private String trustStorePassword;
	private Map<String,EbMSClient> clients = new ConcurrentHashMap<String,EbMSClient>();

	@Override
	protected EbMSClient getEbMSClient(String clientAlias)
	{
		if (!clients.containsKey(clientAlias))
			clients.put(clientAlias,createEbMSClient(clientAlias));
		return clients.get(clientAlias);
	}

	private EbMSClient createEbMSClient(String clientAlias)
	{
		try
		{
			EbMSHttpClientFactory ebMSHttpClientFactory = createEbMSHttpClientFactory(clientAlias);
			return ebMSHttpClientFactory.getObject();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private EbMSHttpClientFactory createEbMSHttpClientFactory(String clientAlias) throws Exception
	{
		EbMSHttpClientFactory result = new EbMSHttpClientFactory();
		result.setType(type.name());
		result.setSslFactoryManager(createSslFactoryManager(clientAlias));
		result.setChunkedStreamingMode(chunkedStreamingMode);
		result.setBase64Writer(base64Writer);
		result.setProxy(proxy);
		result.setEnabledProtocols(enabledProtocols);
		result.setEnabledCipherSuites(enabledCipherSuites);
		result.setVerifyHostnames(verifyHostnames);
		return result;
	}

	private SSLFactoryManager createSslFactoryManager(String clientAlias) throws Exception
	{
		SSLFactoryManager result = new SSLFactoryManager();
		result.setKeyStorePath(keyStorePath);
		result.setKeyStorePassword(keyStorePassword);
		result.setTrustStorePath(trustStorePath);
		result.setTrustStorePassword(trustStorePassword);
		result.setVerifyHostnames(verifyHostnames);
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

	public void setEnabledCipherSuites(String[] enabledCipherSuites)
	{
		this.enabledCipherSuites = enabledCipherSuites;
	}

	public void setVerifyHostnames(boolean verifyHostnames)
	{
		this.verifyHostnames = verifyHostnames;
	}

	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}

	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}

	public void setTrustStorePath(String trustStorePath)
	{
		this.trustStorePath = trustStorePath;
	}

	public void setTrustStorePassword(String trustStorePassword)
	{
		this.trustStorePassword = trustStorePassword;
	}
}
