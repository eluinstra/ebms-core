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
import java.net.HttpURLConnection;

import javax.xml.parsers.ParserConfigurationException;

import nl.clockwork.ebms.common.util.HTTPUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSMessageReader;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.soap.envelope.Fault;

public class EbMSResponseHandler
{
	private HttpURLConnection connection;
	
	public EbMSResponseHandler(HttpURLConnection connection)
	{
		this.connection = connection;
	}

	public EbMSDocument read() throws IOException, ParserConfigurationException, SAXException, EbMSProcessorException
	{
		try
		{
			if (connection.getResponseCode() / 100 == 2)
			{
				if (connection.getResponseCode() == 204 || connection.getContentLength() == 0)
					return null;
				else
				{
					try (InputStream input = connection.getInputStream())
					{
						EbMSMessageReader messageReader = new EbMSMessageReader(getHeaderField("Content-ID"),getHeaderField("Content-Type"));
						//return messageReader.read(input);
						return messageReader.readResponse(input,getEncoding());
					}
				}
			}
			else if (connection.getResponseCode() >= 400)
			{
				try (InputStream input = connection.getErrorStream())
				{
					if (input != null)
					{
						String response = IOUtils.toString(input);
						if (connection.getResponseCode() == 500)
						{
							Fault soapFault = EbMSMessageUtils.getSOAPFault(response);
							if (soapFault != null)
								throw new EbMSResponseSOAPException(connection.getResponseCode(),soapFault.getFaultcode(),response);
						}
						throw new EbMSResponseException(connection.getResponseCode(),response);
					}
					else
						throw new EbMSResponseException(connection.getResponseCode());
				}
			}
			else
				throw new EbMSResponseException(connection.getResponseCode());
		}
		catch (IOException e)
		{
			try (InputStream errorStream = new BufferedInputStream(connection.getErrorStream()))
			{
				String error = IOUtils.toString(errorStream,getEncoding());
				throw new EbMSResponseException(connection.getResponseCode(),error);
			}
			catch (IOException ignore)
			{
			}
			throw e;
		}
	}

	private String getEncoding() throws EbMSProcessingException
	{
		String contentType = getHeaderField("Content-Type");
		if (!StringUtils.isEmpty(contentType))
			return HTTPUtils.getCharSet(contentType);
		else
			throw new EbMSProcessingException("HTTP header Content-Type is not set!");
	}
	
	private String getHeaderField(String name)
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
