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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.HTTPUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EbMSResponseHandler
{
	private HttpURLConnection connection;
	
	public EbMSResponseHandler(HttpURLConnection connection)
	{
		this.connection = connection;
	}

	public EbMSDocument read() throws IOException, ParserConfigurationException, SAXException, EbMSResponseException
	{
		InputStream input = null;
		try
		{
			if (connection.getResponseCode() / 100 == 2)
			{
				if (connection.getResponseCode() == 204 || connection.getContentLength() == 0)
					return null;
				else
				{
					input = connection.getInputStream();
					return getEbMSMessage(input);
				}
			}
			else if (connection.getResponseCode() >= 400)
			{
				input = connection.getErrorStream();
				throw new EbMSResponseException(connection.getResponseCode(),IOUtils.toString(input));
			}
			else
				throw new EbMSResponseException(connection.getResponseCode());
		}
		catch (IOException e)
		{
			try
			{
				InputStream errorStream = new BufferedInputStream(connection.getErrorStream());
				String error = IOUtils.toString(errorStream);
				errorStream.close();
				throw new EbMSResponseException(connection.getResponseCode(),error);
			}
			catch (IOException ignore)
			{
			}
			throw e;
		}
		finally
		{
			if (input != null)
				input.close();
		}
	}

	private EbMSDocument getEbMSMessage(InputStream in) throws IOException, ParserConfigurationException, SAXException
	{
		EbMSDocument result = null;
		String message = IOUtils.toString(in,getCharSet());
		if (StringUtils.isNotBlank(message))
		{
			DocumentBuilder db = DOMUtils.getDocumentBuilder();
			Document d = db.parse(new InputSource(new StringReader(message)));
			result = new EbMSDocument(d,new ArrayList<EbMSAttachment>());
		}
		return result;
	}
	
	private String getCharSet()
	{
		String contentType = getHeaderField(connection,"Content-Type");
		return HTTPUtils.getCharSet(contentType);
	}
	
	private String getHeaderField(HttpURLConnection connection, String name)
	{
		String result = connection.getHeaderField(name);
		if (result == null)
			for (String key : connection.getHeaderFields().keySet())
				if (key.equalsIgnoreCase(name))
				{
					result = connection.getHeaderField(key);
					break;
				}
		return result;
	}
}
