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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.TransformerException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class EbMSHttpClient implements EbMSClient
{
	@NonNull
	SSLFactoryManager sslFactoryManager;
	int connectTimeout;
	int readTimeout;
	boolean chunkedStreamingMode;
	EbMSProxy proxy;
	List<Integer> recoverableHttpErrors;
	List<Integer> unrecoverableHttpErrors;

	public EbMSDocument sendMessage(String uri, EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			@Cleanup("disconnect") val connection = createConnection(uri);
			new EbMSMessageWriter(connection).write(document);
			connection.connect();
			return new EbMSResponseHandler(connection,recoverableHttpErrors,unrecoverableHttpErrors).read();
		}
		catch (IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private HttpURLConnection createConnection(String uri) throws IOException
	{
		val result = (HttpURLConnection)openConnection(uri);
		result.setConnectTimeout(connectTimeout);
		result.setReadTimeout(readTimeout);
		if (chunkedStreamingMode)
			result.setChunkedStreamingMode(0);
		return result;
	}
	
	private URLConnection openConnection(String uri) throws IOException
	{
		val connection = openConnection(new URL(uri));
		connection.setDoOutput(true);
		//connection.setMethod("POST");
		if (connection instanceof HttpsURLConnection)
		{
			((HttpsURLConnection)connection).setHostnameVerifier(sslFactoryManager.getHostnameVerifier());
			((HttpsURLConnection)connection).setSSLSocketFactory(sslFactoryManager.getSslSocketFactory());
		}
		return connection;
	}

	private URLConnection openConnection(URL url) throws IOException
	{
		if (this.proxy != null)
		{
			val connectionProxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(this.proxy.getHost(),this.proxy.getPort()));
			if (this.proxy.useProxyAuthorization())
			{
				val connection = url.openConnection(connectionProxy);
				connection.setRequestProperty(this.proxy.getProxyAuthorizationKey(),this.proxy.getProxyAuthorizationValue());
				return connection;
			}
			else
				return url.openConnection(connectionProxy);
		}
		else
			return url.openConnection();
	}
}
