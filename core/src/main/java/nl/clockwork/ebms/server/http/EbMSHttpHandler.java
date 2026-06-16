/*
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
package nl.clockwork.ebms.server.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.server.processing.EbMSMessageProcessor;
import nl.clockwork.ebms.server.processing.EbMSProcessorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSHttpHandler
{
	@NonNull
	EbMSMessageProcessor messageProcessor;
	long maxRequestBytes;
	int maxLoggedPayloadChars;

	public EbMSHttpHandler(@NonNull EbMSMessageProcessor messageProcessor)
	{
		this(messageProcessor, EbMSInputStreamHandler.DEFAULT_MAX_REQUEST_BYTES, EbMSInputStreamHandler.DEFAULT_MAX_LOGGED_PAYLOAD_CHARS);
	}

	public EbMSHttpHandler(@NonNull EbMSMessageProcessor messageProcessor, long maxRequestBytes, int maxLoggedPayloadChars)
	{
		this.messageProcessor = messageProcessor;
		this.maxRequestBytes = maxRequestBytes;
		this.maxLoggedPayloadChars = maxLoggedPayloadChars;
	}

	public void handle(final HttpServletRequest request, final HttpServletResponse response) throws EbMSProcessorException
	{
		try
		{
			val inputStreamHandler = new EbMSInputStreamHandler(messageProcessor, maxRequestBytes, maxLoggedPayloadChars)
			{
				@Override
				public List<String> getRequestHeaderNames()
				{
					val result = new ArrayList<String>();
					val headerNames = request.getHeaderNames();
					while (headerNames.hasMoreElements())
						result.add((String)headerNames.nextElement());
					return result;
				}

				@Override
				public List<String> getRequestHeaders(String headerName)
				{
					val result = new ArrayList<String>();
					val headers = request.getHeaders(headerName);
					while (headers.hasMoreElements())
						result.add((String)headers.nextElement());
					return result;
				}

				@Override
				public String getRequestHeader(String headerName)
				{
					if ("Content-Type".equals(headerName))
						return request.getContentType();
					else
						return request.getHeader(headerName);
				}

				@Override
				public String getRequestMethod()
				{
					return request.getMethod();
				}

				@Override
				public long getRequestContentLength()
				{
					return request.getContentLengthLong();
				}

				@Override
				public void writeResponseStatus(int statusCode)
				{
					response.setStatus(statusCode);
				}

				@Override
				public void writeResponseHeader(String name, String value)
				{
					if ("Content-Type".equals(name))
						response.setContentType(value);
					else
						response.setHeader(name, value);
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
}
