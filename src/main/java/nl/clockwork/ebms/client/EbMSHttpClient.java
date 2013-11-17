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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

public class EbMSHttpClient implements EbMSClient
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private SSLFactoryManager sslFactoryManager;
	private boolean chunkedStreamingMode;
	//private boolean verifyHostnames;

	public EbMSDocument sendMessage(String uri, EbMSDocument document) throws EbMSProcessorException
	{
		HttpURLConnection connection = null;
		try
		{
			connection = (HttpURLConnection)openConnection(uri);
			//connection.setConnectTimeout(connectTimeout);
			if (chunkedStreaming(uri))
				connection.setChunkedStreamingMode(0);
			if (logger.isDebugEnabled())
				logger.debug("OUT:\n" + DOMUtils.toString(document.getMessage()));
			EbMSMessageWriter writer = new EbMSMessageWriter(connection);
			writer.write(document);
			connection.connect();
			EbMSResponseHandler reader = new EbMSResponseHandler(connection);
			EbMSDocument in = reader.read();
			if (logger.isDebugEnabled())
				logger.debug("IN:\n" + (in == null || in.getMessage() == null ? "" : DOMUtils.toString(in.getMessage())));
			return in;
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (SAXException e)
		{
			throw new EbMSProcessingException(e);
		}
		finally
		{
			if (connection != null)
				connection.disconnect();
		}
	}
	
	public boolean chunkedStreaming(String uri)
	{
		return chunkedStreamingMode;
	}

	@SuppressWarnings({"restriction","deprecation"})
	private URLConnection openConnection(String uri) throws IOException
	{
		URL url = new URL(uri);
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		//connection.setMethod("POST");
		if (connection instanceof HttpsURLConnection)
		{
			//if (!verifyHostnames)
			//	((HttpsURLConnection)connection).setHostnameVerifier(
			//		new HostnameVerifier()
			//		{
			//			@Override
			//			public boolean verify(String hostname, SSLSession sslSession)
			//			{
			//				return true;
			//			}
			//		}
			//	);
			((HttpsURLConnection)connection).setSSLSocketFactory(sslFactoryManager.getSslSocketFactory());
		}
		else if (connection instanceof com.sun.net.ssl.HttpsURLConnection)
		{
			//if (!verifyHostnames)
			//	((com.sun.net.ssl.HttpsURLConnection)connection).setHostnameVerifier(
			//		new com.sun.net.ssl.HostnameVerifier()
			//		{
			//			@Override
			//			public boolean verify(String urlHostname, String certHostname)
			//			{
			//				return true;
			//			}
			//		}
			//	);
			((com.sun.net.ssl.HttpsURLConnection)connection).setSSLSocketFactory(sslFactoryManager.getSslSocketFactory());
		}
		return connection;
	}

	public void setSslFactoryManager(SSLFactoryManager sslFactoryManager)
	{
		this.sslFactoryManager = sslFactoryManager;
	}
	
	public void setChunkedStreamingMode(boolean chunkedStreamingMode)
	{
		this.chunkedStreamingMode = chunkedStreamingMode;
	}
}
