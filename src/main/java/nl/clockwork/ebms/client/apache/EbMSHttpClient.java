/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.client.apache;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

public class EbMSHttpClient implements EbMSClient
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private SSLSocketFactory sslSocketFactory;
	private boolean chunkedStreamingMode;
	
	public EbMSDocument sendMessage(String uri, EbMSDocument document) throws EbMSProcessorException
	{
    HttpClient httpClient = new DefaultHttpClient();
		setSSLSocketFactory(httpClient,uri);
		try
		{
			HttpPost httpPost = new HttpPost(uri);
			logger.info("Sending message to " + uri);
			if (logger.isDebugEnabled())
				logger.debug("OUT:\n" + DOMUtils.toString(document.getMessage()));
			EbMSMessageWriter ebMSMessageWriter = new EbMSMessageWriter(httpPost,chunkedStreaming(uri));
			ebMSMessageWriter.write(document);
			//try
			{
				EbMSDocument in = httpClient.execute(httpPost,new EbMSResponseHandler());
	      if (logger.isDebugEnabled())
					logger.debug("IN:\n" + (in == null || in.getMessage() == null ? "" : DOMUtils.toString(in.getMessage())));
				return in;
			}
			//catch (RuntimeException e)
			//{
			//	httpPost.abort();
			//	throw e;
			//}
		}
		catch (TransformerException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (IOException e)
		{
			throw new EbMSProcessorException(e);
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	private void setSSLSocketFactory(HttpClient httpClient, String uri)
	{
		if (uri.startsWith("https"))
		{
			String port = uri.replaceAll("^https://[-a-zA-Z0-9.]+(:(\\d+))?.*$","$2");
			if (StringUtils.isEmpty(port))
				port = "443";
			Scheme scheme = new Scheme("https",Integer.parseInt(port),sslSocketFactory);
			httpClient.getConnectionManager().getSchemeRegistry().register(scheme);
		}
	}

	public boolean chunkedStreaming(String uri)
	{
		return chunkedStreamingMode;
	}

	public void setSslSocketFactory(SSLSocketFactory sslSocketFactory)
	{
		this.sslSocketFactory = sslSocketFactory;
	}
	
	public void setChunkedStreamingMode(boolean chunkedStreamingMode)
	{
		this.chunkedStreamingMode = chunkedStreamingMode;
	}
}
