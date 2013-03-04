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
package nl.clockwork.ebms.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSResponseDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSHttpClient implements EbMSClient
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private SSLFactoryManager sslFactoryManager;
	private boolean chunkedStreamingMode = true;

	public EbMSDocument sendMessage(String uri, EbMSDocument document) throws EbMSProcessorException
	{
		try
		{
			HttpURLConnection connection = (HttpURLConnection)openConnection(uri);
			if (logger.isDebugEnabled())
				logger.debug("OUT:\n" + DOMUtils.toString(document.getMessage()));
			if (chunkedStreamingMode)
				connection.setChunkedStreamingMode(0);
			EbMSMessageWriter writer = new EbMSMessageWriterImpl(connection);
			writer.write(document);
			writer.flush();
			EbMSResponseDocument in = handleResponse(connection);
			if (logger.isDebugEnabled())
			{
				logger.debug("StatusCode: " + in.getStatusCode());
				logger.debug("IN:\n" + (in.getMessage() == null ? "" : DOMUtils.toString(in.getMessage())));
			}
			return in;
		}
		catch (ConnectException e)
		{
			throw new EbMSProcessorException("Error connecting to: " + uri,e);
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	@SuppressWarnings({"restriction","deprecation"})
	private URLConnection openConnection(String uri) throws IOException
	{
		URL url = new URL(uri);
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		//connection.setMethod("POST");
		if (connection instanceof HttpsURLConnection)
			((HttpsURLConnection)connection).setSSLSocketFactory(sslFactoryManager.getSslFactory());
		else if (connection instanceof com.sun.net.ssl.HttpsURLConnection)
			((com.sun.net.ssl.HttpsURLConnection)connection).setSSLSocketFactory(sslFactoryManager.getSslFactory());
		return connection;
	}

	private EbMSResponseDocument handleResponse(HttpURLConnection connection) throws IOException, EbMSProcessorException, TransformerException, ParserConfigurationException, SAXException
	{
		EbMSDocument document = getEbMSMessage(IOUtils.toString(connection.getInputStream()));
		EbMSResponseDocument result = new EbMSResponseDocument(document,connection.getResponseCode());
		return result;
	}

	private EbMSDocument getEbMSMessage(String message) throws ParserConfigurationException, SAXException, IOException
	{
		EbMSDocument result = null;
		if (StringUtils.isNotBlank(message))
		{
			DocumentBuilder db = DOMUtils.getDocumentBuilder();
			Document d = db.parse(message);
			result = new EbMSDocument(d,new ArrayList<EbMSAttachment>());
		}
		return result;
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
