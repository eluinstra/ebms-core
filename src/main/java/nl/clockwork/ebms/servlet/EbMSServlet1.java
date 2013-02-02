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
package nl.clockwork.ebms.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSInputStreamHandlerImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class EbMSServlet1 extends GenericServlet
{
	private static final long serialVersionUID = 1L;
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSMessageProcessor messageProcessor;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		String id = config.getInitParameter("messageProcessor");
		if (id == null)
			id = "messageProcessor";
		messageProcessor = wac.getBean(id,EbMSMessageProcessor.class);
	}

	@Override
	public void service(final ServletRequest request, final ServletResponse response) throws ServletException, IOException
	{
		try
		{
			@SuppressWarnings("unchecked")
			Enumeration<String> headerNames = ((HttpServletRequest)request).getHeaderNames();
			Map<String,String> headers = new HashMap<String,String>();
			while (headerNames.hasMoreElements())
			{
				String headerName = headerNames.nextElement();
				headers.put(headerName,((HttpServletRequest)request).getHeader(headerName));
			}
			EbMSInputStreamHandlerImpl handler = 
				new EbMSInputStreamHandlerImpl(messageProcessor,headers)
				{
					@Override
					public void writeMessage(int statusCode)
					{
						((HttpServletResponse)response).setStatus(statusCode);
					}
	
					@Override
					public OutputStream writeMessage(Map<String,String> headers, int statusCode) throws IOException
					{
						((HttpServletResponse)response).setStatus(statusCode);
						//((HttpServletResponse)response).setContentType(headers.get("Content-Type"));
						for (String key : headers.keySet())
							((HttpServletResponse)response).setHeader(key,headers.get(key));
						return response.getOutputStream();
					}
				}
			;
			handler.handle(request.getInputStream());
		}
		catch (EbMSProcessorException e)
		{
			throw new ServletException(e);
		}
	}

}
