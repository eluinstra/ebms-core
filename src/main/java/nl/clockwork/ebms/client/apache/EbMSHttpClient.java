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
package nl.clockwork.ebms.client.apache;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSProxy;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.ssl.SSLFactoryManager;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

public class EbMSHttpClient implements EbMSClient
{
	private SSLConnectionSocketFactory sslConnectionSocketFactory;
	private boolean chunkedStreamingMode;
	private EbMSProxy proxy;

	public EbMSHttpClient()
	{
	}

	public EbMSHttpClient(SSLConnectionSocketFactory sslConnectionSocketFactory, boolean chunkedStreamingMode, EbMSProxy proxy) throws Exception
	{
		this.sslConnectionSocketFactory = sslConnectionSocketFactory;
		this.chunkedStreamingMode = chunkedStreamingMode;
		this.proxy = proxy;
	}

	public EbMSHttpClient(SSLFactoryManager sslFactoryManager, String[] enabledProtocols, String[] enabledCipherSuites, boolean verifyHostnames, boolean chunkedStreamingMode, EbMSProxy proxy) throws Exception
	{
		this.sslConnectionSocketFactory = new SSLConnectionSocketFactoryFactory(sslFactoryManager,enabledProtocols,enabledCipherSuites,verifyHostnames).getObject();
		this.chunkedStreamingMode = chunkedStreamingMode;
		this.proxy = proxy;
	}

	public EbMSDocument sendMessage(String uri, EbMSDocument document) throws EbMSProcessorException
	{
		try (CloseableHttpClient httpClient = getHttpClient(uri))
		{
			HttpPost httpPost = getHttpPost(uri);
			EbMSMessageWriter ebMSMessageWriter = new EbMSMessageWriter(httpPost,chunkedStreaming(uri));
			ebMSMessageWriter.write(document);
			return httpClient.execute(httpPost,new EbMSResponseHandler());
		}
		catch (TransformerException | IOException e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	private CloseableHttpClient getHttpClient(String uri)
	{
		HttpClientBuilder custom = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory);
		if (proxy != null && proxy.useProxy(uri) && proxy.useProxyAuthorization())
		{
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(proxy.getHost(),proxy.getPort()),new UsernamePasswordCredentials(proxy.getUsername(),proxy.getPassword()));
			custom.setDefaultCredentialsProvider(credsProvider);
		}
		return custom.build();
	}

	private HttpPost getHttpPost(String uri)
	{
		HttpPost result = new HttpPost(uri);
		if (proxy != null && proxy.useProxy(uri))
		{
			result.setConfig(RequestConfig.custom().setProxy(new HttpHost(proxy.getHost(),proxy.getPort())).build());
		}
		return result;
	}

	public boolean chunkedStreaming(String uri)
	{
		return chunkedStreamingMode;
	}

	public void setSslConnectionSocketFactory(SSLConnectionSocketFactory sslConnectionSocketFactory)
	{
		this.sslConnectionSocketFactory = sslConnectionSocketFactory;
	}
	
	public void setChunkedStreamingMode(boolean chunkedStreamingMode)
	{
		this.chunkedStreamingMode = chunkedStreamingMode;
	}
	
	public void setProxy(EbMSProxy proxy)
	{
		this.proxy = proxy;
	}
}
