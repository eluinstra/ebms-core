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

import nl.clockwork.ebms.ssl.SSLFactoryManager;

import org.springframework.beans.factory.FactoryBean;

public class EbMSHttpClientFactory implements FactoryBean<EbMSClient>
{
	public enum EbMSHttpClientType
	{
		DEFAULT, APACHE;
	}

	private EbMSHttpClientType type;
	private SSLFactoryManager sslFactoryManager;
	private boolean chunkedStreamingMode;
	private boolean base64Writer;
	private EbMSProxy proxy;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean verifyHostnames;

	@Override
	public EbMSClient getObject() throws Exception
	{
		if (EbMSHttpClientType.APACHE.equals(type))
			return new nl.clockwork.ebms.client.apache.EbMSHttpClient(sslFactoryManager,enabledProtocols,enabledCipherSuites,verifyHostnames,chunkedStreamingMode,proxy);
		else
			return new EbMSHttpClient(sslFactoryManager,chunkedStreamingMode,base64Writer,proxy);
	}

	@Override
	public Class<?> getObjectType()
	{
		return EbMSClient.class;
	}

	@Override
	public boolean isSingleton()
	{
		return false;
	}

	public void setType(String type)
	{
		try
		{
			this.type = EbMSHttpClientType.valueOf(type);
		}
		catch (IllegalArgumentException e)
		{
		}
	}

	public void setSslFactoryManager(SSLFactoryManager sslFactoryManager)
	{
		this.sslFactoryManager = sslFactoryManager;
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
}
