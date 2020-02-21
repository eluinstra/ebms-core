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
import java.nio.charset.Charset;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.HTTPUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSMessageReader;

public class EbMSResponseHandler
{
	protected transient Log messageLogger = LogFactory.getLog(Constants.MESSAGE_LOG);
	private HttpURLConnection connection;
	private List<Integer> recoverableHttpErrors;
	private List<Integer> irrecoverableHttpErrors;
	
	public EbMSResponseHandler(HttpURLConnection connection, List<Integer> recoverableHttpErrors, List<Integer> irrecoverableHttpErrors)
	{
		this.connection = connection;
		this.recoverableHttpErrors = recoverableHttpErrors;
		this.irrecoverableHttpErrors = irrecoverableHttpErrors;
	}

	public EbMSDocument read() throws IOException, ParserConfigurationException, SAXException, EbMSProcessorException, TransformerException
	{
		try
		{
			if (connection.getResponseCode() / 100 == 2)
			{
				if (connection.getResponseCode() == Constants.SC_NOCONTENT || connection.getContentLength() == 0)
				{
					messageLogger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (messageLogger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : ""));
					return null;
				}
				else
				{
					try (InputStream input = connection.getInputStream())
					{
						EbMSMessageReader messageReader = new EbMSMessageReader(getHeaderField("Content-ID"),getHeaderField("Content-Type"));
						String response = IOUtils.toString(input,getEncoding());
						messageLogger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (messageLogger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "") + "\n" + response);
						return messageReader.readResponse(response);
					}
				}
			}
			else if (connection.getResponseCode() / 100 == 1 || connection.getResponseCode() / 100 == 3 || connection.getResponseCode() / 100 == 4)
			{
				try (InputStream input = connection.getErrorStream())
				{
					String response = null;
					if (input != null)
					{
						response = IOUtils.toString(input,Charset.defaultCharset());
						messageLogger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (messageLogger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "") + "\n" + response);
					}
					if (recoverableHttpErrors.contains(connection.getResponseCode()))
						throw new EbMSResponseException(connection.getResponseCode(),response);
					else
						throw new EbMSIrrecoverableResponsexception(connection.getResponseCode(),response);
				}
			}
			else if (connection.getResponseCode() / 100 == 5)
			{
				try (InputStream input = connection.getErrorStream())
				{
					String response = null;
					if (input != null)
					{
						response = IOUtils.toString(input,Charset.defaultCharset());
						messageLogger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (messageLogger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "") + "\n" + response);
					}
					if (irrecoverableHttpErrors.contains(connection.getResponseCode()))
						throw new EbMSIrrecoverableResponsexception(connection.getResponseCode(),response);
					else
						throw new EbMSResponseException(connection.getResponseCode(),response);
				}
			}
			else
			{
				messageLogger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (messageLogger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : ""));
				throw new EbMSIrrecoverableResponsexception(connection.getResponseCode());
			}
		}
		catch (IOException e)
		{
			try (InputStream errorStream = new BufferedInputStream(connection.getErrorStream()))
			{
//				String error = IOUtils.toString(errorStream,getEncoding());
//				messageLogger.info("<<<<\nstatusCode: " + connection.getResponseCode() + (logger.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "") + "\n" + error);
//				throw new EbMSResponseException(connection.getResponseCode(),error);
				throw new EbMSResponseException(connection.getResponseCode(),e.getMessage());
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
