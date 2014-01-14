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
package nl.clockwork.mule.ebms.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSInputStreamHandler;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

public class EbMSHttpHandler implements Callable
{
	private EbMSMessageProcessor ebMSMessageProcessor;

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception
	{
		final MuleMessage message = eventContext.getMessage();
		final InputStream request = (InputStream)message.getPayload();
		final HttpResponse response = new HttpResponse();
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try
		{
			EbMSInputStreamHandler messageHandler = 
	  		new EbMSInputStreamHandler(ebMSMessageProcessor)
				{
					@Override
					public String getRequestHeader(String headerName)
					{
						String result = message.getProperty(headerName,PropertyScope.INBOUND);
						if (result == null)
							for (String key : message.getPropertyNames(PropertyScope.INBOUND))
							{
								if (key.equalsIgnoreCase(headerName))
								{
									result = message.getProperty(key,PropertyScope.INBOUND);
									break;
								}
							}
						return result;
					}
					
					@Override
					public void writeResponseStatus(int statusCode)
					{
						try
						{
							response.setStatusLine(HttpVersion.parse(HttpConstants.HTTP11),statusCode);
						}
						catch (ProtocolException e)
						{
						}
					}
					
					@Override
					public void writeResponseHeader(String name, String value)
					{
						response.addHeader(new Header(name,value));
					}
					
					@Override
					public OutputStream getOutputStream() throws IOException
					{
						return output;
					}
				}
	  	;
			messageHandler.handle(request);
		}
		catch (EbMSProcessorException e)
		{
			throw new IOException(e);
		}

		response.setBody(output.size() == 0 ? null :
			new OutputHandler()
			{
				@Override
				public void write(MuleEvent event, final OutputStream out) throws IOException
				{
					output.writeTo(out);
				}
			}
		);
		message.setPayload(response);
		return message;
	}

	public void setEbMSMessageProcessor(EbMSMessageProcessor ebMSMessageProcessor)
	{
		this.ebMSMessageProcessor = ebMSMessageProcessor;
	}

}
