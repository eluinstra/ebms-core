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
public class EbMSMessageServlet extends GenericServlet
{
	private static final long serialVersionUID = 1L;
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSMessageProcessor ebMSMessageProcessor;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		String id = config.getInitParameter("ebMSMessageProcessor");
		if (id == null)
			id = "ebMSMessageProcessor";
		ebMSMessageProcessor = wac.getBean(id,EbMSMessageProcessor.class);
	}

	@Override
	public void service(final ServletRequest request, final ServletResponse response) throws ServletException, IOException
	{
		try
		{
			Map<String,String> headers = getHeaders((HttpServletRequest)request);
			EbMSInputStreamHandlerImpl handler = 
				new EbMSInputStreamHandlerImpl(ebMSMessageProcessor,headers)
				{
					@Override
					public void writeResponseStatus(int statusCode)
					{
						((HttpServletResponse)response).setStatus(statusCode);
					}

					@Override
					public void writeResponseHeader(String name, String value)
					{
						((HttpServletResponse)response).addHeader(name,value);
					}
				
					@Override
					public OutputStream getOutputStream() throws IOException
					{
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

	private Map<String,String> getHeaders(HttpServletRequest request)
	{
		Map<String,String> result = new HashMap<String,String>();
		@SuppressWarnings("unchecked")
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements())
		{
			String headerName = headerNames.nextElement();
			result.put(headerName,request.getHeader(headerName));
		}
		return result;
	}
	
	public void setEbMSMessageProcessor(EbMSMessageProcessor ebMSMessageProcessor)
	{
		this.ebMSMessageProcessor = ebMSMessageProcessor;
	}

}
