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
import nl.clockwork.ebms.Constants;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.HTTPUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSMessageReader;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.soap.envelope.Fault;

public class EbMSResponseHandler
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private HttpURLConnection connection;
	
	public EbMSResponseHandler(HttpURLConnection connection)
	{
		this.connection = connection;
	}

	public EbMSDocument read() throws IOException, ParserConfigurationException, SAXException, EbMSProcessorException, TransformerException
	{
		try
		{
			if (connection.getResponseCode() / 100 == 2)
			{
				if (connection.getResponseCode() == Constants.SC_NOCONTENT || connection.getContentLength() == 0)
				{
					logger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (logger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : ""));
					return null;
				}
				else
				{
					try (InputStream input = connection.getInputStream())
					{
						EbMSMessageReader messageReader = new EbMSMessageReader(getHeaderField("Content-ID"),getHeaderField("Content-Type"));
						//EbMSDocument result = messageReader.read(input);
						EbMSDocument result = messageReader.readResponse(input,getEncoding());
						if (logger.isInfoEnabled())
							logger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (logger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "") + (result == null || result.getMessage() == null ? "" : "\n" + DOMUtils.toString(result.getMessage())));
						return result;
					}
				}
			}
			else if (connection.getResponseCode() >= Constants.SC_BAD_REQUEST)
			{
				try (InputStream input = connection.getErrorStream())
				{
					if (input != null)
					{
						String response = IOUtils.toString(input);
						logger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (logger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "") + "\n" + response);
						if (connection.getResponseCode() == Constants.SC_INTERNAL_SERVER_ERROR)
						{
							Fault soapFault = EbMSMessageUtils.getSOAPFault(response);
							if (soapFault != null)
								throw new EbMSResponseSOAPException(connection.getResponseCode(),soapFault.getFaultcode(),response);
						}
						throw new EbMSResponseException(connection.getResponseCode(),response);
					}
					else
					{
						logger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (logger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : ""));
						throw new EbMSResponseException(connection.getResponseCode());
					}
				}
			}
			else
			{
				logger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (logger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : ""));
				throw new EbMSResponseException(connection.getResponseCode());
			}
		}
		catch (IOException e)
		{
			try (InputStream errorStream = new BufferedInputStream(connection.getErrorStream()))
			{
				String error = IOUtils.toString(errorStream,getEncoding());
				logger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (logger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "") + "\n" + error);
				throw new EbMSResponseException(connection.getResponseCode(),error);
			}
			catch (IOException ignore)
			{
				// ignore error
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
		return connection.getHeaderField(name);
	}
}
