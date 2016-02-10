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
package nl.clockwork.ebms.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSHttpHandler
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSMessageProcessor messageProcessor;

	public void handle(final HttpServletRequest request, final HttpServletResponse response) throws EbMSProcessorException
	{
		try
		{
			EbMSInputStreamHandler inputStreamHandler = new EbMSInputStreamHandler(messageProcessor)
			{
				@Override
				public String getRequestHeader(String headerName)
				{
					String result = request.getHeader(headerName);
					if ("Content-Type".equalsIgnoreCase(headerName))
						result = request.getContentType();
					if (result == null)
					{
						Enumeration<?> headerNames = request.getHeaderNames();
						while (headerNames.hasMoreElements())
						{
							String key = (String)headerNames.nextElement();
							if (key.equalsIgnoreCase(headerName))
							{
								result = request.getHeader(key);
								break;
							}
						}
					}
					return result;
				}

				@Override
				public void writeResponseStatus(int statusCode)
				{
					response.setStatus(statusCode);
				}
				
				@Override
				public void writeResponseHeader(String name, String value)
				{
					if ("Content-Type".equalsIgnoreCase(name))
						response.setContentType(value);
					else
						response.setHeader(name,value);
				}
				
				@Override
				public OutputStream getOutputStream() throws IOException
				{
					return response.getOutputStream();
				}
				
			};
			inputStreamHandler.handle(request.getInputStream());
		}
		catch (IOException e)
		{
			throw new EbMSProcessorException(e);
		}
	}
	
	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}
}
