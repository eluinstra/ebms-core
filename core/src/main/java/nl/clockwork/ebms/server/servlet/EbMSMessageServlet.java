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
package nl.clockwork.ebms.server.servlet;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.server.EbMSInputStreamHandler;
import org.springframework.web.context.support.WebApplicationContextUtils;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSMessageServlet extends GenericServlet
{
	private static final long serialVersionUID = 1L;
	EbMSMessageProcessor ebMSMessageProcessor;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		val wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		ebMSMessageProcessor = wac.getBean(EbMSMessageProcessor.class);
	}

	@Override
	public void service(final ServletRequest request, final ServletResponse response) throws ServletException, IOException
	{
		val handler = new EbMSInputStreamHandler(ebMSMessageProcessor)
		{
			@Override
			public List<String> getRequestHeaderNames()
			{
				val result = new ArrayList<String>();
				val headerNames = ((HttpServletRequest)request).getHeaderNames();
				while (headerNames.hasMoreElements())
					result.add((String)headerNames.nextElement());
				return result;
			}

			@Override
			public List<String> getRequestHeaders(String headerName)
			{
				val result = new ArrayList<String>();
				val headers = ((HttpServletRequest)request).getHeaders(headerName);
				while (headers.hasMoreElements())
					result.add((String)headers.nextElement());
				return result;
			}

			@Override
			public String getRequestHeader(String headerName)
			{
				return "Content-Type".equals(headerName) ? request.getContentType() : ((HttpServletRequest)request).getHeader(headerName);
			}

			@Override
			public String getRequestMethod()
			{
				return ((HttpServletRequest)request).getMethod();
			}

			@Override
			public void writeResponseStatus(int statusCode)
			{
				((HttpServletResponse)response).setStatus(statusCode);
			}

			@Override
			public void writeResponseHeader(String name, String value)
			{
				if ("Content-Type".equals(name))
					response.setContentType(value);
				else
					((HttpServletResponse)response).setHeader(name, value);
			}

			@Override
			public OutputStream getOutputStream() throws IOException
			{
				return response.getOutputStream();
			}
		};
		handler.handle(request.getInputStream());
	}

	public void setEbMSMessageProcessor(EbMSMessageProcessor ebMSMessageProcessor)
	{
		this.ebMSMessageProcessor = ebMSMessageProcessor;
	}

}
