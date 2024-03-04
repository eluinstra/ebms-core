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
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLParameters;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class EbMSHttpClient implements EbMSClient
{
	int readTimeout;
	EbMSProxy proxy;
	List<Integer> recoverableHttpErrors;
	List<Integer> unrecoverableHttpErrors;
	HttpClient httpClient;

	public EbMSHttpClient(
			@NonNull SSLParameters sslParameters,
			@NonNull SSLContextFactory sslFactoryManager,
			int connectTimeout,
			int readTimeout,
			int maxThreads,
			EbMSProxy proxy,
			List<Integer> recoverableHttpErrors,
			List<Integer> unrecoverableHttpErrors)
	{
		this.readTimeout = readTimeout;
		this.proxy = proxy;
		this.recoverableHttpErrors = recoverableHttpErrors;
		this.unrecoverableHttpErrors = unrecoverableHttpErrors;
		this.httpClient = HttpClient.newBuilder()
				.executor(Executors.newFixedThreadPool(maxThreads))
				.connectTimeout(Duration.ofMillis(connectTimeout))
				.sslContext(sslFactoryManager.getSslContext())
				.sslParameters(sslParameters)
				.proxy(getProxy(proxy))
				.build();
	}

	private ProxySelector getProxy(EbMSProxy proxy)
	{
		return proxy != null && proxy.useProxy() ? ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())) : ProxySelector.getDefault();
	}

	public EbMSDocument sendMessage(String uri, EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			val response = httpClient.send(createRequest(readTimeout, proxy, uri, document), BodyHandlers.ofString());
			return new EbMSResponseHandler(response, recoverableHttpErrors, unrecoverableHttpErrors).read();
		}
		catch (IOException | TransformerException | InterruptedException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private static HttpRequest createRequest(int readTimeout, EbMSProxy proxy, String uri, EbMSDocument document) throws TransformerException
	{
		var request = HttpRequest.newBuilder().uri(URI.create(uri)).timeout(Duration.ofMillis(readTimeout));
		request = new EbMSMessageWriter().write(request, document);
		if (proxy != null && proxy.useProxyAuthorization())
			request = request.setHeader(proxy.getProxyAuthorizationKey(), proxy.getProxyAuthorizationValue());
		return request.build();
	}
}
