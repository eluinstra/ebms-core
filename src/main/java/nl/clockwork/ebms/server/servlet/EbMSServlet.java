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
package nl.clockwork.ebms.server.servlet;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.support.WebApplicationContextUtils;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSHttpHandler;

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
			httpHandler.handle((HttpServletRequest)request,(HttpServletResponse)response);
		}
		catch (EbMSProcessorException e)
		{
			throw new ServletException(e);
		}
	}

}
