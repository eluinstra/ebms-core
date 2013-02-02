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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageWriter;
import nl.clockwork.ebms.processor.EbMSMessageWriterImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSHttpClient implements EbMSClient
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private SSLFactoryManager sslFactoryManager;

	public void sendMessage(String uri, EbMSDocument document) throws Exception
	{
		URLConnection connection = openConnection(uri);
		EbMSMessageWriter writer = new EbMSMessageWriterImpl((HttpURLConnection)connection);
		writer.write(document);
		writer.flush();
		handleResponse(connection);
	}
	
	private URLConnection openConnection(String uri) throws IOException
	{
		URL url = new URL(uri);
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		//connection.setMethod("POST");
		if (connection instanceof HttpsURLConnection)
			((HttpsURLConnection)connection).setSSLSocketFactory(sslFactoryManager.getSslFactory());
		return connection;
	}

	private void handleResponse(URLConnection connection) throws IOException
	{
		//TODO: handle response
		if (connection instanceof HttpURLConnection)
			logger.info("StatusCode: " + ((HttpURLConnection)connection).getResponseCode());
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String s;
		while ((s = in.readLine()) != null)
			logger.info(s);
		in.close();
	}

	public void setSslFactoryManager(SSLFactoryManager sslFactoryManager)
	{
		this.sslFactoryManager = sslFactoryManager;
	}
}
