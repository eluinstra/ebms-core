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
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSHttpHandler;
import org.springframework.web.context.support.WebApplicationContextUtils;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSServlet extends GenericServlet
{
	private static final long serialVersionUID = 1L;
	EbMSHttpHandler httpHandler;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		val wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		httpHandler = wac.getBean(EbMSHttpHandler.class);
	}

	@Override
	public void service(final ServletRequest request, ServletResponse response) throws ServletException, IOException
	{
		try
		{
			httpHandler.handle((HttpServletRequest)request, (HttpServletResponse)response);
		}
		catch (EbMSProcessorException e)
		{
			throw new ServletException(e);
		}
	}

}
