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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

import nl.clockwork.ebms.common.util.Utils;
import nl.clockwork.ebms.client.SSLFactoryManager;

public class EbMSHttpClientFactory implements FactoryBean<EbMSClient>
{
	public enum EbMSHttpClientType
	{
		DEFAULT, APACHE;
	}

	private transient Log logger = LogFactory.getLog(getClass());
	private EbMSHttpClientType type;
	private SSLFactoryManager sslFactoryManager;
	private boolean chunkedStreamingMode;
	private boolean base64Writer;
	private EbMSProxy proxy;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean verifyHostnames;
	private List<Integer> recoverableHttpErrors = new ArrayList<>();
	private List<Integer> irrecoverableHttpErrors = new ArrayList<>();

	@Override
	public EbMSClient getObject() throws Exception
	{
		logger.info("Using EbMSHttpClient " + type.name());
		if (EbMSHttpClientType.APACHE.equals(type))
			return new nl.clockwork.ebms.client.apache.EbMSHttpClient(sslFactoryManager,enabledProtocols,enabledCipherSuites,verifyHostnames,chunkedStreamingMode,proxy);
		else
			return new EbMSHttpClient(sslFactoryManager,chunkedStreamingMode,base64Writer,proxy,recoverableHttpErrors,irrecoverableHttpErrors);
	}

	@Override
	public Class<?> getObjectType()
	{
		return EbMSClient.class;
	}

	@Override
	public boolean isSingleton()
	{
		return true;
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
	public void setIrrecoverableServerHttpErrors(String irrecoverableServerHttpErrors)
	{
		this.irrecoverableHttpErrors = Utils.getIntegerList(irrecoverableServerHttpErrors);
	}
}
