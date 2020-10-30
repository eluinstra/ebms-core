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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSMessageReader;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class EbMSResponseHandler
{
	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);
	@NonNull
	HttpURLConnection connection;
	@NonNull
	List<Integer> recoverableHttpErrors;
	@NonNull
	List<Integer> unrecoverableHttpErrors;
	
	public EbMSDocument read() throws EbMSProcessorException
	{
		try
		{
			switch(connection.getResponseCode() / 100)
			{
				case 2:
					if (connection.getResponseCode() == HttpServletResponse.SC_NO_CONTENT || connection.getContentLength() == 0)
					{
						logResponse(connection);
						return null;
					}
					else
						return readSuccesResponse(connection);
				case 1:
				case 3:
				case 4:
					throw createRecoverableErrorException(connection);
				case 5:
					throw createUnrecoverableErrorException(connection);
				default:
					logResponse(connection);
					throw new EbMSUnrecoverableResponseException(connection.getResponseCode(),connection.getHeaderFields());
			}
		}
		catch (IOException e)
		{
			try
			{
				throw new EbMSResponseException(connection.getResponseCode(),connection.getHeaderFields(),e);
			}
			catch (IOException e1)
			{
				throw new EbMSProcessingException(e);
			}
		}
	}

	private EbMSDocument readSuccesResponse(HttpURLConnection connection) throws IOException
	{
		try (val input = connection.getInputStream())
		{
			val messageReader = new EbMSMessageReader(getHeaderField("Content-ID"),getHeaderField("Content-Type"));
			val response = IOUtils.toString(input,getEncoding());
			logResponse(connection,response);
			try
			{
				return messageReader.readResponse(response);
			}
			catch (ParserConfigurationException e)
			{
				throw new EbMSProcessorException(e);
			}
			catch (SAXException e)
			{
				throw new EbMSResponseException(connection.getResponseCode(),connection.getHeaderFields(),response,e);
			}
		}
	}

	private EbMSResponseException createRecoverableErrorException(HttpURLConnection connection) throws IOException
	{
		try (val input = connection.getErrorStream())
		{
			val response = readResponse(connection,input);
			if (recoverableHttpErrors.contains(connection.getResponseCode()))
				return new EbMSResponseException(connection.getResponseCode(),connection.getHeaderFields(),response);
			else
				return new EbMSUnrecoverableResponseException(connection.getResponseCode(),connection.getHeaderFields(),response);
		}
	}

	private EbMSResponseException createUnrecoverableErrorException(HttpURLConnection connection) throws IOException
	{
		try (val input = connection.getErrorStream())
		{
			val response = readResponse(connection,input);
			if (unrecoverableHttpErrors.contains(connection.getResponseCode()))
				return new EbMSUnrecoverableResponseException(connection.getResponseCode(),connection.getHeaderFields(),response);
			else
				return new EbMSResponseException(connection.getResponseCode(),connection.getHeaderFields(),response);
		}
	}

	private String readResponse(HttpURLConnection connection, InputStream input) throws IOException
	{
		String response = null;
		if (input != null)
		{
			response = IOUtils.toString(input,Charset.defaultCharset());
			logResponse(connection,response);
		}
		return response;
	}

	private String getEncoding() throws EbMSProcessingException
	{
		val contentType = getHeaderField("Content-Type");
		if (!StringUtils.isEmpty(contentType))
			return HTTPUtils.getCharSet(contentType);
		else
			throw new EbMSProcessingException("HTTP header Content-Type is not set!");
	}
	
	private String getHeaderField(String name)
	{
		return connection.getHeaderField(name);
	}

	private void logResponse(HttpURLConnection connection) throws IOException
	{
		logResponse(connection,null);
	}

	private void logResponse(HttpURLConnection connection, String response) throws IOException
	{
		val headers = connection.getResponseCode() + (messageLog.isDebugEnabled() ? "\n" + HTTPUtils.toString(connection.getHeaderFields()) : "");
		messageLog.info("<<<<\nStatusCode=" + headers + (response != null ? "\n" + response : ""));
	}

}
