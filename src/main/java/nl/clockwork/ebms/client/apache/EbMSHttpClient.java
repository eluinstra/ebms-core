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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSProxy;
import nl.clockwork.ebms.client.SSLFactoryManager;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSHttpClient implements EbMSClient
{
	public static final String WIRE_LOG = "org.apache.http.wire";
	@NonNull
	SSLConnectionSocketFactory sslConnectionSocketFactory;
	int connectTimeout;
	int socketTimeout;
	boolean chunkedStreamingMode;
	@NonNull
	EbMSProxy proxy;

	public EbMSHttpClient(SSLFactoryManager sslFactoryManager, String[] enabledProtocols, String[] enabledCipherSuites, boolean verifyHostnames, int connectTimeout, int socketTimeout, boolean chunkedStreamingMode, EbMSProxy proxy) throws Exception
	{
		this(new SSLConnectionSocketFactoryFactory(sslFactoryManager,enabledProtocols,enabledCipherSuites,verifyHostnames).getObject(),connectTimeout,socketTimeout,chunkedStreamingMode,proxy);
	}

	public EbMSDocument sendMessage(String uri, EbMSDocument document) throws EbMSProcessorException
	{
		try (val httpClient = getHttpClient(uri))
		{
			val httpPost = getHttpPost(uri);
			val ebMSMessageWriter = new EbMSMessageWriter(httpPost,chunkedStreamingMode);
			ebMSMessageWriter.write(document);
			return httpClient.execute(httpPost,new EbMSResponseHandler());
		}
		catch (TransformerException | IOException e)
		{
			throw new EbMSProcessingException(e);
		}
	}
	
	private CloseableHttpClient getHttpClient(String uri)
	{
		val custom = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory);
		if (proxy != null && proxy.useProxyAuthorization())
		{
			val credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(proxy.getHost(),proxy.getPort()),new UsernamePasswordCredentials(proxy.getUsername(),proxy.getPassword()));
			custom.setDefaultCredentialsProvider(credsProvider);
		}
		return custom.build();
	}

	private HttpPost getHttpPost(String uri)
	{
		val result = new HttpPost(uri);
		if (proxy != null)
		{
			result.setConfig(RequestConfig.custom().setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).setProxy(new HttpHost(proxy.getHost(),proxy.getPort())).build());
		}
		return result;
	}
}
